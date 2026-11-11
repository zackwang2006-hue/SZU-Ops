package com.campusops.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 设备品类（对应 asset_category 表）
 */
@Data
@TableName("asset_category")
public class AssetCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    /** 预期寿命（年），用于预测模块的服役年限因子 */
    private Integer expectedLifespanYears;

    /** 该品类工单 SLA 时限（小时） */
    private Integer slaHours;
}
