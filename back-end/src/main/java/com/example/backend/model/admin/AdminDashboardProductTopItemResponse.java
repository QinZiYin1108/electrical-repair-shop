package com.example.backend.model.admin;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminDashboardProductTopItemResponse {

    private String productId;
    private String productName;
    private String productImage;
    private Long quantity;
    private BigDecimal salesAmount;
    private Long orderCount;
}
