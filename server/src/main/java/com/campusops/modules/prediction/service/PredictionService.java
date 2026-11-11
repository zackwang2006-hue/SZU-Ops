package com.campusops.modules.prediction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusops.common.BizException;
import com.campusops.config.SecurityUtils;
import com.campusops.modules.asset.entity.AssetCategory;
import com.campusops.modules.asset.entity.AssetDevice;
import com.campusops.modules.asset.mapper.AssetCategoryMapper;
import com.campusops.modules.asset.mapper.AssetDeviceMapper;
import com.campusops.modules.prediction.entity.PredictionRisk;
import com.campusops.modules.prediction.mapper.PredictionRiskMapper;
import com.campusops.modules.repair.entity.RepairOrder;
import com.campusops.modules.repair.entity.RepairTimeline;
import com.campusops.modules.repair.mapper.RepairOrderMapper;
import com.campusops.modules.repair.service.RepairService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 预测性维护规则引擎 v1（公式见 docs/ARCHITECTURE.md 第 7 节）：
 * risk_score = w1*近90天同设备故障次数(归一化) + w2*近90天同楼栋同品类故障密度(归一化)
 *            + w3*服役年限/预期寿命 + w4*是否维修中
 * 等级映射：>=0.8 CRITICAL，>=0.6 HIGH，>=0.4 MEDIUM，其余 LOW
 */
@Slf4j
@Service
public class PredictionService {

    // 规则权重
    private static final double W1 = 0.4; // 同设备故障频率
    private static final double W2 = 0.2; // 同楼栋同品类故障密度
    private static final double W3 = 0.3; // 服役年限占比
    private static final double W4 = 0.1; // 维修中状态

    // 归一化分母：90 天内本机故障 5 次封顶、同楼栋同品类 10 次封顶
    private static final double FAULT_COUNT_CAP = 5.0;
    private static final double DENSITY_CAP = 10.0;

    private final AssetDeviceMapper deviceMapper;
    private final AssetCategoryMapper categoryMapper;
    private final RepairOrderMapper orderMapper;
    private final PredictionRiskMapper riskMapper;
    private final RepairService repairService;
    private final MlPredictionClient mlPredictionClient;

    public PredictionService(AssetDeviceMapper deviceMapper, AssetCategoryMapper categoryMapper,
                             RepairOrderMapper orderMapper, PredictionRiskMapper riskMapper,
                             @Lazy RepairService repairService, MlPredictionClient mlPredictionClient) {
        this.deviceMapper = deviceMapper;
        this.categoryMapper = categoryMapper;
        this.orderMapper = orderMapper;
        this.riskMapper = riskMapper;
        this.repairService = repairService;
        this.mlPredictionClient = mlPredictionClient;
    }

    /** 风险预警分页查询 */
    public Page<PredictionRisk> pageRisks(int pageNum, int pageSize, String level, String status) {
        LambdaQueryWrapper<PredictionRisk> wrapper = new LambdaQueryWrapper<PredictionRisk>()
                .eq(StringUtils.hasText(level), PredictionRisk::getRiskLevel, level)
                .eq(StringUtils.hasText(status), PredictionRisk::getStatus, status)
                .orderByDesc(PredictionRisk::getRiskScore)
                .orderByDesc(PredictionRisk::getId);
        Page<PredictionRisk> page = riskMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        // 填充设备展示信息
        page.getRecords().forEach(risk -> {
            if (risk.getDeviceId() != null) {
                AssetDevice device = deviceMapper.selectById(risk.getDeviceId());
                if (device != null) {
                    risk.setDeviceName(device.getName());
                    risk.setDeviceCode(device.getCode());
                    risk.setBuilding(device.getBuilding());
                    risk.setRoom(device.getRoom());
                }
            }
        });
        return page;
    }

    /** 每日凌晨 2 点全量重算风险，并升级 SLA 超时工单优先级 */
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyRecalculate() {
        int risks = recalculate();
        int escalated = repairService.escalateTimeoutOrders();
        log.info("预测定时任务完成：维护风险记录 {} 条，升级超时工单 {} 条", risks, escalated);
    }

    /**
     * 全量重算所有在役/维修中设备的风险分，维护 OPEN 风险记录并回写设备健康度
     *
     * @return 当前 OPEN 的高风险（HIGH/CRITICAL）记录数
     */
    @Transactional
    public int recalculate() {
        LocalDateTime since = LocalDateTime.now().minusDays(90);
        List<AssetDevice> devices = deviceMapper.selectList(new LambdaQueryWrapper<AssetDevice>()
                .ne(AssetDevice::getStatus, AssetDevice.STATUS_SCRAPPED));
        Map<Long, AssetCategory> categoryMap = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(AssetCategory::getId, Function.identity(), (a, b) -> a));

        int openRiskCount = 0;
        for (AssetDevice device : devices) {
            // 因子1：近90天同设备故障次数（归一化）
            long faultCount = countFaults(List.of(device.getId()), since);
            double f1 = Math.min(faultCount / FAULT_COUNT_CAP, 1.0);

            // 因子2：近90天同楼栋同品类故障密度（归一化）
            List<Long> peerIds = deviceMapper.selectList(new LambdaQueryWrapper<AssetDevice>()
                            .eq(AssetDevice::getBuilding, device.getBuilding())
                            .eq(AssetDevice::getCategoryId, device.getCategoryId()))
                    .stream().map(AssetDevice::getId).toList();
            long densityCount = peerIds.isEmpty() ? 0 : countFaults(peerIds, since);
            double f2 = Math.min(densityCount / DENSITY_CAP, 1.0);

            // 因子3：服役年限 / 品类预期寿命
            double years = 0;
            double f3 = 0;
            AssetCategory category = categoryMap.get(device.getCategoryId());
            if (device.getPurchaseDate() != null && category != null
                    && category.getExpectedLifespanYears() != null && category.getExpectedLifespanYears() > 0) {
                years = ChronoUnit.DAYS.between(device.getPurchaseDate(), LocalDate.now()) / 365.25;
                f3 = Math.min(years / category.getExpectedLifespanYears(), 1.0);
            }

            // 因子4：当前是否处于维修中
            double f4 = AssetDevice.STATUS_REPAIRING.equals(device.getStatus()) ? 1.0 : 0.0;

            double score = W1 * f1 + W2 * f2 + W3 * f3 + W4 * f4;
            // 预留 ML 融合：若 ML 服务可用，与规则分取平均
            Double mlScore = safeMlPredict(device.getId());
            if (mlScore != null) {
                score = (score + mlScore) / 2.0;
            }
            String level = levelOf(score);

            // 回写设备健康度（0-100）
            device.setHealthScore((int) Math.round((1 - score) * 100));
            device.setUpdatedAt(LocalDateTime.now());
            deviceMapper.updateById(device);

            // 维护风险记录：高风险生成/更新 OPEN 记录；降为低中风险则自动关闭原 OPEN 记录
            PredictionRisk open = riskMapper.selectOne(new LambdaQueryWrapper<PredictionRisk>()
                    .eq(PredictionRisk::getDeviceId, device.getId())
                    .eq(PredictionRisk::getStatus, PredictionRisk.STATUS_OPEN)
                    .last("LIMIT 1"));
            String reason = buildReason(faultCount, densityCount, years, category, f4 > 0);
            if (PredictionRisk.LEVEL_HIGH.equals(level) || PredictionRisk.LEVEL_CRITICAL.equals(level)) {
                LocalDate predictedDate = predictNextFailureDate(device.getId());
                if (open == null) {
                    PredictionRisk risk = new PredictionRisk();
                    risk.setDeviceId(device.getId());
                    risk.setRiskLevel(level);
                    risk.setRiskScore(score);
                    risk.setReason(reason);
                    risk.setPredictedFailureDate(predictedDate);
                    risk.setStatus(PredictionRisk.STATUS_OPEN);
                    risk.setCreatedAt(LocalDateTime.now());
                    riskMapper.insert(risk);
                } else {
                    open.setRiskLevel(level);
                    open.setRiskScore(score);
                    open.setReason(reason);
                    open.setPredictedFailureDate(predictedDate);
                    riskMapper.updateById(open);
                }
                openRiskCount++;
            } else if (open != null) {
                open.setStatus(PredictionRisk.STATUS_DISMISSED);
                riskMapper.updateById(open);
            }
        }
        return openRiskCount;
    }

    /** 高风险记录一键转巡检工单（复用 repair_order，category='INSPECTION'） */
    @Transactional
    public RepairOrder convertToInspection(Long riskId) {
        PredictionRisk risk = riskMapper.selectById(riskId);
        if (risk == null) {
            throw new BizException("风险记录不存在，id=" + riskId);
        }
        if (!PredictionRisk.STATUS_OPEN.equals(risk.getStatus())) {
            throw new BizException("该风险记录状态为「" + risk.getStatus() + "」，只有 OPEN 状态可转巡检工单");
        }
        AssetDevice device = deviceMapper.selectById(risk.getDeviceId());
        if (device == null) {
            throw new BizException("风险关联的设备不存在，id=" + risk.getDeviceId());
        }

        RepairOrder order = new RepairOrder();
        order.setTitle("预防性巡检：" + device.getName());
        order.setCategory(RepairOrder.CATEGORY_INSPECTION);
        order.setDeviceId(device.getId());
        order.setReporterId(SecurityUtils.currentUserId());
        order.setBuilding(device.getBuilding());
        order.setRoom(device.getRoom());
        order.setDescription("由风险预警 #" + riskId + " 转化。风险等级：" + risk.getRiskLevel()
                + "，原因：" + risk.getReason());
        // CRITICAL 风险对应 URGENT 巡检，HIGH 对应 HIGH
        order.setPriority(PredictionRisk.LEVEL_CRITICAL.equals(risk.getRiskLevel())
                ? RepairOrder.PRIORITY_URGENT : RepairOrder.PRIORITY_HIGH);
        order.setStatus(RepairOrder.STATUS_PENDING);
        LocalDateTime now = LocalDateTime.now();
        order.setCreatedAt(now);
        order.setSlaDeadline(now.plusHours(72)); // 巡检工单统一 72 小时时限
        orderMapper.insert(order);
        order.setOrderNo("RO" + now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%05d", order.getId()));
        orderMapper.updateById(order);

        repairService.addTimeline(order.getId(), RepairTimeline.ACTION_CREATE,
                SecurityUtils.currentUserId(), "由风险预警 #" + riskId + " 转化为巡检工单");

        risk.setStatus(PredictionRisk.STATUS_CONVERTED);
        riskMapper.updateById(risk);
        return order;
    }

    /** 忽略风险记录 */
    public void dismiss(Long riskId) {
        PredictionRisk risk = riskMapper.selectById(riskId);
        if (risk == null) {
            throw new BizException("风险记录不存在，id=" + riskId);
        }
        if (!PredictionRisk.STATUS_OPEN.equals(risk.getStatus())) {
            throw new BizException("该风险记录状态为「" + risk.getStatus() + "」，只有 OPEN 状态可忽略");
        }
        risk.setStatus(PredictionRisk.STATUS_DISMISSED);
        riskMapper.updateById(risk);
    }

    // ---------- 内部工具 ----------

    /** 统计近90天内、排除巡检工单与已取消的故障工单数 */
    private long countFaults(List<Long> deviceIds, LocalDateTime since) {
        Long count = orderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                .in(RepairOrder::getDeviceId, deviceIds)
                .ge(RepairOrder::getCreatedAt, since)
                .ne(RepairOrder::getCategory, RepairOrder.CATEGORY_INSPECTION)
                .ne(RepairOrder::getStatus, RepairOrder.STATUS_CANCELLED));
        return count == null ? 0 : count;
    }

    /** 按历史故障间隔均值外推预测故障日；历史故障不足 2 次时无法外推，返回 null */
    private LocalDate predictNextFailureDate(Long deviceId) {
        List<RepairOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getDeviceId, deviceId)
                .ne(RepairOrder::getCategory, RepairOrder.CATEGORY_INSPECTION)
                .ne(RepairOrder::getStatus, RepairOrder.STATUS_CANCELLED)
                .orderByAsc(RepairOrder::getCreatedAt));
        if (orders.size() < 2) {
            return null;
        }
        long totalDays = 0;
        for (int i = 1; i < orders.size(); i++) {
            totalDays += ChronoUnit.DAYS.between(
                    orders.get(i - 1).getCreatedAt().toLocalDate(),
                    orders.get(i).getCreatedAt().toLocalDate());
        }
        long avgInterval = Math.max(totalDays / (orders.size() - 1), 1);
        return orders.get(orders.size() - 1).getCreatedAt().toLocalDate().plusDays(avgInterval);
    }

    /** 等级映射：>=0.8 CRITICAL，>=0.6 HIGH，>=0.4 MEDIUM，其余 LOW */
    private String levelOf(double score) {
        if (score >= 0.8) {
            return PredictionRisk.LEVEL_CRITICAL;
        }
        if (score >= 0.6) {
            return PredictionRisk.LEVEL_HIGH;
        }
        if (score >= 0.4) {
            return PredictionRisk.LEVEL_MEDIUM;
        }
        return PredictionRisk.LEVEL_LOW;
    }

    private String buildReason(long faultCount, long densityCount, double years,
                               AssetCategory category, boolean repairing) {
        String lifespan = category == null || category.getExpectedLifespanYears() == null
                ? "未知" : category.getExpectedLifespanYears().toString();
        return String.format("近90天本机故障%d次；同楼栋同品类故障%d次；服役%.1f年/预期寿命%s年；%s",
                faultCount, densityCount, years, lifespan, repairing ? "当前处于维修中" : "当前在役");
    }

    /** 调用预留的 ML 服务；异常时静默退回规则引擎 */
    private Double safeMlPredict(Long deviceId) {
        try {
            return mlPredictionClient.predictFailureProbability(deviceId);
        } catch (Exception e) {
            log.warn("ML 预测服务调用失败，退回规则引擎结果: {}", e.getMessage());
            return null;
        }
    }
}
