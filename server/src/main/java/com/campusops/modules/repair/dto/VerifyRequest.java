package com.campusops.modules.repair.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 验收请求：pass=true 验收通过（可带评分），pass=false 退回处理中
 */
@Data
public class VerifyRequest {

    @NotNull(message = "验收结果不能为空")
    private Boolean pass;

    /** 满意度评分 1-5（验收通过时填写） */
    @Min(value = 1, message = "评分范围 1-5")
    @Max(value = 5, message = "评分范围 1-5")
    private Integer score;

    @Size(max = 500, message = "评价内容不能超过 500 字")
    private String comment;
}
