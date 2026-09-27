package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

public final class AdminProductOrderModel {

    private AdminProductOrderModel() {}

    @Data
    public static class ListItemResponse {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "订单号")
        private String orderNo;

        @Schema(description = "订单状态")
        private Integer orderStatus;

        @Schema(description = "排序状态Text")
        private String orderStatusText;

        @Schema(description = "支付状态")
        private Integer paymentStatus;

        @Schema(description = "支付状态Text")
        private String paymentStatusText;

        @Schema(description = "配送状态")
        private Integer deliveryStatus;

        @Schema(description = "配送状态Text")
        private String deliveryStatusText;

        @Schema(description = "用户ID")
        private String userId;

        @Schema(description = "用户名称")
        private String userName;

        @Schema(description = "用户手机号")
        private String userPhone;

        @Schema(description = "收货人")
        private String deliveryName;

        @Schema(description = "收货电话")
        private String deliveryPhone;

        @Schema(description = "收货地址")
        private String deliveryAddress;

        @Schema(description = "item数量")
        private Integer itemCount;

        @Schema(description = "商品Summary")
        private String productSummary;

        @Schema(description = "总金额")
        private String totalAmount;

        @Schema(description = "实付金额")
        private String actualAmount;

        @Schema(description = "支付方式")
        private Integer paymentMethod;

        @Schema(description = "支付MethodText")
        private String paymentMethodText;

        @Schema(description = "创建时间")
        private Long createdTime;

        @Schema(description = "支付时间")
        private Long paymentTime;

        @Schema(description = "发货时间")
        private Long deliveryTime;

        @Schema(description = "取消原因")
        private String cancelReason;
    }

    @Data
    public static class DetailResponse {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "订单号")
        private String orderNo;

        @Schema(description = "订单状态")
        private Integer orderStatus;

        @Schema(description = "排序状态Text")
        private String orderStatusText;

        @Schema(description = "支付状态")
        private Integer paymentStatus;

        @Schema(description = "支付状态Text")
        private String paymentStatusText;

        @Schema(description = "配送状态")
        private Integer deliveryStatus;

        @Schema(description = "配送状态Text")
        private String deliveryStatusText;

        @Schema(description = "用户ID")
        private String userId;

        @Schema(description = "用户名称")
        private String userName;

        @Schema(description = "用户手机号")
        private String userPhone;

        @Schema(description = "收货人")
        private String deliveryName;

        @Schema(description = "收货电话")
        private String deliveryPhone;

        @Schema(description = "收货地址")
        private String deliveryAddress;

        @Schema(description = "快递公司")
        private String deliveryCompany;

        @Schema(description = "快递单号")
        private String deliveryNo;

        @Schema(description = "item数量")
        private Integer itemCount;

        @Schema(description = "商品Summary")
        private String productSummary;

        @Schema(description = "总金额")
        private String totalAmount;

        @Schema(description = "商品金额")
        private String productAmount;

        @Schema(description = "运费")
        private String shippingFee;

        @Schema(description = "优惠金额")
        private String discountAmount;

        @Schema(description = "实付金额")
        private String actualAmount;

        @Schema(description = "支付方式")
        private Integer paymentMethod;

        @Schema(description = "支付MethodText")
        private String paymentMethodText;

        @Schema(description = "支付编号")
        private String paymentNo;

        @Schema(description = "thirdParty编号")
        private String thirdPartyNo;

        @Schema(description = "支付金额")
        private String paymentAmount;

        @Schema(description = "支付备注")
        private String paymentRemark;

        @Schema(description = "备注")
        private String remark;

        @Schema(description = "取消原因")
        private String cancelReason;

        @Schema(description = "取消时间")
        private Long cancelTime;

        @Schema(description = "退款原因")
        private String refundReason;

        @Schema(description = "退款金额")
        private String refundAmount;

        @Schema(description = "退款时间")
        private Long refundTime;

        @Schema(description = "创建时间")
        private Long createdTime;

        @Schema(description = "支付时间")
        private Long paymentTime;

        @Schema(description = "发货时间")
        private Long deliveryTime;

        @Schema(description = "收货时间")
        private Long receiveTime;

        @Schema(description = "完成时间")
        private Long completionTime;

        private List<OrderItemResponse> items = new ArrayList<>();
    }

    @Data
    public static class OrderItemResponse {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "商品ID")
        private String productId;

        @Schema(description = "商品名称")
        private String productName;

        @Schema(description = "商品图片")
        private String productImage;

        @Schema(description = "商品价格")
        private String productPrice;

        @Schema(description = "quantity")
        private Integer quantity;

        @Schema(description = "总数价格")
        private String totalPrice;
    }

    @Data
    public static class ShipRequest {
        @NotBlank(message = "快递公司不能为空")
        @Schema(description = "快递公司")
        private String deliveryCompany;

        @NotBlank(message = "快递单号不能为空")
        @Schema(description = "快递单号")
        private String deliveryNo;
    }
}
