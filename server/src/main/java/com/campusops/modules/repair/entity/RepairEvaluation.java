package com.campusops.modules.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单评价（对应 repair_evaluation 表）
 */
@Data
@TableName("repair_evaluation")
public class RepairEvaluation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    /** 满意度 1-5 */
    private Integer score;

    private String comment;

    private LocalDateTime createdAt;
}
