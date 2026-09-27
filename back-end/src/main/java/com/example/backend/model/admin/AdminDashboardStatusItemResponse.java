package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理员仪表盘状态Item响应")
@Data
public class AdminDashboardStatusItemResponse {

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "标签")
    private String label;

    @Schema(description = "数量")
    private Long count;
}
