package com.example.backend.model.admin;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminDashboardTrendItemResponse {

    private String dateLabel;
    private Long orderCount;
    private Long completedCount;
    private BigDecimal income;
}
