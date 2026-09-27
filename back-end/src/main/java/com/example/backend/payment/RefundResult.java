package com.example.backend.payment;

import java.math.BigDecimal;

/** 渠道退款申请或查询的结果。渠道返回的退款状态必须映射到平台内部状态后才能用于记账。 */
public record RefundResult(
        String refundNo,
        String providerRefundNo,
        BigDecimal refundAmount,
        String currency,
        ChannelRefundStatus status,
        String verificationDigest) {}
