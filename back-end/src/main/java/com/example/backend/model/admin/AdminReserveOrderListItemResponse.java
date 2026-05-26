package com.example.backend.model.admin;

import lombok.Data;

@Data
public class AdminReserveOrderListItemResponse {

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
    private String userId;
    private String userName;
    private String userPhone;
    private String technicianId;
    private String technicianName;
    private String technicianPhone;
    private String contactName;
    private String contactPhone;
    private String serviceAddress;
    private String applianceBrand;
    private String applianceModel;
    private String faultSummary;
    private String totalAmount;
    private String paidAmount;
    private Long appointmentTime;
    private Long createdTime;
    private Long updatedTime;
}
