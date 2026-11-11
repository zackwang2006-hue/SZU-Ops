package com.campusops.modules.prediction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预测性维护风险记录（对应 prediction_risk 表）
 */
@Data
@TableName("prediction_risk")
public class PredictionRisk {

    // 风险等级常量
    public static final String LEVEL_LOW = "LOW";
    public static final String LEVEL_MEDIUM = "MEDIUM";
    public static final String LEVEL_HIGH = "HIGH";
    public static final String LEVEL_CRITICAL = "CRITICAL";

    // 状态常量
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_DISMISSED = "DISMISSED";
    public static final String STATUS_CONVERTED = "CONVERTED";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long deviceId;

    /** LOW/MEDIUM/HIGH/CRITICAL */
    private String riskLevel;

    /** 0-1 风险分 */
    private Double riskScore;

    /** 可解释的风险原因 */
    private String reason;

    /** 按历史故障间隔均值外推的预测故障日 */
    private LocalDate predictedFailureDate;

    /** OPEN/DISMISSED/CONVERTED */
    private String status;

    private LocalDateTime createdAt;

    // ---- 展示用冗余字段（非数据库列，查询时填充） ----
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String deviceName;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String deviceCode;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String building;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String room;
}
