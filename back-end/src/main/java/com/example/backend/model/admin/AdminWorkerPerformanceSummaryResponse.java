package com.example.backend.model.admin;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminWorkerPerformanceSummaryResponse {

    private Long totalWorkers;
    private Long activeWorkers;
    private Long totalOrders;
    private Long pendingOrders;
    private Long completedOrders;
    private BigDecimal grossIncome;
    private BigDecimal refundAmount;
    private BigDecimal netIncome;
    private BigDecimal averageRating;
}
