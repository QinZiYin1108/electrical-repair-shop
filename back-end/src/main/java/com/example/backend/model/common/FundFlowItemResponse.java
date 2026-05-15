package com.example.backend.model.common;

import lombok.Data;

@Data
public class FundFlowItemResponse {

    private String id;

    private Integer flowType;

    private String flowTypeText;

    private String amount;

    private String balanceBefore;

    private String balanceAfter;

    private String businessType;

    private String businessId;

    private String description;

    private Long createdTime;
}

