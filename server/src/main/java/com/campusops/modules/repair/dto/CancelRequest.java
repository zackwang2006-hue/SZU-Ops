package com.campusops.modules.repair.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 取消工单请求
 */
@Data
public class CancelRequest {

    @Size(max = 500, message = "取消原因不能超过 500 字")
    private String reason;
}
