package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

public final class AdminWarrantyCardModel {

    private AdminWarrantyCardModel() {}

    @Data
    public static class ListItemResponse {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "card编号")
        private String cardNo;

        @Schema(description = "用户ID")
        private String userId;

        @Schema(description = "用户名称")
        private String userName;

        @Schema(description = "用户手机号")
        private String userPhone;

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

        @Schema(description = "创建时间")
        private Long createdTime;
    }

    @Data
    public static class DetailResponse {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "card编号")
        private String cardNo;

        @Schema(description = "用户ID")
        private String userId;

        @Schema(description = "用户名称")
        private String userName;

        @Schema(description = "用户手机号")
        private String userPhone;

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

        @Schema(description = "创建时间")
        private Long createdTime;

        @Schema(description = "更新时间")
        private Long updatedTime;
    }

    @Data
    public static class UsageRecordResponse {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "保修CardID")
        private String warrantyCardId;

        @Schema(description = "card编号")
        private String cardNo;

        @Schema(description = "用户ID")
        private String userId;

        @Schema(description = "用户名称")
        private String userName;

        @Schema(description = "用户手机号")
        private String userPhone;

        @Schema(description = "商品ID")
        private String productId;

        @Schema(description = "商品名称")
        private String productName;

        @Schema(description = "商品型号")
        private String productModel;

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
    public static class UsageRecordListResponse {
        private List<UsageRecordResponse> items = new ArrayList<>();
    }

    @Data
    public static class CreateRequest {
        @NotBlank(message = "用户ID不能为空")
        @Schema(description = "用户ID")
        private String userId;

        @NotBlank(message = "商品ID不能为空")
        @Schema(description = "商品ID")
        private String productId;

        @Schema(description = "购买Date")
        private String purchaseDate;

        @Schema(description = "保修开始Date")
        private String warrantyStartDate;

        @Schema(description = "保修期")
        private Integer warrantyPeriod;

        @NotNull(message = "保修类型不能为空")
        @Schema(description = "保修类型")
        private Integer warrantyType;
    }

    @Data
    public static class ProcessUsageRequest {
        @NotNull(message = "处理结果不能为空")
        @Schema(description = "状态")
        private Integer status;

        @Schema(description = "process备注")
        private String processRemark;
    }
}
