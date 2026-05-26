package com.example.backend.model.user;

import lombok.Data;

@Data
public class UserOrderListItemResponse {

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
    private String serviceAddress;
    private String serviceAddressShort;
    private String contactName;
    private String contactPhone;
    private String applianceBrand;
    private String applianceModel;
    private String faultSummary;
    private String totalAmount;
    private String paidAmount;
    private Long appointmentTime;
    private Long createdTime;
    private Long updatedTime;
    private Boolean hasDoorQr;
    private Boolean canApplyAfterSales;
    private Boolean hasAfterSalesEntry;
    private String afterSalesTip;
    private Boolean canConfirmCompletion;
    private String confirmCompletionTip;
}
