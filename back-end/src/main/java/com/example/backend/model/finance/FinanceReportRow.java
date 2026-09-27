package com.example.backend.model.finance;

import java.math.BigDecimal;

/** 财务报表明细行（按订单快照汇总）。 */
public record FinanceReportRow(
        int orderCount,
        BigDecimal incomeService,
        BigDecimal incomeMaterial,
        BigDecimal incomeProduct,
        BigDecimal incomeDoorFee,
        BigDecimal incomeShipping,
        BigDecimal incomePlatformService,
        BigDecimal totalPaid,
        BigDecimal totalRefunded,
        BigDecimal netAmount,
        BigDecimal platformCommission,
        BigDecimal technicianIncome,
        BigDecimal taxAmount,
        BigDecimal discountAmount) {}
