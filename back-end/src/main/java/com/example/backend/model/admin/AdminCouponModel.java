package com.example.backend.model.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class AdminCouponModel {

    private AdminCouponModel() {
    }

    @Data
    public static class ListItemResponse {
        private String id;
        private String name;
        private Integer type;
        private String typeText;
        private Integer discountType;
        private String discountTypeText;
        private BigDecimal discountValue;
        private BigDecimal minAmount;
        private BigDecimal maxDiscount;
        private Integer totalCount;
        private Integer receiveCount;
        private Integer usedCount;
        private Integer remainingCount;
        private Integer perUserLimit;
        private Integer applicableType;
        private String applicableTypeText;
        private List<String> applicableIds = new ArrayList<>();
        private Integer status;
        private String statusText;
        private Long startTime;
        private Long endTime;
        private Long createdTime;
        private Long updatedTime;
    }

    @Data
    public static class SaveRequest {
        @NotBlank(message = "优惠券名称不能为空")
        private String name;

        @NotNull(message = "优惠类型不能为空")
        private Integer type;

        @NotNull(message = "折扣方式不能为空")
        private Integer discountType;

        @NotNull(message = "优惠金额不能为空")
        @DecimalMin(value = "0.00", message = "优惠金额不能小于0")
        private BigDecimal discountValue;

        @NotNull(message = "使用门槛不能为空")
        @DecimalMin(value = "0.00", message = "使用门槛不能小于0")
        private BigDecimal minAmount;

        @DecimalMin(value = "0.00", message = "最高减免不能小于0")
        private BigDecimal maxDiscount;

        @NotNull(message = "发放总量不能为空")
        @Min(value = 1, message = "发放总量不能小于1")
        private Integer totalCount;

        private Integer perUserLimit;

        @NotNull(message = "适用范围不能为空")
        private Integer applicableType;

        private List<String> applicableIds = new ArrayList<>();

        @NotNull(message = "状态不能为空")
        private Integer status;

        @NotNull(message = "开始时间不能为空")
        private Long startTime;

        @NotNull(message = "结束时间不能为空")
        private Long endTime;
    }

    @Data
    public static class StatusUpdateRequest {
        @NotNull(message = "状态不能为空")
        private Integer status;
    }

    @Data
    public static class GrantRequest {
        private List<String> userIds = new ArrayList<>();
    }

    @Data
    public static class GrantResponse {
        private Integer grantCount;
        private Integer skipCount;
        private List<String> grantedUserIds = new ArrayList<>();
        private List<String> skippedUserIds = new ArrayList<>();
    }
}
