package com.campusops.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 设备台账（对应 asset_device 表）
 */
@Data
@TableName("asset_device")
public class AssetDevice {

    // 状态常量
    public static final String STATUS_IN_USE = "IN_USE";
    public static final String STATUS_REPAIRING = "REPAIRING";
    public static final String STATUS_SCRAPPED = "SCRAPPED";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备编号（唯一） */
    private String code;

    private String name;

    private Long categoryId;

    private String building;

    private String room;

    private LocalDate purchaseDate;

    private String supplier;

    private BigDecimal price;

    /** IN_USE/REPAIRING/SCRAPPED */
    private String status;

    /** 健康度 0-100，由预测模块每日重算回写 */
    private Integer healthScore;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 品类名称（非数据库列，查询时填充） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String categoryName;
}
