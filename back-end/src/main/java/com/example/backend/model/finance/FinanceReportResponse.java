package com.example.backend.model.finance;

/** 财务报表响应：口径 + 时间窗口 + 汇总。 */
public record FinanceReportResponse(String timeBasis, long from, long to, FinanceReportRow row) {}
