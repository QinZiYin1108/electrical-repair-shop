package com.example.backend.model.worker;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WorkerOrderDetailResponse {

    private String id;
    private String orderNo;
    private Integer status;
    private String statusText;
    private String nextActionText;
    private String serviceTypeId;
    private String serviceTypeName;
    private String serviceCategoryId;
    private String serviceCategoryName;
    private String serviceCategoryPath;
    private Integer serviceMode;
    private String serviceModeText;
    private String userId;
    private String userName;
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

    private Integer paymentStatus;
    private String paymentStatusText;
    private String doorFee;
    private String distanceFee;
    private String serviceFee;
    private String materialFee;
    private String overtimeFee;
    private String remark;
    private String inspectionDiagnosis;
    private String repairPlan;
    private Long inspectionTime;

    private boolean actionAvailable;
    private String primaryActionType;
    private String primaryActionText;
    private String actionHint;

    private List<WorkerOrderFaultItem> faultList = new ArrayList<>();
    private List<WorkerOrderMediaItem> inspectionImages = new ArrayList<>();
    private List<WorkerOrderMediaItem> inspectionVideos = new ArrayList<>();
    private List<WorkerOrderProgressItem> progressList = new ArrayList<>();
}
