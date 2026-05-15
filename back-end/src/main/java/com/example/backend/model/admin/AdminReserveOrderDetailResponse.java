package com.example.backend.model.admin;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminReserveOrderDetailResponse {

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
    private String userEmail;
    private String technicianId;
    private String technicianName;
    private String technicianPhone;
    private String technicianEmail;
    private String contactName;
    private String contactPhone;
    private String serviceAddress;
    private String serviceAddressShort;
    private String applianceBrand;
    private String applianceModel;
    private Long purchaseDate;
    private Long appointmentTime;
    private Long createdTime;
    private Long updatedTime;
    private Long startTime;
    private Long endTime;
    private Long completionTime;
    private String faultSummary;
    private String totalAmount;
    private String paidAmount;
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
    private List<AdminReserveOrderFaultItemResponse> faultList = new ArrayList<>();
    private List<AdminReserveOrderMediaItemResponse> inspectionImages = new ArrayList<>();
    private List<AdminReserveOrderMediaItemResponse> inspectionVideos = new ArrayList<>();
    private List<AdminReserveOrderProgressItemResponse> progressList = new ArrayList<>();
}
