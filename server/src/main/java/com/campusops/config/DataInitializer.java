package com.campusops.config;

import com.campusops.modules.asset.entity.AssetCategory;
import com.campusops.modules.asset.entity.AssetDevice;
import com.campusops.modules.asset.mapper.AssetCategoryMapper;
import com.campusops.modules.asset.mapper.AssetDeviceMapper;
import com.campusops.modules.auth.entity.SysUser;
import com.campusops.modules.auth.mapper.SysUserMapper;
import com.campusops.modules.prediction.service.PredictionService;
import com.campusops.modules.repair.entity.RepairEvaluation;
import com.campusops.modules.repair.entity.RepairOrder;
import com.campusops.modules.repair.entity.RepairTimeline;
import com.campusops.modules.repair.mapper.RepairEvaluationMapper;
import com.campusops.modules.repair.mapper.RepairOrderMapper;
import com.campusops.modules.repair.mapper.RepairTimelineMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 内置演示数据（仅当数据库为空时执行一次）：
 * 5 个账号（密码均为 123456，BCrypt 加密）、8 个品类、22 台设备、34 条覆盖全状态的工单。
 * 工单创建时间分布在近 90 天内，保证预测与看板开箱有数据。
 */
@Slf4j
@Component
public class DataInitializer implements ApplicationRunner {

    private final SysUserMapper userMapper;
    private final AssetCategoryMapper categoryMapper;
    private final AssetDeviceMapper deviceMapper;
    private final RepairOrderMapper orderMapper;
    private final RepairTimelineMapper timelineMapper;
    private final RepairEvaluationMapper evaluationMapper;
    private final PasswordEncoder passwordEncoder;
    private final PredictionService predictionService;

    /** 品类 id -> slaHours，用于计算种子工单的 sla_deadline */
    private final Map<Long, Integer> slaMap = new HashMap<>();

    public DataInitializer(SysUserMapper userMapper, AssetCategoryMapper categoryMapper,
                           AssetDeviceMapper deviceMapper, RepairOrderMapper orderMapper,
                           RepairTimelineMapper timelineMapper, RepairEvaluationMapper evaluationMapper,
                           PasswordEncoder passwordEncoder, PredictionService predictionService) {
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
        this.deviceMapper = deviceMapper;
        this.orderMapper = orderMapper;
        this.timelineMapper = timelineMapper;
        this.evaluationMapper = evaluationMapper;
        this.passwordEncoder = passwordEncoder;
        this.predictionService = predictionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        Long userCount = userMapper.selectCount(null);
        if (userCount != null && userCount > 0) {
            return; // 已有数据，跳过初始化
        }
        log.info("开始初始化演示数据...");

        // ---------- 用户（密码均 123456，BCrypt） ----------
        String pwd = passwordEncoder.encode("123456");
        SysUser admin = seedUser("admin", pwd, "系统管理员", SysUser.ROLE_ADMIN);
        SysUser dispatcher = seedUser("dispatcher01", pwd, "调度员小周", SysUser.ROLE_DISPATCHER);
        SysUser tech01 = seedUser("tech01", pwd, "维修工老王", SysUser.ROLE_TECHNICIAN);
        SysUser tech02 = seedUser("tech02", pwd, "维修工小李", SysUser.ROLE_TECHNICIAN);
        SysUser user01 = seedUser("user01", pwd, "学生小张", SysUser.ROLE_USER);

        // ---------- 设备品类（含 sla_hours 与预期寿命） ----------
        AssetCategory catPc = seedCategory("电脑", "PC", 6, 24);
        AssetCategory catAc = seedCategory("空调", "AC", 8, 48);
        AssetCategory catPj = seedCategory("投影仪", "PROJECTOR", 5, 24);
        AssetCategory catNw = seedCategory("网络设备", "NETWORK", 8, 12);
        AssetCategory catPr = seedCategory("打印机", "PRINTER", 5, 24);
        AssetCategory catEl = seedCategory("电梯", "ELEVATOR", 15, 4);
        AssetCategory catLt = seedCategory("照明", "LIGHTING", 10, 48);
        AssetCategory catWh = seedCategory("热水器", "WATER_HEATER", 8, 24);

        // ---------- 设备台账（22 台，分布于多栋楼） ----------
        AssetDevice pc001 = seedDevice("PC-001", "联想台式机", catPc, "教学楼A", "301室", "2021-03-15", "联想", 4500, AssetDevice.STATUS_IN_USE);
        AssetDevice pc002 = seedDevice("PC-002", "联想台式机", catPc, "教学楼A", "302室", "2022-09-01", "联想", 4600, AssetDevice.STATUS_IN_USE);
        AssetDevice pc003 = seedDevice("PC-003", "戴尔一体机", catPc, "图书馆", "电子阅览室", "2020-06-20", "戴尔", 5200, AssetDevice.STATUS_IN_USE);
        AssetDevice pc004 = seedDevice("PC-004", "惠普笔记本", catPc, "行政楼", "205室", "2023-04-11", "惠普", 6300, AssetDevice.STATUS_IN_USE);
        AssetDevice pc005 = seedDevice("PC-005", "联想台式机", catPc, "实验楼", "401室", "2019-11-11", "联想", 4300, AssetDevice.STATUS_IN_USE);
        // 老旧空调：服役 9 年超寿命 + 近 90 天多次故障 → 触发高风险预警
        AssetDevice ac001 = seedDevice("AC-001", "格力柜式空调", catAc, "教学楼A", "101室", "2016-05-10", "格力", 8900, AssetDevice.STATUS_REPAIRING);
        AssetDevice ac002 = seedDevice("AC-002", "格力挂机空调", catAc, "宿舍1栋", "大厅", "2017-08-22", "格力", 3200, AssetDevice.STATUS_IN_USE);
        AssetDevice ac003 = seedDevice("AC-003", "美的挂机空调", catAc, "宿舍2栋", "大厅", "2019-06-30", "美的", 3100, AssetDevice.STATUS_IN_USE);
        AssetDevice ac004 = seedDevice("AC-004", "格力中央空调", catAc, "图书馆", "大厅", "2021-01-15", "格力", 56000, AssetDevice.STATUS_IN_USE);
        AssetDevice ac005 = seedDevice("AC-005", "美的柜式空调", catAc, "体育馆", "办公室", "2017-05-05", "美的", 8600, AssetDevice.STATUS_IN_USE);
        AssetDevice pj001 = seedDevice("PJ-001", "爱普生投影仪", catPj, "教学楼B", "201室", "2018-03-05", "爱普生", 6800, AssetDevice.STATUS_IN_USE);
        AssetDevice pj002 = seedDevice("PJ-002", "明基投影仪", catPj, "教学楼B", "305室", "2022-11-20", "明基", 5900, AssetDevice.STATUS_IN_USE);
        // 核心交换机：近 90 天 3 次故障 → 中高风险
        AssetDevice nw001 = seedDevice("NW-001", "华为核心交换机", catNw, "图书馆", "机房", "2019-09-01", "华为", 38000, AssetDevice.STATUS_REPAIRING);
        AssetDevice nw002 = seedDevice("NW-002", "H3C接入交换机", catNw, "实验楼", "机房", "2020-05-18", "H3C", 8500, AssetDevice.STATUS_IN_USE);
        AssetDevice nw003 = seedDevice("NW-003", "无线AP", catNw, "体育馆", "看台", "2022-02-14", "华为", 2200, AssetDevice.STATUS_IN_USE);
        AssetDevice pr001 = seedDevice("PR-001", "惠普激光打印机", catPr, "行政楼", "文印室", "2018-07-09", "惠普", 2600, AssetDevice.STATUS_IN_USE);
        AssetDevice pr002 = seedDevice("PR-002", "佳能打印机", catPr, "图书馆", "服务台", "2021-10-01", "佳能", 2400, AssetDevice.STATUS_IN_USE);
        AssetDevice el001 = seedDevice("EL-001", "三菱电梯", catEl, "行政楼", "东梯", "2015-12-01", "三菱", 280000, AssetDevice.STATUS_IN_USE);
        AssetDevice lt001 = seedDevice("LT-001", "LED照明组", catLt, "教学楼A", "走廊", "2020-04-25", "欧普", 1500, AssetDevice.STATUS_IN_USE);
        AssetDevice lt002 = seedDevice("LT-002", "LED照明组", catLt, "宿舍1栋", "走廊", "2021-08-15", "欧普", 1500, AssetDevice.STATUS_IN_USE);
        AssetDevice wh001 = seedDevice("WH-001", "海尔热水器", catWh, "宿舍1栋", "洗漱间", "2018-09-10", "海尔", 1800, AssetDevice.STATUS_IN_USE);
        AssetDevice wh002 = seedDevice("WH-002", "美的热水器", catWh, "宿舍2栋", "洗漱间", "2020-03-22", "美的", 1750, AssetDevice.STATUS_IN_USE);

        // ---------- 工单（34 条：12 完成 / 4 待验收 / 4 处理中 / 4 已派单 / 8 待派单 / 2 取消） ----------

        // 老空调 AC-001 近 90 天 5 次故障（4 完成 + 1 处理中），供预测模块产生高风险
        seedOrder("空调制冷效果差，出风不凉", "制冷异常", ac001, user01, tech01, RepairOrder.PRIORITY_HIGH,
                RepairOrder.STATUS_COMPLETED, 80, 30, 4);
        seedOrder("空调内机漏水", "漏水", ac001, user01, tech02, RepairOrder.PRIORITY_MEDIUM,
                RepairOrder.STATUS_COMPLETED, 62, 50, 3);
        seedOrder("空调开机后自动停机", "电路故障", ac001, user01, tech01, RepairOrder.PRIORITY_HIGH,
                RepairOrder.STATUS_COMPLETED, 45, 55, 3);
        seedOrder("空调噪音过大", "噪音异常", ac001, user01, tech02, RepairOrder.PRIORITY_MEDIUM,
                RepairOrder.STATUS_COMPLETED, 20, 40, 4);
        seedOrder("空调完全不制冷", "制冷异常", ac001, user01, tech01, RepairOrder.PRIORITY_URGENT,
                RepairOrder.STATUS_PROCESSING, 3, null, null);

        // 核心交换机 NW-001 近 90 天 3 次故障
        seedOrder("机房核心交换机端口异常", "网络中断", nw001, user01, tech02, RepairOrder.PRIORITY_URGENT,
                RepairOrder.STATUS_COMPLETED, 70, 8, 5);
        seedOrder("交换机温度过高告警", "硬件故障", nw001, user01, tech01, RepairOrder.PRIORITY_HIGH,
                RepairOrder.STATUS_COMPLETED, 33, 14, 4);
        seedOrder("交换机电源模块故障", "硬件故障", nw001, user01, tech02, RepairOrder.PRIORITY_URGENT,
                RepairOrder.STATUS_ASSIGNED, 2, null, null);

        // 其余完成工单（含 SLA 超时完成与按时完成，覆盖多名维修工与评分）
        seedOrder("电脑无法开机", "硬件故障", pc001, user01, tech01, RepairOrder.PRIORITY_MEDIUM,
                RepairOrder.STATUS_COMPLETED, 85, 20, 5);
        seedOrder("电脑蓝屏频繁", "系统故障", pc003, user01, tech02, RepairOrder.PRIORITY_MEDIUM,
                RepairOrder.STATUS_COMPLETED, 55, 30, 3); // sla 24h，30h 完成 → 超时完成
        seedOrder("投影仪画面模糊", "显示异常", pj001, user01, tech01, RepairOrder.PRIORITY_MEDIUM,
                RepairOrder.STATUS_COMPLETED, 48, 10, 5);
        seedOrder("打印机卡纸严重", "机械故障", pr001, user01, tech02, RepairOrder.PRIORITY_LOW,
                RepairOrder.STATUS_COMPLETED, 40, 26, 4); // sla 24h，超时完成
        seedOrder("热水器不出热水", "加热故障", wh001, user01, tech01, RepairOrder.PRIORITY_HIGH,
                RepairOrder.STATUS_COMPLETED, 25, 18, 5);
        seedOrder("走廊灯闪烁", "照明故障", lt002, user01, tech02, RepairOrder.PRIORITY_LOW,
                RepairOrder.STATUS_COMPLETED, 15, 30, 4);
        seedOrder("电梯运行有异响", "机械故障", el001, user01, tech01, RepairOrder.PRIORITY_URGENT,
                RepairOrder.STATUS_COMPLETED, 6, 3, 5);

        // 待验收 4 条
        seedOrder("笔记本电池不充电", "硬件故障", pc004, user01, tech02, RepairOrder.PRIORITY_MEDIUM,
                RepairOrder.STATUS_TO_VERIFY, 4, 12, null);
        seedOrder("投影仪无法开机", "电路故障", pj002, user01, tech01, RepairOrder.PRIORITY_HIGH,
                RepairOrder.STATUS_TO_VERIFY, 2, 20, null);
        seedOrder("中央空调出风口异响", "噪音异常", ac004, user01, tech02, RepairOrder.PRIORITY_MEDIUM,
                RepairOrder.STATUS_TO_VERIFY, 1, 30, null);
        seedOrder("打印机打印有黑线", "硬件故障", pr002, user01, tech01, RepairOrder.PRIORITY_LOW,
                RepairOrder.STATUS_TO_VERIFY, 1, 8, null);

        // 处理中 3 条（另有 AC-001 那条共 4 条）
        seedOrder("实验室电脑运行缓慢", "系统故障", pc005, user01, tech01, RepairOrder.PRIORITY_LOW,
                RepairOrder.STATUS_PROCESSING, 5, null, null);
        seedOrder("宿舍空调遥控器失灵", "配件故障", ac003, user01, tech02, RepairOrder.PRIORITY_MEDIUM,
                RepairOrder.STATUS_PROCESSING, 2, null, null);
        seedOrder("文印室打印机缺纸传感器故障", "机械故障", pr001, user01, tech02, RepairOrder.PRIORITY_MEDIUM,
                RepairOrder.STATUS_PROCESSING, 1, null, null);

        // 已派单 3 条（另有 NW-001 那条共 4 条）
        seedOrder("体育馆空调不制热", "制热异常", ac005, user01, tech01, RepairOrder.PRIORITY_HIGH,
                RepairOrder.STATUS_ASSIGNED, 1, null, null);
        seedOrder("服务台打印机无法连接", "网络故障", pr002, user01, tech02, RepairOrder.PRIORITY_MEDIUM,
                RepairOrder.STATUS_ASSIGNED, 1, null, null);
        // 已派单但 SLA 超时（照明 sla 48h，5 天前派单未开工）
        seedOrder("教学楼走廊灯不亮", "照明故障", lt001, user01, tech01, RepairOrder.PRIORITY_MEDIUM,
                RepairOrder.STATUS_ASSIGNED, 5, null, null);

        // 待派单 8 条（含 3 条已 SLA 超时，供超时列表展示）
        seedOrder("阅览室电脑耳机孔损坏", "硬件故障", pc003, user01, null, RepairOrder.PRIORITY_LOW,
                RepairOrder.STATUS_PENDING, 1, null, null);
        seedOrder("宿舍热水器水温忽冷忽热", "加热故障", wh002, user01, null, RepairOrder.PRIORITY_MEDIUM,
                RepairOrder.STATUS_PENDING, 2, null, null);
        seedOrder("教学楼投影仪灯泡老化", "显示异常", pj001, user01, null, RepairOrder.PRIORITY_MEDIUM,
                RepairOrder.STATUS_PENDING, 3, null, null);
        seedOrder("宿舍大厅空调漏水", "漏水", ac002, user01, null, RepairOrder.PRIORITY_HIGH,
                RepairOrder.STATUS_PENDING, 1, null, null);
        seedOrder("无线AP信号弱", "网络故障", nw003, user01, null, RepairOrder.PRIORITY_MEDIUM,
                RepairOrder.STATUS_PENDING, 6, null, null);
        // 超时待派单：网络 sla 12h / 电梯 sla 4h / 空调 sla 48h
        seedOrder("实验楼交换机端口不通", "网络中断", nw002, user01, null, RepairOrder.PRIORITY_HIGH,
                RepairOrder.STATUS_PENDING, 4, null, null);
        seedOrder("电梯按钮失灵", "电路故障", el001, user01, null, RepairOrder.PRIORITY_URGENT,
                RepairOrder.STATUS_PENDING, 2, null, null);
        seedOrder("机房空调温度异常", "制冷异常", ac004, user01, null, RepairOrder.PRIORITY_HIGH,
                RepairOrder.STATUS_PENDING, 3, null, null);

        // 已取消 2 条
        seedOrder("电脑键盘按键失灵", "配件故障", pc002, user01, null, RepairOrder.PRIORITY_LOW,
                RepairOrder.STATUS_CANCELLED, 30, null, null);
        seedOrder("投影仪遥控器丢失", "配件故障", pj002, user01, tech01, RepairOrder.PRIORITY_LOW,
                RepairOrder.STATUS_CANCELLED, 10, null, null);

        // 初始化后立即重算一次风险与健康度，让预测/看板开箱有数据
        int riskCount = predictionService.recalculate();
        log.info("演示数据初始化完成：用户 5，品类 8，设备 22，工单 34，OPEN 风险 {}", riskCount);
    }

    // ---------- 种子数据工具方法 ----------

    private SysUser seedUser(String username, String password, String realName, String role) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(password);
        user.setRealName(realName);
        user.setPhone("13800000000");
        user.setRole(role);
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    private AssetCategory seedCategory(String name, String code, int lifespanYears, int slaHours) {
        AssetCategory category = new AssetCategory();
        category.setName(name);
        category.setCode(code);
        category.setExpectedLifespanYears(lifespanYears);
        category.setSlaHours(slaHours);
        categoryMapper.insert(category);
        slaMap.put(category.getId(), slaHours);
        return category;
    }

    private AssetDevice seedDevice(String code, String name, AssetCategory category, String building,
                                   String room, String purchaseDate, String supplier,
                                   double price, String status) {
        AssetDevice device = new AssetDevice();
        device.setCode(code);
        device.setName(name);
        device.setCategoryId(category.getId());
        device.setBuilding(building);
        device.setRoom(room);
        device.setPurchaseDate(LocalDate.parse(purchaseDate));
        device.setSupplier(supplier);
        device.setPrice(BigDecimal.valueOf(price));
        device.setStatus(status);
        device.setHealthScore(100);
        LocalDateTime now = LocalDateTime.now();
        device.setCreatedAt(now);
        device.setUpdatedAt(now);
        deviceMapper.insert(device);
        return device;
    }

    /**
     * 构造一条种子工单及其时间线/评价
     *
     * @param createdDaysAgo  创建于多少天前
     * @param finishHours     创建后多少小时完工（仅 TO_VERIFY/COMPLETED 需要）
     * @param evalScore       满意度评分（仅 COMPLETED 可填）
     */
    private void seedOrder(String title, String faultCategory, AssetDevice device, SysUser reporter,
                           SysUser assignee, String priority, String status,
                           int createdDaysAgo, Integer finishHours, Integer evalScore) {
        LocalDateTime created = LocalDateTime.now().minusDays(createdDaysAgo);
        boolean beyondPending = !RepairOrder.STATUS_PENDING.equals(status);
        boolean beyondAssigned = RepairOrder.STATUS_PROCESSING.equals(status)
                || RepairOrder.STATUS_TO_VERIFY.equals(status)
                || RepairOrder.STATUS_COMPLETED.equals(status);
        boolean beyondProcessing = RepairOrder.STATUS_TO_VERIFY.equals(status)
                || RepairOrder.STATUS_COMPLETED.equals(status);
        boolean cancelled = RepairOrder.STATUS_CANCELLED.equals(status);

        RepairOrder order = new RepairOrder();
        order.setTitle(title);
        order.setCategory(faultCategory);
        order.setDeviceId(device.getId());
        order.setReporterId(reporter.getId());
        order.setBuilding(device.getBuilding());
        order.setRoom(device.getRoom());
        order.setDescription(title + "，请尽快处理。");
        order.setPriority(priority);
        order.setStatus(status);
        order.setCreatedAt(created);
        // SLA：创建时间 + 品类 sla_hours
        order.setSlaDeadline(created.plusHours(slaMap.getOrDefault(device.getCategoryId(), 48)));
        orderMapper.insert(order);
        order.setOrderNo("RO" + created.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%05d", order.getId()));

        // 时间线按流转补齐
        seedTimeline(order.getId(), RepairTimeline.ACTION_CREATE, reporter.getId(), "提交报修：" + title, created);
        if (cancelled) {
            if (assignee != null) {
                order.setAssigneeId(assignee.getId());
                order.setAssignedAt(created.plusHours(1));
                seedTimeline(order.getId(), RepairTimeline.ACTION_ASSIGN, null,
                        "派单给维修工：" + assignee.getRealName(), created.plusHours(1));
            }
            seedTimeline(order.getId(), RepairTimeline.ACTION_CANCEL, reporter.getId(),
                    "用户取消报修：问题已自行解决", created.plusHours(5));
        } else if (beyondPending) {
            order.setAssigneeId(assignee.getId());
            order.setAssignedAt(created.plusHours(1));
            seedTimeline(order.getId(), RepairTimeline.ACTION_ASSIGN, null,
                    "派单给维修工：" + assignee.getRealName(), created.plusHours(1));
        }
        if (!cancelled && beyondAssigned) {
            seedTimeline(order.getId(), RepairTimeline.ACTION_START, assignee.getId(),
                    "维修工接单，开始处理", created.plusHours(2));
        }
        if (!cancelled && beyondProcessing && finishHours != null) {
            order.setFinishedAt(created.plusHours(finishHours));
            seedTimeline(order.getId(), RepairTimeline.ACTION_FINISH, assignee.getId(),
                    "维修完成：已修复并现场测试通过", created.plusHours(finishHours));
        }
        if (RepairOrder.STATUS_COMPLETED.equals(status)) {
            LocalDateTime verifiedAt = (order.getFinishedAt() == null ? created.plusHours(24)
                    : order.getFinishedAt()).plusHours(2);
            order.setVerifiedAt(verifiedAt);
            seedTimeline(order.getId(), RepairTimeline.ACTION_VERIFY_PASS, reporter.getId(),
                    "验收通过", verifiedAt);
            if (evalScore != null) {
                RepairEvaluation evaluation = new RepairEvaluation();
                evaluation.setOrderId(order.getId());
                evaluation.setScore(evalScore);
                evaluation.setComment(evalScore >= 4 ? "响应及时，维修专业" : "修复较慢，效果一般");
                evaluation.setCreatedAt(verifiedAt);
                evaluationMapper.insert(evaluation);
            }
        }
        orderMapper.updateById(order);
    }

    private void seedTimeline(Long orderId, String action, Long operatorId, String content, LocalDateTime time) {
        RepairTimeline timeline = new RepairTimeline();
        timeline.setOrderId(orderId);
        timeline.setAction(action);
        timeline.setOperatorId(operatorId);
        timeline.setContent(content);
        timeline.setCreatedAt(time);
        timelineMapper.insert(timeline);
    }
}
