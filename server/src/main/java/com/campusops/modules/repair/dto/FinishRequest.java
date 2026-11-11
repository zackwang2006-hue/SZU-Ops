package com.campusops.modules.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 完工请求（维修报告）
 */
@Data
public class FinishRequest {

    @NotBlank(message = "维修报告不能为空")
    @Size(max = 1000, message = "维修报告不能超过 1000 字")
    private String report;
}
