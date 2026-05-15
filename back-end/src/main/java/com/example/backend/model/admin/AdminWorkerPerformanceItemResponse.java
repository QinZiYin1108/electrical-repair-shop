package com.example.backend.model.admin;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminWorkerPerformanceItemResponse {

    private String id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private String avatarUrl;
    private Integer accountStatus;
    private Integer workStatus;
    private BigDecimal rating;
    private Integer reviewCount;
    private Long totalOrders;
    private Long waitingOrders;
    private Long ongoingOrders;
    private Long waitingPayOrders;
    private Long pendingOrders;
    private Long completedOrders;
    private Long canceledOrders;
    private Long refundedOrders;
    private BigDecimal completionRate;
    private BigDecimal grossIncome;
    private BigDecimal refundAmount;
    private BigDecimal netIncome;
    private BigDecimal averageOrderAmount;
    private BigDecimal serviceHours;
    private Long latestCompletedTime;
    private Long createdTime;
}
