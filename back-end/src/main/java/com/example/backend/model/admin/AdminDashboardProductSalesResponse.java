package com.example.backend.model.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class AdminDashboardProductSalesResponse {

    private Long totalOrderCount;
    private Long totalPaidOrderCount;
    private Long todayPaidOrderCount;
    private Long pendingDeliveryOrderCount;
    private Long refundedOrderCount;
    private Long totalSoldQuantity;
    private BigDecimal totalSalesAmount;
    private BigDecimal todaySalesAmount;
    private BigDecimal totalRefundAmount;
    private List<AdminDashboardProductSalesTrendItemResponse> recentTrend = new ArrayList<>();
    private List<AdminDashboardStatusItemResponse> orderStatusDistribution = new ArrayList<>();
    private List<AdminDashboardProductTopItemResponse> topProducts = new ArrayList<>();
    private List<AdminDashboardProductPaymentItemResponse> paymentMethodDistribution = new ArrayList<>();
}
