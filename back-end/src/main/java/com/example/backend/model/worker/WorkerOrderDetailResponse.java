package com.example.backend.model.worker;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "师傅订单详情响应")
@Data
public class WorkerOrderDetailResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "状态Text")
    private String statusText;

    @Schema(description = "nextActionText")
    private String nextActionText;

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

    @Schema(description = "支付状态")
    private Integer paymentStatus;

    @Schema(description = "支付状态Text")
    private String paymentStatusText;

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

    @Schema(description = "actionAvailable")
    private boolean actionAvailable;

    @Schema(description = "primaryAction类型")
    private String primaryActionType;

    @Schema(description = "primaryActionText")
    private String primaryActionText;

    @Schema(description = "actionHint")
    private String actionHint;

    private List<WorkerOrderFaultItem> faultList = new ArrayList<>();
    private List<WorkerOrderMediaItem> inspectionImages = new ArrayList<>();
    private List<WorkerOrderMediaItem> inspectionVideos = new ArrayList<>();
    private List<WorkerOrderProgressItem> progressList = new ArrayList<>();
}
