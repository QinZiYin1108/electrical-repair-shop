package com.example.backend.model.admin;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminDashboardProductPaymentItemResponse {

    private Integer paymentMethod;
    private String label;
    private Long count;
    private BigDecimal amount;
}
