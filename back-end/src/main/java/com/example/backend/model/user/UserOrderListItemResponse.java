package com.example.backend.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户订单列表Item响应")
@Data
public class UserOrderListItemResponse {

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

    @Schema(description = "师傅ID")
    private String technicianId;

    @Schema(description = "师傅名称")
    private String technicianName;

    @Schema(description = "师傅手机号")
    private String technicianPhone;

    @Schema(description = "服务地址")
    private String serviceAddress;

    @Schema(description = "服务地址Short")
    private String serviceAddressShort;

    @Schema(description = "联系名称")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

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

    @Schema(description = "has上门二维码")
    private Boolean hasDoorQr;

    @Schema(description = "can申请售后销量")
    private Boolean canApplyAfterSales;

    @Schema(description = "has售后销量Entry")
    private Boolean hasAfterSalesEntry;

    @Schema(description = "售后销量Tip")
    private String afterSalesTip;

    @Schema(description = "can确认完成")
    private Boolean canConfirmCompletion;

    @Schema(description = "确认完成Tip")
    private String confirmCompletionTip;
}
