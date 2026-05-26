package com.example.backend.model.user;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

public final class UserProductOrderModel {

    private UserProductOrderModel() {
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
        private String firstProductImage;
        private String productSummary;
        private Integer itemCount;
        private String totalAmount;
        private String actualAmount;
        private Long createdTime;
        private Long paymentTime;
        private Long deliveryTime;
        private Boolean canConfirmReceipt;
        private Boolean canReview;
        private Boolean hasReview;
        private String reviewId;
        private Boolean canApplyAfterSales;
        private Boolean hasAfterSalesEntry;
        private String afterSalesTip;
        private AfterSalesSummary afterSalesApplication;
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
        private String deliveryName;
        private String deliveryPhone;
        private String deliveryAddress;
        private String deliveryCompany;
        private String deliveryNo;
        private Integer itemCount;
        private String productSummary;
        private String productAmount;
        private String shippingFee;
        private String discountAmount;
        private String totalAmount;
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
        private Boolean canConfirmReceipt;
        private Boolean canReview;
        private Boolean hasReview;
        private String reviewId;
        private Boolean canApplyAfterSales;
        private Boolean hasAfterSalesEntry;
        private String afterSalesTip;
        private AfterSalesSummary afterSalesApplication;
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
    public static class ConfirmReceiptRequest {
        private String orderId;
    }

    @Data
    public static class AfterSalesSummary {
        private String id;
        private Integer applicationType;
        private String applicationTypeText;
        private Integer status;
        private String statusText;
        private String reason;
        private String description;
        private String refundAmount;
        private String adminRemark;
        private Long createdTime;
        private Long updatedTime;
    }

    @Data
    public static class AfterSalesTypeOption {
        private Integer value;
        private String label;
        private String description;
    }

    @Data
    public static class AfterSalesApplicationDetailResponse {
        private String id;
        private Integer applicationType;
        private String applicationTypeText;
        private Integer status;
        private String statusText;
        private String reason;
        private String description;
        private String refundAmount;
        private String adminRemark;
        private Boolean canCancel;
        private Long createdTime;
        private Long updatedTime;
        private Long processedTime;
        private Long completedTime;
        private List<UserOrderMediaItemResponse> evidenceImages = new ArrayList<>();
        private List<UserOrderMediaItemResponse> evidenceVideos = new ArrayList<>();
    }

    @Data
    public static class AfterSalesDetailResponse {
        private String orderId;
        private String orderNo;
        private Integer orderStatus;
        private String orderStatusText;
        private Integer paymentStatus;
        private String paymentStatusText;
        private Integer deliveryStatus;
        private String deliveryStatusText;
        private String productSummary;
        private Integer itemCount;
        private String deliveryName;
        private String deliveryPhone;
        private String deliveryAddress;
        private Boolean canApplyAfterSales;
        private String afterSalesTip;
        private List<AfterSalesTypeOption> applicationTypeOptions = new ArrayList<>();
        private AfterSalesApplicationDetailResponse application;
    }

    @Data
    public static class AfterSalesApplyRequest {
        private String orderId;
        private Integer applicationType;
        private String reason;
        private String description;
        private List<UserAfterSalesSubmitMediaItem> images = new ArrayList<>();
        private UserAfterSalesSubmitMediaItem video;
    }

    @Data
    public static class AfterSalesCancelRequest {
        private String orderId;
    }
}
