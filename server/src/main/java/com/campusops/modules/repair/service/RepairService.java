package com.campusops.modules.repair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusops.common.BizException;
import com.campusops.config.LoginUser;
import com.campusops.config.SecurityUtils;
import com.campusops.modules.asset.entity.AssetCategory;
import com.campusops.modules.asset.entity.AssetDevice;
import com.campusops.modules.asset.mapper.AssetCategoryMapper;
import com.campusops.modules.asset.mapper.AssetDeviceMapper;
import com.campusops.modules.auth.entity.SysUser;
import com.campusops.modules.auth.mapper.SysUserMapper;
import com.campusops.modules.repair.dto.*;
import com.campusops.modules.repair.entity.RepairEvaluation;
import com.campusops.modules.repair.entity.RepairOrder;
import com.campusops.modules.repair.entity.RepairTimeline;
import com.campusops.modules.repair.mapper.RepairEvaluationMapper;
import com.campusops.modules.repair.mapper.RepairOrderMapper;
import com.campusops.modules.repair.mapper.RepairTimelineMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报修工单服务：状态机流转 + 时间线 + SLA
 * 状态机（见 docs/ARCHITECTURE.md 第 6 节）：
 * PENDING --assign--> ASSIGNED --start--> PROCESSING --finish--> TO_VERIFY
 * TO_VERIFY --verify(pass)--> COMPLETED；TO_VERIFY --verify(fail)--> PROCESSING
 * 任意进行中状态 --cancel--> CANCELLED
 */
@Service
public class RepairService {

    /** 未关联设备时的默认 SLA 时限（小时） */
    private static final int DEFAULT_SLA_HOURS = 48;

    private final RepairOrderMapper orderMapper;
    private final RepairTimelineMapper timelineMapper;
    private final RepairEvaluationMapper evaluationMapper;
    private final AssetDeviceMapper deviceMapper;
    private final AssetCategoryMapper categoryMapper;
    private final SysUserMapper userMapper;

    public RepairService(RepairOrderMapper orderMapper, RepairTimelineMapper timelineMapper,
                         RepairEvaluationMapper evaluationMapper, AssetDeviceMapper deviceMapper,
                         AssetCategoryMapper categoryMapper, SysUserMapper userMapper) {
        this.orderMapper = orderMapper;
        this.timelineMapper = timelineMapper;
        this.evaluationMapper = evaluationMapper;
        this.deviceMapper = deviceMapper;
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
    }

    /** 工单分页查询：多条件过滤 */
    public Page<RepairOrder> page(int pageNum, int pageSize, String status, String priority,
                                  String building, String category, Long reporterId,
                                  Long assigneeId, String keyword) {
        LambdaQueryWrapper<RepairOrder> wrapper = new LambdaQueryWrapper<RepairOrder>()
                .eq(StringUtils.hasText(status), RepairOrder::getStatus, status)
                .eq(StringUtils.hasText(priority), RepairOrder::getPriority, priority)
                .eq(StringUtils.hasText(building), RepairOrder::getBuilding, building)
                .eq(StringUtils.hasText(category), RepairOrder::getCategory, category)
                .eq(reporterId != null, RepairOrder::getReporterId, reporterId)
                .eq(assigneeId != null, RepairOrder::getAssigneeId, assigneeId)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(RepairOrder::getTitle, keyword)
                        .or()
                        .like(RepairOrder::getOrderNo, keyword))
                .orderByDesc(RepairOrder::getId);
        Page<RepairOrder> page = orderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        page.getRecords().forEach(order -> {
            fillDisplayNames(order);
            order.setTimeline(listTimeline(order.getId()));
        });
        return page;
    }

    /** 填充展示用冗余字段：报修人/维修工姓名、设备名称 */
    private void fillDisplayNames(RepairOrder order) {
        if (order.getReporterId() != null) {
            SysUser reporter = userMapper.selectById(order.getReporterId());
            if (reporter != null) {
                order.setReporterName(reporter.getRealName());
            }
        }
        if (order.getAssigneeId() != null) {
            SysUser assignee = userMapper.selectById(order.getAssigneeId());
            if (assignee != null) {
                order.setAssigneeName(assignee.getRealName());
            }
        }
        if (order.getDeviceId() != null) {
            AssetDevice device = deviceMapper.selectById(order.getDeviceId());
            if (device != null) {
                order.setDeviceName(device.getName());
            }
        }
    }

    /** 维修工列表（派单选择用） */
    public List<SysUser> listTechnicians() {
        List<SysUser> list = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, SysUser.ROLE_TECHNICIAN));
        list.forEach(u -> u.setPassword(null));
        return list;
    }

    /** 工单详情：工单 + 时间线 + 评价 */
    public Map<String, Object> detail(Long id) {
        RepairOrder order = mustGet(id);
        fillDisplayNames(order);
        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("timeline", listTimeline(id));
        result.put("evaluation", evaluationMapper.selectOne(
                new LambdaQueryWrapper<RepairEvaluation>().eq(RepairEvaluation::getOrderId, id).last("LIMIT 1")));
        return result;
    }

    public List<RepairTimeline> listTimeline(Long orderId) {
        List<RepairTimeline> timeline = timelineMapper.selectList(new LambdaQueryWrapper<RepairTimeline>()
                .eq(RepairTimeline::getOrderId, orderId)
                .orderByAsc(RepairTimeline::getId));
        // 填充操作人姓名
        timeline.forEach(t -> {
            if (t.getOperatorId() != null) {
                SysUser operator = userMapper.selectById(t.getOperatorId());
                if (operator != null) {
                    t.setOperatorName(operator.getRealName());
                }
            }
        });
        return timeline;
    }

    /** 提交报修：创建工单并按品类 sla_hours 计算 sla_deadline */
    @Transactional
    public RepairOrder create(OrderCreateRequest request) {
        LoginUser current = SecurityUtils.current();
        LocalDateTime now = LocalDateTime.now();

        RepairOrder order = new RepairOrder();
        order.setTitle(request.getTitle());
        order.setCategory(request.getCategory());
        order.setDeviceId(request.getDeviceId());
        order.setBuilding(request.getBuilding());
        order.setRoom(request.getRoom());
        order.setDescription(request.getDescription());
        order.setImages(request.getImages());
        order.setPriority(StringUtils.hasText(request.getPriority())
                ? request.getPriority() : RepairOrder.PRIORITY_MEDIUM);
        order.setReporterId(current.getId());
        order.setStatus(RepairOrder.STATUS_PENDING);
        order.setCreatedAt(now);

        // SLA：优先取关联设备所属品类的 sla_hours
        int slaHours = DEFAULT_SLA_HOURS;
        if (request.getDeviceId() != null) {
            AssetDevice device = deviceMapper.selectById(request.getDeviceId());
            if (device == null) {
                throw new BizException("关联设备不存在，id=" + request.getDeviceId());
            }
            AssetCategory category = device.getCategoryId() == null
                    ? null : categoryMapper.selectById(device.getCategoryId());
            if (category != null && category.getSlaHours() != null) {
                slaHours = category.getSlaHours();
            }
        }
        order.setSlaDeadline(now.plusHours(slaHours));

        orderMapper.insert(order);
        // 单号：RO + 日期 + 自增 ID
        order.setOrderNo("RO" + now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%05d", order.getId()));
        orderMapper.updateById(order);

        addTimeline(order.getId(), RepairTimeline.ACTION_CREATE, current.getId(), "提交报修：" + request.getTitle());
        return order;
    }

    /** 派单：PENDING → ASSIGNED */
    @Transactional
    public void assign(Long orderId, AssignRequest request) {
        RepairOrder order = mustGet(orderId);
        requireStatus(order, RepairOrder.STATUS_PENDING, "派单");

        SysUser technician = userMapper.selectById(request.getTechnicianId());
        if (technician == null || !SysUser.ROLE_TECHNICIAN.equals(technician.getRole())) {
            throw new BizException("维修人员不存在或角色不是维修工");
        }
        if (technician.getStatus() != null && technician.getStatus() == 0) {
            throw new BizException("该维修人员账号已停用");
        }

        order.setAssigneeId(technician.getId());
        order.setAssignedAt(LocalDateTime.now());
        order.setStatus(RepairOrder.STATUS_ASSIGNED);
        orderMapper.updateById(order);
        addTimeline(orderId, RepairTimeline.ACTION_ASSIGN, SecurityUtils.currentUserId(),
                "派单给维修工：" + technician.getRealName());
    }

    /** 开工：ASSIGNED → PROCESSING（仅接单人或管理角色） */
    @Transactional
    public void start(Long orderId) {
        RepairOrder order = mustGet(orderId);
        requireStatus(order, RepairOrder.STATUS_ASSIGNED, "开工");
        requireAssigneeOrAdmin(order, "开工");

        order.setStatus(RepairOrder.STATUS_PROCESSING);
        orderMapper.updateById(order);
        // 设备联动：标记维修中
        updateDeviceStatus(order.getDeviceId(), AssetDevice.STATUS_REPAIRING);
        addTimeline(orderId, RepairTimeline.ACTION_START, SecurityUtils.currentUserId(), "维修工接单，开始处理");
    }

    /** 完工：PROCESSING → TO_VERIFY */
    @Transactional
    public void finish(Long orderId, FinishRequest request) {
        RepairOrder order = mustGet(orderId);
        requireStatus(order, RepairOrder.STATUS_PROCESSING, "完工");
        requireAssigneeOrAdmin(order, "完工");

        order.setStatus(RepairOrder.STATUS_TO_VERIFY);
        order.setFinishedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        addTimeline(orderId, RepairTimeline.ACTION_FINISH, SecurityUtils.currentUserId(),
                "维修完成：" + request.getReport());
    }

    /** 验收：TO_VERIFY --pass--> COMPLETED；--fail--> PROCESSING（仅报修人或管理角色） */
    @Transactional
    public void verify(Long orderId, VerifyRequest request) {
        RepairOrder order = mustGet(orderId);
        requireStatus(order, RepairOrder.STATUS_TO_VERIFY, "验收");
        LoginUser current = SecurityUtils.current();
        if (!current.getId().equals(order.getReporterId())
                && !SecurityUtils.hasRole(SysUser.ROLE_ADMIN, SysUser.ROLE_DISPATCHER)) {
            throw new BizException(403, "只有报修人本人或管理角色可以验收");
        }

        if (Boolean.TRUE.equals(request.getPass())) {
            order.setStatus(RepairOrder.STATUS_COMPLETED);
            order.setVerifiedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            // 设备联动：恢复正常使用
            updateDeviceStatus(order.getDeviceId(), AssetDevice.STATUS_IN_USE);
            addTimeline(orderId, RepairTimeline.ACTION_VERIFY_PASS, current.getId(), "验收通过");
            // 满意度评价
            if (request.getScore() != null) {
                RepairEvaluation evaluation = new RepairEvaluation();
                evaluation.setOrderId(orderId);
                evaluation.setScore(request.getScore());
                evaluation.setComment(request.getComment());
                evaluation.setCreatedAt(LocalDateTime.now());
                evaluationMapper.insert(evaluation);
            }
        } else {
            // 验收不通过退回处理中；updateById 不更新 null 字段，用 UpdateWrapper 显式清空 finished_at
            orderMapper.update(null, new LambdaUpdateWrapper<RepairOrder>()
                    .eq(RepairOrder::getId, orderId)
                    .set(RepairOrder::getStatus, RepairOrder.STATUS_PROCESSING)
                    .set(RepairOrder::getFinishedAt, null));
            addTimeline(orderId, RepairTimeline.ACTION_VERIFY_FAIL, current.getId(),
                    "验收不通过，退回处理中" + (StringUtils.hasText(request.getComment()) ? "：" + request.getComment() : ""));
        }
    }

    /** 催单：进行中（PENDING/ASSIGNED/PROCESSING）可催，记录时间线 */
    @Transactional
    public void urge(Long orderId) {
        RepairOrder order = mustGet(orderId);
        if (!List.of(RepairOrder.STATUS_PENDING, RepairOrder.STATUS_ASSIGNED, RepairOrder.STATUS_PROCESSING)
                .contains(order.getStatus())) {
            throw new BizException("当前工单状态为「" + statusName(order.getStatus()) + "」，无法催单");
        }
        LoginUser current = SecurityUtils.current();
        if (!current.getId().equals(order.getReporterId()) && !SecurityUtils.hasRole(SysUser.ROLE_ADMIN)) {
            throw new BizException(403, "只有报修人本人可以催单");
        }
        addTimeline(orderId, RepairTimeline.ACTION_URGE, current.getId(), "用户催单，请尽快处理");
    }

    /** 取消：任意进行中状态 → CANCELLED */
    @Transactional
    public void cancel(Long orderId, CancelRequest request) {
        RepairOrder order = mustGet(orderId);
        if (RepairOrder.STATUS_COMPLETED.equals(order.getStatus())
                || RepairOrder.STATUS_CANCELLED.equals(order.getStatus())) {
            throw new BizException("当前工单状态为「" + statusName(order.getStatus()) + "」，无法取消");
        }
        LoginUser current = SecurityUtils.current();
        if (!current.getId().equals(order.getReporterId())
                && !SecurityUtils.hasRole(SysUser.ROLE_ADMIN, SysUser.ROLE_DISPATCHER)) {
            throw new BizException(403, "只有报修人本人或管理角色可以取消工单");
        }
        order.setStatus(RepairOrder.STATUS_CANCELLED);
        orderMapper.updateById(order);
        updateDeviceStatus(order.getDeviceId(), AssetDevice.STATUS_IN_USE);
        addTimeline(orderId, RepairTimeline.ACTION_CANCEL, current.getId(),
                "取消工单" + (StringUtils.hasText(request.getReason()) ? "：" + request.getReason() : ""));
    }

    /** SLA 超时列表：未完成且已过 sla_deadline */
    public List<RepairOrder> timeoutList() {
        return orderMapper.selectList(new LambdaQueryWrapper<RepairOrder>()
                .notIn(RepairOrder::getStatus, RepairOrder.STATUS_COMPLETED, RepairOrder.STATUS_CANCELLED)
                .lt(RepairOrder::getSlaDeadline, LocalDateTime.now())
                .orderByAsc(RepairOrder::getSlaDeadline));
    }

    /** 定时任务调用：超时未完成的工单自动提升优先级（见架构文档 SLA 规则） */
    @Transactional
    public int escalateTimeoutOrders() {
        List<RepairOrder> timeoutOrders = orderMapper.selectList(new LambdaQueryWrapper<RepairOrder>()
                .notIn(RepairOrder::getStatus, RepairOrder.STATUS_COMPLETED, RepairOrder.STATUS_CANCELLED)
                .lt(RepairOrder::getSlaDeadline, LocalDateTime.now())
                .ne(RepairOrder::getPriority, RepairOrder.PRIORITY_URGENT));
        for (RepairOrder order : timeoutOrders) {
            order.setPriority(nextPriority(order.getPriority()));
            orderMapper.updateById(order);
            addTimeline(order.getId(), RepairTimeline.ACTION_ESCALATE, null,
                    "SLA 超时，系统自动提升优先级为 " + order.getPriority());
        }
        return timeoutOrders.size();
    }

    // ---------- 内部工具 ----------

    public RepairOrder mustGet(Long id) {
        RepairOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException("工单不存在，id=" + id);
        }
        return order;
    }

    /** 状态机校验：状态不符时抛出友好错误 */
    private void requireStatus(RepairOrder order, String expected, String action) {
        if (!expected.equals(order.getStatus())) {
            throw new BizException("当前工单状态为「" + statusName(order.getStatus())
                    + "」，无法执行「" + action + "」操作（需要状态：「" + statusName(expected) + "」）");
        }
    }

    /** 操作人必须是接单人或管理角色 */
    private void requireAssigneeOrAdmin(RepairOrder order, String action) {
        LoginUser current = SecurityUtils.current();
        if (!current.getId().equals(order.getAssigneeId())
                && !SecurityUtils.hasRole(SysUser.ROLE_ADMIN, SysUser.ROLE_DISPATCHER)) {
            throw new BizException(403, "只有接单人或管理角色可以执行「" + action + "」");
        }
    }

    public void addTimeline(Long orderId, String action, Long operatorId, String content) {
        RepairTimeline timeline = new RepairTimeline();
        timeline.setOrderId(orderId);
        timeline.setAction(action);
        timeline.setOperatorId(operatorId);
        timeline.setContent(content);
        timeline.setCreatedAt(LocalDateTime.now());
        timelineMapper.insert(timeline);
    }

    private void updateDeviceStatus(Long deviceId, String status) {
        if (deviceId == null) {
            return;
        }
        AssetDevice device = deviceMapper.selectById(deviceId);
        if (device != null && !AssetDevice.STATUS_SCRAPPED.equals(device.getStatus())) {
            device.setStatus(status);
            device.setUpdatedAt(LocalDateTime.now());
            deviceMapper.updateById(device);
        }
    }

    /** 优先级升一档：LOW→MEDIUM→HIGH→URGENT */
    private String nextPriority(String priority) {
        return switch (priority == null ? RepairOrder.PRIORITY_MEDIUM : priority) {
            case RepairOrder.PRIORITY_LOW -> RepairOrder.PRIORITY_MEDIUM;
            case RepairOrder.PRIORITY_MEDIUM -> RepairOrder.PRIORITY_HIGH;
            default -> RepairOrder.PRIORITY_URGENT;
        };
    }

    /** 状态中文名，用于友好错误提示 */
    public static String statusName(String status) {
        return switch (status == null ? "" : status) {
            case RepairOrder.STATUS_PENDING -> "待派单";
            case RepairOrder.STATUS_ASSIGNED -> "已派单";
            case RepairOrder.STATUS_PROCESSING -> "处理中";
            case RepairOrder.STATUS_TO_VERIFY -> "待验收";
            case RepairOrder.STATUS_COMPLETED -> "已完成";
            case RepairOrder.STATUS_CANCELLED -> "已取消";
            default -> "未知";
        };
    }
}
