package com.example.backend.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "用户售后售后Application详情响应")
@Data
public class UserAfterSalesApplicationDetailResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "application类型")
    private Integer applicationType;

    @Schema(description = "application类型Text")
    private String applicationTypeText;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "状态Text")
    private String statusText;

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

    @Schema(description = "can取消")
    private Boolean canCancel;

    @Schema(description = "创建时间")
    private Long createdTime;

    @Schema(description = "更新时间")
    private Long updatedTime;

    @Schema(description = "processed时间")
    private Long processedTime;

    @Schema(description = "completed时间")
    private Long completedTime;

    private List<UserOrderMediaItemResponse> evidenceImages = new ArrayList<>();
    private List<UserOrderMediaItemResponse> evidenceVideos = new ArrayList<>();
}
