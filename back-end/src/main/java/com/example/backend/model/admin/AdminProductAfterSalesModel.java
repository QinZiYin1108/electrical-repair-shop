package com.example.backend.model.admin;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

public final class AdminProductAfterSalesModel {

    private AdminProductAfterSalesModel() {
    }

    @Data
    public static class ListItemResponse {
        private String id;
        private String orderId;
        private String orderNo;
        private Integer status;
        private String statusText;
        private Integer applicationType;
        private String applicationTypeText;
        private String reason;
        private String userId;
        private String userName;
        private String userPhone;
        private String productSummary;
        private Integer itemCount;
        private Long createdTime;
        private Long updatedTime;
    }

    @Data
    public static class DetailResponse {
        private String id;
        private String orderId;
        private String orderNo;
        private Integer status;
        private String statusText;
        private Integer applicationType;
        private String applicationTypeText;
        private String reason;
        private String description;
        private String refundAmount;
        private String adminRemark;
        private String contactPhone;
        private String contactAddress;
        private Long createdTime;
        private Long updatedTime;
        private Long processedTime;
        private Long completedTime;
        private String userId;
        private String userName;
        private String userPhone;
        private Integer orderStatus;
        private String orderStatusText;
        private String paymentStatusText;
        private String deliveryStatusText;
        private String productSummary;
        private Integer itemCount;
        private String deliveryName;
        private String deliveryPhone;
        private String deliveryAddress;
        private String deliveryCompany;
        private String deliveryNo;
        private String totalAmount;
        private String paidAmount;
        private Boolean canApprove;
        private Boolean canReject;
        private Boolean canRefund;
        private List<AdminProductOrderModel.OrderItemResponse> items = new ArrayList<>();
        private List<AdminAfterSalesMediaItemResponse> evidenceImages = new ArrayList<>();
        private List<AdminAfterSalesMediaItemResponse> evidenceVideos = new ArrayList<>();
    }
}
