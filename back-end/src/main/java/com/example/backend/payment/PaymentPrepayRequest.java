package com.example.backend.payment;

import java.math.BigDecimal;

/** 支付应用服务传递给渠道适配器的预下单数据。 */
public record PaymentPrepayRequest(
        String paymentNo,
        BigDecimal amount,
        String currency,
        String description,
        long expiresAt,
        String payerOpenId) {}
