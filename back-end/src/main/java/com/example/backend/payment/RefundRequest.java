package com.example.backend.payment;

import java.math.BigDecimal;

/** 退款申请传递给渠道适配器的数据。 */
public record RefundRequest(
        String paymentNo,
        String refundNo,
        String providerTransactionNo,
        BigDecimal totalAmount,
        BigDecimal refundAmount,
        String currency,
        String reason) {}
