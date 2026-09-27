package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理员预约订单列表Item响应")
@Data
public class AdminReserveOrderListItemResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "状态Text")
    private String statusText;

    @Schema(description = "支付状态")
    private Integer paymentStatus;

    @Schema(description = "支付状态Text")
    private String paymentStatusText;

    @Schema(description = "服务类型ID")
    private String serviceTypeId;

    @Schema(description = "服务类型名称")
    private String serviceTypeName;

    @Schema(description = "服务分类ID")
    private String serviceCategoryId;

    @Schema(description = "服务分类名称")
    private String serviceCategoryName;

    @Schema(description = "服务分类路径")
    private String serviceCategoryPath;

    @Schema(description = "服务Mode")
    private Integer serviceMode;

    @Schema(description = "服务ModeText")
    private String serviceModeText;

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

    @Schema(description = "师傅手机号")
    private String technicianPhone;

    @Schema(description = "联系名称")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "服务地址")
    private String serviceAddress;

    @Schema(description = "电器品牌")
    private String applianceBrand;

    @Schema(description = "电器型号")
    private String applianceModel;

    @Schema(description = "故障Summary")
    private String faultSummary;

    @Schema(description = "总金额")
    private String totalAmount;

    @Schema(description = "paid金额")
    private String paidAmount;

    @Schema(description = "预约时间")
    private Long appointmentTime;

    @Schema(description = "创建时间")
    private Long createdTime;

    @Schema(description = "更新时间")
    private Long updatedTime;

    @Schema(description = "取消原因")
    private String cancelReason;

    @Schema(description = "取消原因标签")
    private String cancelReasonLabel;
}
