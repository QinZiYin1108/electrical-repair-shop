package com.example.backend.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

public final class UserCouponModel {

    private UserCouponModel() {}

    @Data
    public static class ListItemResponse {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "优惠券ID")
        private String couponId;

        @Schema(description = "名称")
        private String name;

        @Schema(description = "类型")
        private Integer type;

        @Schema(description = "类型Text")
        private String typeText;

        @Schema(description = "折扣类型")
        private Integer discountType;

        @Schema(description = "折扣值")
        private BigDecimal discountValue;

        @Schema(description = "折扣Text")
        private String discountText;

        @Schema(description = "min金额")
        private BigDecimal minAmount;

        @Schema(description = "max折扣")
        private BigDecimal maxDiscount;

        @Schema(description = "applicable类型")
        private Integer applicableType;

        @Schema(description = "applicable类型Text")
        private String applicableTypeText;

        @Schema(description = "状态")
        private Integer status;

        @Schema(description = "状态Text")
        private String statusText;

        @Schema(description = "disabled原因")
        private String disabledReason;

        @Schema(description = "开始时间")
        private Long startTime;

        @Schema(description = "结束时间")
        private Long endTime;

        @Schema(description = "收货时间")
        private Long receiveTime;

        @Schema(description = "use时间")
        private Long useTime;

        @Schema(description = "过期时间")
        private Long expireTime;

        @Schema(description = "排序ID")
        private String orderId;
    }

    @Data
    public static class DetailResponse {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "优惠券ID")
        private String couponId;

        @Schema(description = "名称")
        private String name;

        @Schema(description = "类型")
        private Integer type;

        @Schema(description = "类型Text")
        private String typeText;

        @Schema(description = "折扣类型")
        private Integer discountType;

        @Schema(description = "折扣值")
        private BigDecimal discountValue;

        @Schema(description = "折扣Text")
        private String discountText;

        @Schema(description = "min金额")
        private BigDecimal minAmount;

        @Schema(description = "max折扣")
        private BigDecimal maxDiscount;

        @Schema(description = "applicable类型")
        private Integer applicableType;

        @Schema(description = "applicable类型Text")
        private String applicableTypeText;

        @Schema(description = "状态")
        private Integer status;

        @Schema(description = "状态Text")
        private String statusText;

        @Schema(description = "disabled原因")
        private String disabledReason;

        @Schema(description = "开始时间")
        private Long startTime;

        @Schema(description = "结束时间")
        private Long endTime;

        @Schema(description = "收货时间")
        private Long receiveTime;

        @Schema(description = "use时间")
        private Long useTime;

        @Schema(description = "过期时间")
        private Long expireTime;

        @Schema(description = "排序ID")
        private String orderId;
    }

    @Data
    public static class ListResponse {
        private List<ListItemResponse> items = new ArrayList<>();
    }
}
