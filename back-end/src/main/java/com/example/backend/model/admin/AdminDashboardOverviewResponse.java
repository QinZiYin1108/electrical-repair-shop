package com.example.backend.model.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class AdminDashboardOverviewResponse {

    private Long totalUsers;
    private Long totalWorkers;
    private Long activeWorkers;
    private Long totalOrders;
    private Long todayOrders;
    private Long pendingOrders;
    private Long todayCompletedOrders;
    private Long pendingAfterSales;
    private BigDecimal totalGrossIncome;
    private BigDecimal totalRefundAmount;
    private BigDecimal totalNetIncome;
    private BigDecimal todayIncome;
    private List<AdminDashboardTrendItemResponse> recentTrend = new ArrayList<>();
    private List<AdminDashboardStatusItemResponse> orderStatusDistribution = new ArrayList<>();
}
