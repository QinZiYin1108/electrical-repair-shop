package com.example.backend.model.admin;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminDashboardProductSalesTrendItemResponse {

    private String dateLabel;
    private Long paidOrderCount;
    private Long soldQuantity;
    private BigDecimal salesAmount;
}
