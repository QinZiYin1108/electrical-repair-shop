package com.example.backend.domain.finance;

/** 财务报表统计口径：按支付时间 / 履约完成时间 / 退款时间。 */
public enum FinanceTimeBasis {
    PAY_TIME,
    PERFORM_TIME,
    REFUND_TIME;

    public static FinanceTimeBasis fromValue(String value) {
        if (value == null) {
            return PAY_TIME;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return PAY_TIME;
        }
    }
}
