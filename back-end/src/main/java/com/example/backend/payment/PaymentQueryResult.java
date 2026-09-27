package com.example.backend.payment;

import java.math.BigDecimal;

public record PaymentQueryResult(
        String paymentNo,
        String providerTransactionNo,
        BigDecimal amount,
        String currency,
        ChannelPaymentStatus status,
        String verificationDigest) {}
