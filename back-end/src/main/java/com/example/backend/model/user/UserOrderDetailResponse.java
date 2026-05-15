package com.example.backend.model.user;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserOrderDetailResponse {

    private String id;
    private String orderNo;
    private Integer status;
    private String statusText;
    private Integer paymentStatus;
    private String paymentStatusText;
    private String serviceTypeId;
    private String serviceTypeName;
    private String serviceCategoryId;
    private String serviceCategoryName;
    private String serviceCategoryPath;
    private Integer serviceMode;
    private String serviceModeText;
    private String technicianId;
    private String technicianName;
    private String technicianPhone;
    private String serviceAddressId;
    private String serviceAddress;
    private String serviceAddressShort;
    private String contactName;
    private String contactPhone;
    private String applianceBrand;
    private String applianceModel;
    private Long purchaseDate;
    private String faultSummary;
    private String totalAmount;
    private String paidAmount;
    private Long appointmentTime;
    private Long createdTime;
    private Long updatedTime;
    private Boolean hasDoorQr;

    private String doorFee;
    private String distanceFee;
    private String serviceFee;
    private String materialFee;
    private String overtimeFee;
    private String remark;
    private String inspectionDiagnosis;
    private String repairPlan;
    private Long inspectionTime;
    private String cancelReason;
    private Long cancelTime;
    private String refundReason;
    private String refundAmount;
    private Long refundTime;
    private Boolean canCancel;
    private String cancelTip;
    private String cancelRefundAmount;
    private Boolean canModifyOrder;
    private Boolean canModifyAppointment;
    private Boolean canConfirmCompletion;
    private String confirmCompletionTip;
    private Boolean canApplyAfterSales;
    private String afterSalesTip;
    private Boolean canReview;
    private Boolean hasReview;
    private String reviewId;
    private UserAfterSalesApplicationSummary afterSalesApplication;

    private List<UserOrderFaultItemResponse> faultList = new ArrayList<>();
    private List<UserOrderMediaItemResponse> inspectionImages = new ArrayList<>();
    private List<UserOrderMediaItemResponse> inspectionVideos = new ArrayList<>();
    private List<UserOrderProgressItemResponse> progressList = new ArrayList<>();
}
