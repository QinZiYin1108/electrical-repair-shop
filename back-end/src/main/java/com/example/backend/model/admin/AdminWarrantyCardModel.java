package com.example.backend.model.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

public final class AdminWarrantyCardModel {

    private AdminWarrantyCardModel() {
    }

    @Data
    public static class ListItemResponse {
        private String id;
        private String cardNo;
        private String userId;
        private String userName;
        private String userPhone;
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
        private Long createdTime;
    }

    @Data
    public static class DetailResponse {
        private String id;
        private String cardNo;
        private String userId;
        private String userName;
        private String userPhone;
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
        private Long createdTime;
        private Long updatedTime;
    }

    @Data
    public static class UsageRecordResponse {
        private String id;
        private String warrantyCardId;
        private String cardNo;
        private String userId;
        private String userName;
        private String userPhone;
        private String productId;
        private String productName;
        private String productModel;
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
    public static class UsageRecordListResponse {
        private List<UsageRecordResponse> items = new ArrayList<>();
    }

    @Data
    public static class CreateRequest {
        @NotBlank(message = "用户ID不能为空")
        private String userId;

        @NotBlank(message = "商品ID不能为空")
        private String productId;

        private String purchaseDate;

        private String warrantyStartDate;

        private Integer warrantyPeriod;

        @NotNull(message = "保修类型不能为空")
        private Integer warrantyType;
    }

    @Data
    public static class ProcessUsageRequest {
        @NotNull(message = "处理结果不能为空")
        private Integer status;

        private String processRemark;
    }
}
