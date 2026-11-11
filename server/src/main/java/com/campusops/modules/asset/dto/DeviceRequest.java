package com.campusops.modules.asset.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 设备台账新增/修改请求
 */
@Data
public class DeviceRequest {

    @NotBlank(message = "设备编号不能为空")
    private String code;

    @NotBlank(message = "设备名称不能为空")
    private String name;

    @NotNull(message = "所属品类不能为空")
    private Long categoryId;

    @NotBlank(message = "所在楼栋不能为空")
    private String building;

    private String room;

    private LocalDate purchaseDate;

    private String supplier;

    @DecimalMin(value = "0", message = "价格不能为负数")
    private BigDecimal price;

    /** IN_USE/REPAIRING/SCRAPPED，默认 IN_USE */
    @Pattern(regexp = "IN_USE|REPAIRING|SCRAPPED", message = "设备状态不合法")
    private String status;
}
