package com.example.backend.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

public final class UserWarrantyCardModel {

    private UserWarrantyCardModel() {}

    @Data
    public static class ListItemResponse {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "card编号")
        private String cardNo;

        @Schema(description = "商品ID")
        private String productId;

        @Schema(description = "商品名称")
        private String productName;

        @Schema(description = "商品型号")
        private String productModel;

        @Schema(description = "购买Date")
        private String purchaseDate;

        @Schema(description = "保修开始Date")
        private String warrantyStartDate;

        @Schema(description = "保修结束Date")
        private String warrantyEndDate;

        @Schema(description = "保修期")
        private Integer warrantyPeriod;

        @Schema(description = "保修类型")
        private Integer warrantyType;

        @Schema(description = "保修类型Text")
        private String warrantyTypeText;

        @Schema(description = "保修状态")
        private Integer warrantyStatus;

        @Schema(description = "保修状态Text")
        private String warrantyStatusText;

        @Schema(description = "维修数量")
        private Integer repairCount;

        @Schema(description = "last维修Date")
        private String lastRepairDate;

        @Schema(description = "remainingDays")
        private Long remainingDays;
    }

    @Data
    public static class DetailResponse {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "card编号")
        private String cardNo;

        @Schema(description = "商品ID")
        private String productId;

        @Schema(description = "商品名称")
        private String productName;

        @Schema(description = "商品型号")
        private String productModel;

        @Schema(description = "购买Date")
        private String purchaseDate;

        @Schema(description = "保修开始Date")
        private String warrantyStartDate;

        @Schema(description = "保修结束Date")
        private String warrantyEndDate;

        @Schema(description = "保修期")
        private Integer warrantyPeriod;

        @Schema(description = "保修类型")
        private Integer warrantyType;

        @Schema(description = "保修类型Text")
        private String warrantyTypeText;

        @Schema(description = "保修状态")
        private Integer warrantyStatus;

        @Schema(description = "保修状态Text")
        private String warrantyStatusText;

        @Schema(description = "维修数量")
        private Integer repairCount;

        @Schema(description = "last维修Date")
        private String lastRepairDate;

        @Schema(description = "remainingDays")
        private Long remainingDays;

        @Schema(description = "pendingUsage数量")
        private Integer pendingUsageCount;

        @Schema(description = "can申请Usage")
        private Boolean canApplyUsage;

        private List<UsageRecordResponse> usageRecords = new ArrayList<>();
    }

    @Data
    public static class ListResponse {
        private List<ListItemResponse> items = new ArrayList<>();
    }

    @Data
    public static class UsageRecordResponse {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "issueDescription")
        private String issueDescription;

        @Schema(description = "联系名称")
        private String contactName;

        @Schema(description = "联系电话")
        private String contactPhone;

        @Schema(description = "状态")
        private Integer status;

        @Schema(description = "状态Text")
        private String statusText;

        @Schema(description = "process备注")
        private String processRemark;

        @Schema(description = "申请时间")
        private Long applyTime;

        @Schema(description = "process时间")
        private Long processTime;
    }

    @Data
    public static class ApplyUsageRequest {
        @NotBlank(message = "保修卡ID不能为空")
        @Schema(description = "保修CardID")
        private String warrantyCardId;

        @NotBlank(message = "故障描述不能为空")
        @Schema(description = "issueDescription")
        private String issueDescription;

        @NotBlank(message = "联系人不能为空")
        @Schema(description = "联系名称")
        private String contactName;

        @NotBlank(message = "联系电话不能为空")
        @Schema(description = "联系电话")
        private String contactPhone;
    }
}
