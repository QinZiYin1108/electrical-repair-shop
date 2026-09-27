package com.example.backend.payment;

import java.util.Map;

/** 渠道适配器只能与支付渠道通信，不得直接修改余额或业务订单。 */
public interface PaymentGateway {
    int provider();

    boolean isAvailable();

    Map<String, String> prepay(PaymentPrepayRequest request);

    default PaymentQueryResult query(String paymentNo) {
        throw new UnsupportedOperationException("支付渠道未实现查单");
    }

    default void close(String paymentNo) {
        throw new UnsupportedOperationException("支付渠道未实现关单");
    }

    default RefundResult refund(RefundRequest request) {
        throw new UnsupportedOperationException("支付渠道未实现退款");
    }

    default RefundResult queryRefund(String refundNo) {
        throw new UnsupportedOperationException("支付渠道未实现退款查询");
    }

    VerifiedPaymentCallback verifyCallback(Map<String, String> headers, String body);

    default VerifiedRefundCallback verifyRefundCallback(Map<String, String> headers, String body) {
        throw new UnsupportedOperationException("支付渠道未实现退款回调验签");
    }
}
