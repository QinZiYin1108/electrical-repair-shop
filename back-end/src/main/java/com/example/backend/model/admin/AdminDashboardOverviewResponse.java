package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "管理员仪表盘概览响应")
@Data
public class AdminDashboardOverviewResponse {

    @Schema(description = "总数Users")
    private Long totalUsers;

    @Schema(description = "总数Workers")
    private Long totalWorkers;

    @Schema(description = "是否启用Workers")
    private Long activeWorkers;

    @Schema(description = "总数Orders")
    private Long totalOrders;

    @Schema(description = "todayOrders")
    private Long todayOrders;

    @Schema(description = "pendingOrders")
    private Long pendingOrders;

    @Schema(description = "todayCompletedOrders")
    private Long todayCompletedOrders;

    @Schema(description = "pending售后销量")
    private Long pendingAfterSales;

    @Schema(description = "总数GrossIncome")
    private BigDecimal totalGrossIncome;

    @Schema(description = "总数退款金额")
    private BigDecimal totalRefundAmount;

    @Schema(description = "总数NetIncome")
    private BigDecimal totalNetIncome;

    @Schema(description = "todayIncome")
    private BigDecimal todayIncome;

    private List<AdminDashboardTrendItemResponse> recentTrend = new ArrayList<>();
    private List<AdminDashboardStatusItemResponse> orderStatusDistribution = new ArrayList<>();
}
