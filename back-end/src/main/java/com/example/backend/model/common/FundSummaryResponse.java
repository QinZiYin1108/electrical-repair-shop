package com.example.backend.model.common;

import lombok.Data;

@Data
public class FundSummaryResponse {

    private String balance;

    private String frozenBalance;

    private String totalIncome;

    private String totalExpense;
}

