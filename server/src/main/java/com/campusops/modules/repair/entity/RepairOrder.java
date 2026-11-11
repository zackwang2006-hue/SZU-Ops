package com.campusops.modules.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报修工单（对应 repair_order 表）
 * 状态机：PENDING → ASSIGNED → PROCESSING → TO_VERIFY → COMPLETED
 *        任意进行中状态 → CANCELLED；TO_VERIFY 验收不通过退回 PROCESSING
 */
@Data
@TableName("repair_order")
public class RepairOrder {

    // 工单状态常量
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ASSIGNED = "ASSIGNED";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_TO_VERIFY = "TO_VERIFY";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    // 优先级常量
    public static final String PRIORITY_LOW = "LOW";
    public static final String PRIORITY_MEDIUM = "MEDIUM";
    public static final String PRIORITY_HIGH = "HIGH";
    public static final String PRIORITY_URGENT = "URGENT";

    /** 巡检工单专用 category 值（由预测风险转化生成） */
    public static final String CATEGORY_INSPECTION = "INSPECTION";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private String title;

    /** 故障类型；巡检工单固定为 INSPECTION */
    private String category;

    private Long deviceId;

    private Long reporterId;

    private String building;

    private String room;

    private String description;

    /** 图片地址，逗号分隔 */
    private String images;

    /** LOW/MEDIUM/HIGH/URGENT */
    private String priority;

    private String status;

    private Long assigneeId;

    /** SLA 截止时间 = 创建时间 + 品类 sla_hours */
    private LocalDateTime slaDeadline;

    private LocalDateTime createdAt;

    private LocalDateTime assignedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime verifiedAt;

    // ---- 展示用冗余字段（非数据库列，查询时填充） ----
    /** 报修人姓名 */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String reporterName;

    /** 维修工姓名 */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String assigneeName;

    /** 关联设备名称 */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String deviceName;

    /** 工单时间线（非数据库列，列表/详情查询时填充） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private java.util.List<RepairTimeline> timeline;
}
