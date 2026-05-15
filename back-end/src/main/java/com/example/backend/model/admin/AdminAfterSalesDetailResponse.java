package com.example.backend.model.admin;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminAfterSalesDetailResponse {

    private String id;
    private String orderId;
    private String orderNo;
    private Integer status;
    private String statusText;
    private Integer applicationType;
    private String applicationTypeText;
    private String reason;
    private String description;
    private String refundAmount;
    private String adminRemark;
    private String contactPhone;
    private String contactAddress;
    private Long createdTime;
    private Long updatedTime;
    private Long processedTime;
    private Long completedTime;
    private String userId;
    private String userName;
    private String userPhone;
    private String technicianId;
    private String technicianName;
    private Integer orderStatus;
    private String orderStatusText;
    private String paymentStatusText;
    private String serviceTypeName;
    private String serviceCategoryName;
    private String serviceModeText;
    private String serviceAddress;
    private String totalAmount;
    private String paidAmount;
    private Boolean canProcess;
    private List<AdminAfterSalesMediaItemResponse> evidenceImages = new ArrayList<>();
    private List<AdminAfterSalesMediaItemResponse> evidenceVideos = new ArrayList<>();
}
