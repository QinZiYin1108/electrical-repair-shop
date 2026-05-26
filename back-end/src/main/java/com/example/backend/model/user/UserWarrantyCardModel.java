package com.example.backend.model.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

public final class UserWarrantyCardModel {

    private UserWarrantyCardModel() {
    }

    @Data
    public static class ListItemResponse {
        private String id;
        private String cardNo;
        private String productId;
        private String productName;
        private String productModel;
        private String purchaseDate;
        private String warrantyStartDate;
        private String warrantyEndDate;
        private Integer warrantyPeriod;
        private Integer warrantyType;
        private String warrantyTypeText;
        private Integer warrantyStatus;
        private String warrantyStatusText;
        private Integer repairCount;
        private String lastRepairDate;
        private Long remainingDays;
    }

    @Data
    public static class DetailResponse {
        private String id;
        private String cardNo;
        private String productId;
        private String productName;
        private String productModel;
        private String purchaseDate;
        private String warrantyStartDate;
        private String warrantyEndDate;
        private Integer warrantyPeriod;
        private Integer warrantyType;
        private String warrantyTypeText;
        private Integer warrantyStatus;
        private String warrantyStatusText;
        private Integer repairCount;
        private String lastRepairDate;
        private Long remainingDays;
        private Integer pendingUsageCount;
        private Boolean canApplyUsage;
        private List<UsageRecordResponse> usageRecords = new ArrayList<>();
    }

    @Data
    public static class ListResponse {
        private List<ListItemResponse> items = new ArrayList<>();
    }

    @Data
    public static class UsageRecordResponse {
        private String id;
        private String issueDescription;
        private String contactName;
        private String contactPhone;
        private Integer status;
        private String statusText;
        private String processRemark;
        private Long applyTime;
        private Long processTime;
    }

    @Data
    public static class ApplyUsageRequest {
        @NotBlank(message = "保修卡ID不能为空")
        private String warrantyCardId;

        @NotBlank(message = "故障描述不能为空")
        private String issueDescription;

        @NotBlank(message = "联系人不能为空")
        private String contactName;

        @NotBlank(message = "联系电话不能为空")
        private String contactPhone;
    }
}
