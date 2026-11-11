package com.campusops.modules.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单时间线（对应 repair_timeline 表），记录每个动作
 */
@Data
@TableName("repair_timeline")
public class RepairTimeline {

    // 动作常量
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_ASSIGN = "ASSIGN";
    public static final String ACTION_START = "START";
    public static final String ACTION_FINISH = "FINISH";
    public static final String ACTION_VERIFY_PASS = "VERIFY_PASS";
    public static final String ACTION_VERIFY_FAIL = "VERIFY_FAIL";
    public static final String ACTION_URGE = "URGE";
    public static final String ACTION_CANCEL = "CANCEL";
    public static final String ACTION_ESCALATE = "ESCALATE";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String action;

    /** 操作人；系统动作为 null */
    private Long operatorId;

    private String content;

    private String images;

    private LocalDateTime createdAt;

    /** 操作人姓名（非数据库列，查询时填充） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String operatorName;
}
