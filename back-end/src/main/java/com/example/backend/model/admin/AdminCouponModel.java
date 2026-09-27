package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

public final class AdminCouponModel {

    private AdminCouponModel() {}

    @Data
    public static class ListItemResponse {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "名称")
        private String name;

        @Schema(description = "类型")
        private Integer type;

        @Schema(description = "类型Text")
        private String typeText;

        @Schema(description = "折扣类型")
        private Integer discountType;

        @Schema(description = "折扣类型Text")
        private String discountTypeText;

        @Schema(description = "折扣值")
        private BigDecimal discountValue;

        @Schema(description = "min金额")
        private BigDecimal minAmount;

        @Schema(description = "max折扣")
        private BigDecimal maxDiscount;

        @Schema(description = "总数数量")
        private Integer totalCount;

        @Schema(description = "收货数量")
        private Integer receiveCount;

        @Schema(description = "已使用数量")
        private Integer usedCount;

        @Schema(description = "remaining数量")
        private Integer remainingCount;

        @Schema(description = "per用户Limit")
        private Integer perUserLimit;

        @Schema(description = "applicable类型")
        private Integer applicableType;

        @Schema(description = "applicable类型Text")
        private String applicableTypeText;

        private List<String> applicableIds = new ArrayList<>();

        @Schema(description = "状态")
        private Integer status;

        @Schema(description = "状态Text")
        private String statusText;

        @Schema(description = "开始时间")
        private Long startTime;

        @Schema(description = "结束时间")
        private Long endTime;

        @Schema(description = "创建时间")
        private Long createdTime;

        @Schema(description = "更新时间")
        private Long updatedTime;
    }

    @Data
    public static class SaveRequest {
        @NotBlank(message = "优惠券名称不能为空")
        @Schema(description = "名称")
        private String name;

        @NotNull(message = "优惠类型不能为空")
        @Schema(description = "类型")
        private Integer type;

        @NotNull(message = "折扣方式不能为空")
        @Schema(description = "折扣类型")
        private Integer discountType;

        @NotNull(message = "优惠金额不能为空")
        @DecimalMin(value = "0.00", message = "优惠金额不能小于0")
        @Schema(description = "折扣值")
        private BigDecimal discountValue;

        @NotNull(message = "使用门槛不能为空")
        @DecimalMin(value = "0.00", message = "使用门槛不能小于0")
        @Schema(description = "min金额")
        private BigDecimal minAmount;

        @DecimalMin(value = "0.00", message = "最高减免不能小于0")
        @Schema(description = "max折扣")
        private BigDecimal maxDiscount;

        @NotNull(message = "发放总量不能为空")
        @Min(value = 1, message = "发放总量不能小于1")
        @Schema(description = "总数数量")
        private Integer totalCount;

        @Schema(description = "per用户Limit")
        private Integer perUserLimit;

        @NotNull(message = "适用范围不能为空")
        @Schema(description = "applicable类型")
        private Integer applicableType;

        private List<String> applicableIds = new ArrayList<>();

        @NotNull(message = "状态不能为空")
        @Schema(description = "状态")
        private Integer status;

        @NotNull(message = "开始时间不能为空")
        @Schema(description = "开始时间")
        private Long startTime;

        @NotNull(message = "结束时间不能为空")
        @Schema(description = "结束时间")
        private Long endTime;
    }

    @Data
    public static class StatusUpdateRequest {
        @NotNull(message = "状态不能为空")
        @Schema(description = "状态")
        private Integer status;
    }

    @Data
    public static class GrantRequest {
        private List<String> userIds = new ArrayList<>();
    }

    @Data
    public static class GrantResponse {
        @Schema(description = "grant数量")
        private Integer grantCount;

        @Schema(description = "skip数量")
        private Integer skipCount;

        private List<String> grantedUserIds = new ArrayList<>();
        private List<String> skippedUserIds = new ArrayList<>();
    }
}
