package com.example.backend.payment;

import java.math.BigDecimal;

/** 只有渠道适配器完成验签、解密和商户身份校验后才能构造此对象。 */
public record VerifiedPaymentCallback(
        String paymentNo,
        String providerTransactionNo,
        BigDecimal amount,
        String currency,
        boolean paid,
        String callbackDigest) {}
