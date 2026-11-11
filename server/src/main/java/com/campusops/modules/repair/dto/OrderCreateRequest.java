package com.campusops.modules.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 报修提交请求
 */
@Data
public class OrderCreateRequest {

    @NotBlank(message = "工单标题不能为空")
    @Size(max = 200, message = "标题不能超过 200 字")
    private String title;

    /** 故障类型（如 硬件故障/制冷异常），可不填 */
    private String category;

    /** 关联设备，可为空（如公共区域的报修） */
    private Long deviceId;

    @NotBlank(message = "所在楼栋不能为空")
    private String building;

    private String room;

    @NotBlank(message = "故障描述不能为空")
    @Size(max = 1000, message = "描述不能超过 1000 字")
    private String description;

    /** 图片地址，逗号分隔 */
    private String images;

    @Pattern(regexp = "LOW|MEDIUM|HIGH|URGENT", message = "优先级不合法")
    private String priority;
}
