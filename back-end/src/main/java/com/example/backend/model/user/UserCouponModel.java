package com.example.backend.model.user;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class UserCouponModel {

    private UserCouponModel() {
    }

    @Data
    public static class ListItemResponse {
        private String id;
        private String couponId;
        private String name;
        private Integer type;
        private String typeText;
        private Integer discountType;
        private BigDecimal discountValue;
        private String discountText;
        private BigDecimal minAmount;
        private BigDecimal maxDiscount;
        private Integer applicableType;
        private String applicableTypeText;
        private Integer status;
        private String statusText;
        private String disabledReason;
        private Long startTime;
        private Long endTime;
        private Long receiveTime;
        private Long useTime;
        private Long expireTime;
        private String orderId;
    }

    @Data
    public static class DetailResponse {
        private String id;
        private String couponId;
        private String name;
        private Integer type;
        private String typeText;
        private Integer discountType;
        private BigDecimal discountValue;
        private String discountText;
        private BigDecimal minAmount;
        private BigDecimal maxDiscount;
        private Integer applicableType;
        private String applicableTypeText;
        private Integer status;
        private String statusText;
        private String disabledReason;
        private Long startTime;
        private Long endTime;
        private Long receiveTime;
        private Long useTime;
        private Long expireTime;
        private String orderId;
    }

    @Data
    public static class ListResponse {
        private List<ListItemResponse> items = new ArrayList<>();
    }
}
