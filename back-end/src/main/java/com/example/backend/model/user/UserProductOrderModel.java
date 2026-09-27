package com.example.backend.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

public final class UserProductOrderModel {

    private UserProductOrderModel() {}

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

        @Schema(description = "是否首次商品图片")
        private String firstProductImage;

        @Schema(description = "商品Summary")
        private String productSummary;

        @Schema(description = "item数量")
        private Integer itemCount;

        @Schema(description = "总金额")
        private String totalAmount;

        @Schema(description = "实付金额")
        private String actualAmount;

        @Schema(description = "创建时间")
        private Long createdTime;

        @Schema(description = "支付时间")
        private Long paymentTime;

        @Schema(description = "发货时间")
        private Long deliveryTime;

        @Schema(description = "can确认Receipt")
        private Boolean canConfirmReceipt;

        @Schema(description = "can评价")
        private Boolean canReview;

        @Schema(description = "has评价")
        private Boolean hasReview;

        @Schema(description = "评价ID")
        private String reviewId;

        @Schema(description = "can申请售后销量")
        private Boolean canApplyAfterSales;

        @Schema(description = "has售后销量Entry")
        private Boolean hasAfterSalesEntry;

        @Schema(description = "售后销量Tip")
        private String afterSalesTip;

        @Schema(description = "售后销量Application")
        private AfterSalesSummary afterSalesApplication;
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

        @Schema(description = "商品金额")
        private String productAmount;

        @Schema(description = "运费")
        private String shippingFee;

        @Schema(description = "优惠金额")
        private String discountAmount;

        @Schema(description = "总金额")
        private String totalAmount;

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

        @Schema(description = "can确认Receipt")
        private Boolean canConfirmReceipt;

        @Schema(description = "can评价")
        private Boolean canReview;

        @Schema(description = "has评价")
        private Boolean hasReview;

        @Schema(description = "评价ID")
        private String reviewId;

        @Schema(description = "can申请售后销量")
        private Boolean canApplyAfterSales;

        @Schema(description = "has售后销量Entry")
        private Boolean hasAfterSalesEntry;

        @Schema(description = "售后销量Tip")
        private String afterSalesTip;

        @Schema(description = "售后销量Application")
        private AfterSalesSummary afterSalesApplication;

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
    public static class ConfirmReceiptRequest {
        @Schema(description = "排序ID")
        private String orderId;
    }

    @Data
    public static class AfterSalesSummary {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "application类型")
        private Integer applicationType;

        @Schema(description = "application类型Text")
        private String applicationTypeText;

        @Schema(description = "状态")
        private Integer status;

        @Schema(description = "状态Text")
        private String statusText;

        @Schema(description = "原因")
        private String reason;

        @Schema(description = "description")
        private String description;

        @Schema(description = "退款金额")
        private String refundAmount;

        @Schema(description = "管理员备注")
        private String adminRemark;

        @Schema(description = "创建时间")
        private Long createdTime;

        @Schema(description = "更新时间")
        private Long updatedTime;
    }

    @Data
    public static class AfterSalesTypeOption {
        @Schema(description = "值")
        private Integer value;

        @Schema(description = "标签")
        private String label;

        @Schema(description = "description")
        private String description;
    }

    @Data
    public static class AfterSalesApplicationDetailResponse {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "application类型")
        private Integer applicationType;

        @Schema(description = "application类型Text")
        private String applicationTypeText;

        @Schema(description = "状态")
        private Integer status;

        @Schema(description = "状态Text")
        private String statusText;

        @Schema(description = "原因")
        private String reason;

        @Schema(description = "description")
        private String description;

        @Schema(description = "退款金额")
        private String refundAmount;

        @Schema(description = "管理员备注")
        private String adminRemark;

        @Schema(description = "can取消")
        private Boolean canCancel;

        @Schema(description = "创建时间")
        private Long createdTime;

        @Schema(description = "更新时间")
        private Long updatedTime;

        @Schema(description = "processed时间")
        private Long processedTime;

        @Schema(description = "completed时间")
        private Long completedTime;

        private List<UserOrderMediaItemResponse> evidenceImages = new ArrayList<>();
        private List<UserOrderMediaItemResponse> evidenceVideos = new ArrayList<>();
    }

    @Data
    public static class AfterSalesDetailResponse {
        @Schema(description = "排序ID")
        private String orderId;

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

        @Schema(description = "商品Summary")
        private String productSummary;

        @Schema(description = "item数量")
        private Integer itemCount;

        @Schema(description = "收货人")
        private String deliveryName;

        @Schema(description = "收货电话")
        private String deliveryPhone;

        @Schema(description = "收货地址")
        private String deliveryAddress;

        @Schema(description = "can申请售后销量")
        private Boolean canApplyAfterSales;

        @Schema(description = "售后销量Tip")
        private String afterSalesTip;

        private List<AfterSalesTypeOption> applicationTypeOptions = new ArrayList<>();

        @Schema(description = "application")
        private AfterSalesApplicationDetailResponse application;
    }

    @Data
    public static class AfterSalesApplyRequest {
        @Schema(description = "排序ID")
        private String orderId;

        @Schema(description = "application类型")
        private Integer applicationType;

        @Schema(description = "原因")
        private String reason;

        @Schema(description = "description")
        private String description;

        private List<UserAfterSalesSubmitMediaItem> images = new ArrayList<>();

        @Schema(description = "视频")
        private UserAfterSalesSubmitMediaItem video;
    }

    @Data
    public static class AfterSalesCancelRequest {
        @Schema(description = "排序ID")
        private String orderId;
    }

    @Data
    public static class CancelOrderRequest {
        @Schema(description = "订单ID")
        private String orderId;

        @Schema(
                description =
                        "取消原因编码：schedule_change-临时有事, no_need-不想修了, price_issue-价格不合适, duplicate_order-重复下单, reschedule-改约其他时间, other-其他原因")
        private String reasonCode;

        @Schema(description = "取消原因标签")
        private String reasonLabel;

        @Schema(description = "用户备注")
        private String userRemark;
    }
}
