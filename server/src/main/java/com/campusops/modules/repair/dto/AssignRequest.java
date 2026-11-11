package com.campusops.modules.repair.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 派单请求
 */
@Data
public class AssignRequest {

    @NotNull(message = "维修人员 ID 不能为空")
    private Long technicianId;
}
