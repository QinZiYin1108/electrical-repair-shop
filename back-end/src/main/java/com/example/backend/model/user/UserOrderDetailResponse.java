package com.example.backend.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "用户订单详情响应")
@Data
public class UserOrderDetailResponse {

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

    @Schema(description = "服务地址ID")
    private String serviceAddressId;

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

    @Schema(description = "购买Date")
    private Long purchaseDate;

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

    @Schema(description = "上门费用")
    private String doorFee;

    @Schema(description = "距离费用")
    private String distanceFee;

    @Schema(description = "服务费用")
    private String serviceFee;

    @Schema(description = "material费用")
    private String materialFee;

    @Schema(description = "overtime费用")
    private String overtimeFee;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "inspectionDiagnosis")
    private String inspectionDiagnosis;

    @Schema(description = "维修Plan")
    private String repairPlan;

    @Schema(description = "inspection时间")
    private Long inspectionTime;

    @Schema(description = "取消原因")
    private String cancelReason;

    @Schema(description = "取消时间")
    private Long cancelTime;

    @Schema(description = "退款原因")
    private String refundReason;

    @Schema(description = "退款金额")
    private String refundAmount;

    @Schema(description = "退款时间")
    private Long refundTime;

    @Schema(description = "can取消")
    private Boolean canCancel;

    @Schema(description = "取消Tip")
    private String cancelTip;

    @Schema(description = "取消退款金额")
    private String cancelRefundAmount;

    @Schema(description = "canModify排序")
    private Boolean canModifyOrder;

    @Schema(description = "canModify预约")
    private Boolean canModifyAppointment;

    @Schema(description = "can确认完成")
    private Boolean canConfirmCompletion;

    @Schema(description = "确认完成Tip")
    private String confirmCompletionTip;

    @Schema(description = "can申请售后销量")
    private Boolean canApplyAfterSales;

    @Schema(description = "售后销量Tip")
    private String afterSalesTip;

    @Schema(description = "can评价")
    private Boolean canReview;

    @Schema(description = "has评价")
    private Boolean hasReview;

    @Schema(description = "评价ID")
    private String reviewId;

    @Schema(description = "售后销量Application")
    private UserAfterSalesApplicationSummary afterSalesApplication;

    private List<UserOrderFaultItemResponse> faultList = new ArrayList<>();
    private List<UserOrderMediaItemResponse> inspectionImages = new ArrayList<>();
    private List<UserOrderMediaItemResponse> inspectionVideos = new ArrayList<>();
    private List<UserOrderProgressItemResponse> progressList = new ArrayList<>();
}
