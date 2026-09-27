package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "管理员预约订单详情响应")
@Data
public class AdminReserveOrderDetailResponse {

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

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名称")
    private String userName;

    @Schema(description = "用户手机号")
    private String userPhone;

    @Schema(description = "用户邮箱")
    private String userEmail;

    @Schema(description = "师傅ID")
    private String technicianId;

    @Schema(description = "师傅名称")
    private String technicianName;

    @Schema(description = "师傅手机号")
    private String technicianPhone;

    @Schema(description = "师傅邮箱")
    private String technicianEmail;

    @Schema(description = "联系名称")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "服务地址")
    private String serviceAddress;

    @Schema(description = "服务地址Short")
    private String serviceAddressShort;

    @Schema(description = "电器品牌")
    private String applianceBrand;

    @Schema(description = "电器型号")
    private String applianceModel;

    @Schema(description = "购买Date")
    private Long purchaseDate;

    @Schema(description = "预约时间")
    private Long appointmentTime;

    @Schema(description = "创建时间")
    private Long createdTime;

    @Schema(description = "更新时间")
    private Long updatedTime;

    @Schema(description = "开始时间")
    private Long startTime;

    @Schema(description = "结束时间")
    private Long endTime;

    @Schema(description = "完成时间")
    private Long completionTime;

    @Schema(description = "故障Summary")
    private String faultSummary;

    @Schema(description = "总金额")
    private String totalAmount;

    @Schema(description = "paid金额")
    private String paidAmount;

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

    private List<AdminReserveOrderFaultItemResponse> faultList = new ArrayList<>();
    private List<AdminReserveOrderMediaItemResponse> inspectionImages = new ArrayList<>();
    private List<AdminReserveOrderMediaItemResponse> inspectionVideos = new ArrayList<>();
    private List<AdminReserveOrderProgressItemResponse> progressList = new ArrayList<>();
}
