package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "管理员售后售后详情响应")
@Data
public class AdminAfterSalesDetailResponse {

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

    @Schema(description = "description")
    private String description;

    @Schema(description = "退款金额")
    private String refundAmount;

    @Schema(description = "管理员备注")
    private String adminRemark;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "联系地址")
    private String contactAddress;

    @Schema(description = "创建时间")
    private Long createdTime;

    @Schema(description = "更新时间")
    private Long updatedTime;

    @Schema(description = "processed时间")
    private Long processedTime;

    @Schema(description = "completed时间")
    private Long completedTime;

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

    @Schema(description = "订单状态")
    private Integer orderStatus;

    @Schema(description = "排序状态Text")
    private String orderStatusText;

    @Schema(description = "支付状态Text")
    private String paymentStatusText;

    @Schema(description = "服务类型名称")
    private String serviceTypeName;

    @Schema(description = "服务分类名称")
    private String serviceCategoryName;

    @Schema(description = "服务ModeText")
    private String serviceModeText;

    @Schema(description = "服务地址")
    private String serviceAddress;

    @Schema(description = "总金额")
    private String totalAmount;

    @Schema(description = "paid金额")
    private String paidAmount;

    @Schema(description = "canProcess")
    private Boolean canProcess;

    private List<AdminAfterSalesMediaItemResponse> evidenceImages = new ArrayList<>();
    private List<AdminAfterSalesMediaItemResponse> evidenceVideos = new ArrayList<>();
}
