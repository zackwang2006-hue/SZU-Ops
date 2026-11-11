package com.campusops.modules.dashboard.service;

import com.campusops.modules.auth.entity.SysUser;
import com.campusops.modules.auth.mapper.SysUserMapper;
import com.campusops.modules.repair.entity.RepairEvaluation;
import com.campusops.modules.repair.entity.RepairOrder;
import com.campusops.modules.repair.mapper.RepairEvaluationMapper;
import com.campusops.modules.repair.mapper.RepairOrderMapper;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据看板服务：工单量/完成率/SLA 达标率/趋势/热力/品类分布/维修人员绩效
 */
@Service
public class DashboardService {

    private final RepairOrderMapper orderMapper;
    private final RepairEvaluationMapper evaluationMapper;
    private final SysUserMapper userMapper;

    public DashboardService(RepairOrderMapper orderMapper, RepairEvaluationMapper evaluationMapper,
                            SysUserMapper userMapper) {
        this.orderMapper = orderMapper;
        this.evaluationMapper = evaluationMapper;
        this.userMapper = userMapper;
    }

    public Map<String, Object> overview() {
        // 演示规模下全量加载后在内存聚合，保证 H2/MySQL 行为一致
        List<RepairOrder> all = orderMapper.selectList(null);
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        Map<String, Object> result = new LinkedHashMap<>();

        // ---- 今日/本周工单量 ----
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime weekStart = today.with(DayOfWeek.MONDAY).atStartOfDay();
        result.put("todayOrders", all.stream()
                .filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isBefore(dayStart)).count());
        result.put("weekCount", all.stream()
                .filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isBefore(weekStart)).count());
        result.put("totalOrders", all.size());

        // ---- 完成率：已完成 / (全部 - 已取消) ----
        List<RepairOrder> completed = all.stream()
                .filter(o -> RepairOrder.STATUS_COMPLETED.equals(o.getStatus())).toList();
        long cancelled = all.stream()
                .filter(o -> RepairOrder.STATUS_CANCELLED.equals(o.getStatus())).count();
        long effective = all.size() - cancelled;
        result.put("completionRate", effective == 0 ? 0.0 : round2(completed.size() * 1.0 / effective));

        // ---- SLA 达标率：已完成工单中 finished_at <= sla_deadline ----
        long onTime = completed.stream()
                .filter(o -> o.getFinishedAt() != null && o.getSlaDeadline() != null
                        && !o.getFinishedAt().isAfter(o.getSlaDeadline())).count();
        result.put("slaRate", completed.isEmpty() ? 0.0 : round2(onTime * 1.0 / completed.size()));

        // ---- 整体平均修复时长（小时）：assigned_at → finished_at ----
        OptionalDouble overallAvgHours = completed.stream()
                .filter(o -> o.getAssignedAt() != null && o.getFinishedAt() != null)
                .mapToLong(o -> ChronoUnit.MINUTES.between(o.getAssignedAt(), o.getFinishedAt()))
                .average()
                .stream().map(minutes -> minutes / 60.0).findFirst();
        result.put("avgRepairHours", overallAvgHours.isPresent() ? round2(overallAvgHours.getAsDouble()) : null);

        // ---- 进行中/超时工单数 ----
        result.put("pendingOrders", all.stream().filter(o ->
                !RepairOrder.STATUS_COMPLETED.equals(o.getStatus())
                        && !RepairOrder.STATUS_CANCELLED.equals(o.getStatus())).count());
        result.put("timeoutCount", all.stream().filter(o ->
                !RepairOrder.STATUS_COMPLETED.equals(o.getStatus())
                        && !RepairOrder.STATUS_CANCELLED.equals(o.getStatus())
                        && o.getSlaDeadline() != null && o.getSlaDeadline().isBefore(now)).count());

        // ---- 近30天工单趋势（按创建日期，补零） ----
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, Long> trendMap = new LinkedHashMap<>();
        for (int i = 29; i >= 0; i--) {
            trendMap.put(today.minusDays(i).format(dayFmt), 0L);
        }
        for (RepairOrder o : all) {
            if (o.getCreatedAt() != null) {
                String day = o.getCreatedAt().toLocalDate().format(dayFmt);
                if (trendMap.containsKey(day)) {
                    trendMap.merge(day, 1L, Long::sum);
                }
            }
        }
        List<Map<String, Object>> trend30d = trendMap.entrySet().stream()
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("date", e.getKey());
                    item.put("count", e.getValue());
                    return item;
                }).toList();
        result.put("trend", trend30d);

        // ---- 按楼栋热力分布 ----
        result.put("buildingHeat", groupCount(all, o ->
                o.getBuilding() == null ? "未知" : o.getBuilding(), "building"));

        // ---- 按品类（故障类型）分布 ----
        result.put("categoryDist", groupCount(all, o ->
                o.getCategory() == null ? "其他" : o.getCategory(), "category"));

        // ---- 维修人员绩效 ----
        result.put("technicianPerf", technicianPerformance(all));

        return result;
    }

    /** 通用分组计数 */
    private List<Map<String, Object>> groupCount(List<RepairOrder> orders,
                                                 java.util.function.Function<RepairOrder, String> classifier,
                                                 String keyName) {
        return orders.stream()
                .collect(Collectors.groupingBy(classifier, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put(keyName, e.getKey());
                    item.put("count", e.getValue());
                    return item;
                }).toList();
    }

    /** 维修人员绩效：平均修复时长/完成率/平均满意度 */
    private List<Map<String, Object>> technicianPerformance(List<RepairOrder> all) {
        // 工单评价：orderId -> score
        Map<Long, Integer> scoreByOrder = evaluationMapper.selectList(null).stream()
                .filter(e -> e.getScore() != null)
                .collect(Collectors.toMap(RepairEvaluation::getOrderId, RepairEvaluation::getScore, (a, b) -> a));
        // 人员姓名
        Map<Long, String> userNames = userMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName, (a, b) -> a));

        Map<Long, List<RepairOrder>> byTechnician = all.stream()
                .filter(o -> o.getAssigneeId() != null)
                .collect(Collectors.groupingBy(RepairOrder::getAssigneeId));

        List<Map<String, Object>> performance = new ArrayList<>();
        for (Map.Entry<Long, List<RepairOrder>> entry : byTechnician.entrySet()) {
            Long techId = entry.getKey();
            List<RepairOrder> orders = entry.getValue();
            List<RepairOrder> done = orders.stream()
                    .filter(o -> RepairOrder.STATUS_COMPLETED.equals(o.getStatus())).toList();

            // 平均修复时长（小时）：assigned_at → finished_at
            OptionalDouble avgHours = done.stream()
                    .filter(o -> o.getAssignedAt() != null && o.getFinishedAt() != null)
                    .mapToLong(o -> ChronoUnit.MINUTES.between(o.getAssignedAt(), o.getFinishedAt()))
                    .average()
                    .stream().map(minutes -> minutes / 60.0).findFirst();

            // 平均满意度
            OptionalDouble avgScore = done.stream()
                    .map(o -> scoreByOrder.get(o.getId()))
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .average();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("technicianId", techId);
            item.put("technicianName", userNames.getOrDefault(techId, "未知"));
            item.put("assignedCount", orders.size());
            item.put("finishedCount", done.size());
            item.put("completionRate", orders.isEmpty() ? 0.0 : round2(done.size() * 1.0 / orders.size()));
            item.put("avgRepairHours", avgHours.isPresent() ? round2(avgHours.getAsDouble()) : null);
            item.put("satisfaction", avgScore.isPresent() ? round2(avgScore.getAsDouble()) : null);
            performance.add(item);
        }
        performance.sort((a, b) -> Integer.compare((int) b.get("finishedCount"), (int) a.get("finishedCount")));
        return performance;
    }

    private double round2(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
