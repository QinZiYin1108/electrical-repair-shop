package com.example.backend.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

public class UserMallOrderModel {

    @Data
    public static class AddCartRequest {
        @NotBlank(message = "商品ID不能为空")
        @Schema(description = "商品ID")
        private String productId;

        @Min(value = 1, message = "购买数量不能小于1")
        @Max(value = 99, message = "购买数量不能大于99")
        @Schema(description = "quantity")
        private Integer quantity;
    }

    @Data
    public static class UpdateCartQuantityRequest {
        @NotBlank(message = "购物车ID不能为空")
        @Schema(description = "购物车ID")
        private String cartId;

        @Min(value = 1, message = "购买数量不能小于1")
        @Max(value = 99, message = "购买数量不能大于99")
        @Schema(description = "quantity")
        private Integer quantity;
    }

    @Data
    public static class ToggleCartSelectedRequest {
        @NotBlank(message = "购物车ID不能为空")
        @Schema(description = "购物车ID")
        private String cartId;

        @Schema(description = "selected")
        private Boolean selected;
    }

    @Data
    public static class ToggleAllCartSelectedRequest {
        @Schema(description = "selected")
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
        @Schema(description = "地址ID")
        private String addressId;

        private List<String> cartIds = new ArrayList<>();

        private List<SubmitOrderItem> items = new ArrayList<>();

        @Min(value = 1, message = "支付方式不能小于1")
        @Max(value = 5, message = "支付方式不能大于5")
        @Schema(description = "支付方式")
        private Integer paymentMethod;

        @Schema(description = "用户优惠券ID")
        private String userCouponId;

        @Schema(description = "备注")
        private String remark;

        @Schema(description = "预约时间戳（毫秒），送货上门且需预约时必填")
        private Long appointmentTime;
    }

    @Data
    public static class SubmitOrderItem {
        @NotBlank(message = "商品ID不能为空")
        @Schema(description = "商品ID")
        private String productId;

        @Min(value = 1, message = "购买数量不能小于1")
        @Max(value = 99, message = "购买数量不能大于99")
        @Schema(description = "quantity")
        private Integer quantity;
    }

    @Data
    public static class AvailableCouponItem {
        @Schema(description = "用户优惠券ID")
        private String userCouponId;

        @Schema(description = "优惠券ID")
        private String couponId;

        @Schema(description = "名称")
        private String name;

        @Schema(description = "类型")
        private Integer type;

        @Schema(description = "类型Text")
        private String typeText;

        @Schema(description = "折扣Text")
        private String discountText;

        @Schema(description = "min金额")
        private BigDecimal minAmount;

        @Schema(description = "优惠金额")
        private BigDecimal discountAmount;

        @Schema(description = "available")
        private Boolean available;

        @Schema(description = "unavailable原因")
        private String unavailableReason;

        @Schema(description = "过期时间")
        private Long expireTime;

        @Schema(description = "applicableText")
        private String applicableText;
    }

    @Data
    public static class AvailableCouponListResponse {
        private List<AvailableCouponItem> coupons = new ArrayList<>();

        @Schema(description = "best优惠券ID")
        private String bestCouponId;

        @Schema(description = "best折扣金额")
        private BigDecimal bestDiscountAmount;
    }

    @Data
    public static class CartItem {
        @Schema(description = "购物车ID")
        private String cartId;

        @Schema(description = "商品ID")
        private String productId;

        @Schema(description = "名称")
        private String name;

        @Schema(description = "主图URL")
        private String mainImageUrl;

        @Schema(description = "分类路径")
        private String categoryPath;

        @Schema(description = "品牌")
        private String brand;

        @Schema(description = "型号")
        private String model;

        @Schema(description = "售价")
        private BigDecimal sellingPrice;

        @Schema(description = "原价")
        private BigDecimal originalPrice;

        @Schema(description = "quantity")
        private Integer quantity;

        @Schema(description = "selected")
        private Integer selected;

        @Schema(description = "库存数量")
        private Integer stockQuantity;

        @Schema(description = "line金额")
        private BigDecimal lineAmount;

        @Schema(description = "履约方式：1-自取，2-送货上门")
        private Integer fulfillmentType;
    }

    @Data
    public static class CartListResponse {
        private List<CartItem> items = new ArrayList<>();

        @Schema(description = "总数数量")
        private Integer totalCount;

        @Schema(description = "selected数量")
        private Integer selectedCount;

        @Schema(description = "selected金额")
        private BigDecimal selectedAmount;
    }

    @Data
    public static class SubmitOrderResponse {
        @Schema(description = "排序ID")
        private String orderId;

        @Schema(description = "订单号")
        private String orderNo;

        @Schema(description = "item数量")
        private Integer itemCount;

        @Schema(description = "实付金额")
        private BigDecimal actualAmount;

        @Schema(description = "优惠金额")
        private BigDecimal discountAmount;

        @Schema(description = "用户优惠券ID")
        private String userCouponId;

        @Schema(description = "优惠券名称")
        private String couponName;
    }
}
