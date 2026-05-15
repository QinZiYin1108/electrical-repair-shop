package com.example.backend.model.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class UserMallOrderModel {

    @Data
    public static class AddCartRequest {
        @NotBlank(message = "商品ID不能为空")
        private String productId;

        @Min(value = 1, message = "购买数量不能小于1")
        @Max(value = 99, message = "购买数量不能大于99")
        private Integer quantity;
    }

    @Data
    public static class UpdateCartQuantityRequest {
        @NotBlank(message = "购物车ID不能为空")
        private String cartId;

        @Min(value = 1, message = "购买数量不能小于1")
        @Max(value = 99, message = "购买数量不能大于99")
        private Integer quantity;
    }

    @Data
    public static class ToggleCartSelectedRequest {
        @NotBlank(message = "购物车ID不能为空")
        private String cartId;

        private Boolean selected;
    }

    @Data
    public static class ToggleAllCartSelectedRequest {
        private Boolean selected;
    }

    @Data
    public static class RemoveCartItemsRequest {
        private List<String> cartIds = new ArrayList<>();
    }

    @Data
    public static class AvailableCouponRequest {
        private List<String> cartIds = new ArrayList<>();

        private List<SubmitOrderItem> items = new ArrayList<>();
    }

    @Data
    public static class SubmitOrderRequest {
        @NotBlank(message = "收货地址不能为空")
        private String addressId;

        private List<String> cartIds = new ArrayList<>();

        private List<SubmitOrderItem> items = new ArrayList<>();

        @Min(value = 1, message = "支付方式不能小于1")
        @Max(value = 5, message = "支付方式不能大于5")
        private Integer paymentMethod;

        private String userCouponId;

        private String remark;
    }

    @Data
    public static class SubmitOrderItem {
        @NotBlank(message = "商品ID不能为空")
        private String productId;

        @Min(value = 1, message = "购买数量不能小于1")
        @Max(value = 99, message = "购买数量不能大于99")
        private Integer quantity;
    }

    @Data
    public static class AvailableCouponItem {
        private String userCouponId;
        private String couponId;
        private String name;
        private Integer type;
        private String typeText;
        private String discountText;
        private BigDecimal minAmount;
        private BigDecimal discountAmount;
        private Boolean available;
        private String unavailableReason;
        private Long expireTime;
        private String applicableText;
    }

    @Data
    public static class AvailableCouponListResponse {
        private List<AvailableCouponItem> coupons = new ArrayList<>();
        private String bestCouponId;
        private BigDecimal bestDiscountAmount;
    }

    @Data
    public static class CartItem {
        private String cartId;
        private String productId;
        private String name;
        private String mainImageUrl;
        private String categoryPath;
        private String brand;
        private String model;
        private BigDecimal sellingPrice;
        private BigDecimal originalPrice;
        private Integer quantity;
        private Integer selected;
        private Integer stockQuantity;
        private BigDecimal lineAmount;
    }

    @Data
    public static class CartListResponse {
        private List<CartItem> items = new ArrayList<>();
        private Integer totalCount;
        private Integer selectedCount;
        private BigDecimal selectedAmount;
    }

    @Data
    public static class SubmitOrderResponse {
        private String orderId;
        private String orderNo;
        private Integer itemCount;
        private BigDecimal actualAmount;
        private BigDecimal discountAmount;
        private String userCouponId;
        private String couponName;
    }
}
