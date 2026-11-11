package com.campusops.modules.asset.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 设备品类新增/修改请求
 */
@Data
public class CategoryRequest {

    @NotBlank(message = "品类名称不能为空")
    private String name;

    @NotBlank(message = "品类编码不能为空")
    private String code;

    @NotNull(message = "预期寿命不能为空")
    @Min(value = 1, message = "预期寿命至少 1 年")
    private Integer expectedLifespanYears;

    @NotNull(message = "SLA 时限不能为空")
    @Min(value = 1, message = "SLA 时限至少 1 小时")
    private Integer slaHours;
}
