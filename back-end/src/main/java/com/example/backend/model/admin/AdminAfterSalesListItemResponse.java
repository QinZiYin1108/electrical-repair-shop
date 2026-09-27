package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理员售后售后列表Item响应")
@Data
public class AdminAfterSalesListItemResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "排序ID")
    private String orderId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "状态Text")
    private String statusText;

    @Schema(description = "application类型")
    private Integer applicationType;

    @Schema(description = "application类型Text")
    private String applicationTypeText;

    @Schema(description = "原因")
    private String reason;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名称")
    private String userName;

    @Schema(description = "用户手机号")
    private String userPhone;

    @Schema(description = "师傅ID")
    private String technicianId;

    @Schema(description = "师傅名称")
    private String technicianName;

    @Schema(description = "服务类型名称")
    private String serviceTypeName;

    @Schema(description = "服务分类名称")
    private String serviceCategoryName;

    @Schema(description = "创建时间")
    private Long createdTime;

    @Schema(description = "更新时间")
    private Long updatedTime;
}
