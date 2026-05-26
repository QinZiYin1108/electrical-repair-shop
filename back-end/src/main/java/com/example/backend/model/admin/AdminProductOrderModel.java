package com.example.backend.model.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

public final class AdminProductOrderModel {

    private AdminProductOrderModel() {
    }

    @Data
    public static class ListItemResponse {
        private String id;
        private String orderNo;
        private Integer orderStatus;
        private String orderStatusText;
        private Integer paymentStatus;
        private String paymentStatusText;
        private Integer deliveryStatus;
        private String deliveryStatusText;
        private String userId;
        private String userName;
        private String userPhone;
        private String deliveryName;
        private String deliveryPhone;
        private String deliveryAddress;
        private Integer itemCount;
        private String productSummary;
        private String totalAmount;
        private String actualAmount;
        private Integer paymentMethod;
        private String paymentMethodText;
        private Long createdTime;
        private Long paymentTime;
        private Long deliveryTime;
    }

    @Data
    public static class DetailResponse {
        private String id;
        private String orderNo;
        private Integer orderStatus;
        private String orderStatusText;
        private Integer paymentStatus;
        private String paymentStatusText;
        private Integer deliveryStatus;
        private String deliveryStatusText;
        private String userId;
        private String userName;
        private String userPhone;
        private String deliveryName;
        private String deliveryPhone;
        private String deliveryAddress;
        private String deliveryCompany;
        private String deliveryNo;
        private Integer itemCount;
        private String productSummary;
        private String totalAmount;
        private String productAmount;
        private String shippingFee;
        private String discountAmount;
        private String actualAmount;
        private Integer paymentMethod;
        private String paymentMethodText;
        private String paymentNo;
        private String thirdPartyNo;
        private String paymentAmount;
        private String paymentRemark;
        private String remark;
        private String cancelReason;
        private Long cancelTime;
        private String refundReason;
        private String refundAmount;
        private Long refundTime;
        private Long createdTime;
        private Long paymentTime;
        private Long deliveryTime;
        private Long receiveTime;
        private Long completionTime;
        private List<OrderItemResponse> items = new ArrayList<>();
    }

    @Data
    public static class OrderItemResponse {
        private String id;
        private String productId;
        private String productName;
        private String productImage;
        private String productPrice;
        private Integer quantity;
        private String totalPrice;
    }

    @Data
    public static class ShipRequest {
        @NotBlank(message = "快递公司不能为空")
        private String deliveryCompany;

        @NotBlank(message = "快递单号不能为空")
        private String deliveryNo;
    }
}
