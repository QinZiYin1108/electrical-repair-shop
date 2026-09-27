package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理员售后售后处理请求")
@Data
public class AdminAfterSalesProcessRequest {

    @Schema(description = "action")
    private String action;

    @Schema(description = "管理员备注")
    private String adminRemark;
}
