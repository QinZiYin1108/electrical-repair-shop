package com.example.backend.service.impl;

import com.example.backend.common.ErrorCode;
import com.example.backend.exception.BusinessException;
import com.example.backend.payment.PaymentGateway;
import com.example.backend.payment.VerifiedPaymentCallback;
import com.example.backend.payment.VerifiedRefundCallback;
import com.example.backend.service.PaymentCallbackService;
import com.example.backend.service.PaymentPostingService;
import com.example.backend.service.PaymentRefundsService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PaymentCallbackServiceImpl implements PaymentCallbackService {
    private final PaymentPostingService paymentPostingService;
    private final PaymentRefundsService paymentRefundsService;
    private final Map<Integer, PaymentGateway> gateways = new HashMap<>();

    public PaymentCallbackServiceImpl(
            PaymentPostingService paymentPostingService,
            PaymentRefundsService paymentRefundsService,
            List<PaymentGateway> gateways) {
        this.paymentPostingService = paymentPostingService;
        this.paymentRefundsService = paymentRefundsService;
        for (PaymentGateway gateway : gateways) {
            if (this.gateways.put(gateway.provider(), gateway) != null) {
                throw new IllegalStateException("支付渠道重复注册: " + gateway.provider());
            }
        }
    }

    @Override
    public void handle(int provider, Map<String, String> headers, String body) {
        PaymentGateway gateway = gateways.get(provider);
        if (gateway == null || !gateway.isAvailable()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "支付回调渠道不可用");
        }
        VerifiedPaymentCallback callback = gateway.verifyCallback(headers, body);
        if (callback == null) {
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR, "支付回调解析失败");
        }
        paymentPostingService.postVerifiedPayment(provider, callback);
    }

    @Override
    public void handleRefund(int provider, Map<String, String> headers, String body) {
        PaymentGateway gateway = gateways.get(provider);
        if (gateway == null || !gateway.isAvailable()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "支付回调渠道不可用");
        }
        VerifiedRefundCallback callback = gateway.verifyRefundCallback(headers, body);
        if (callback == null) {
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR, "退款回调解析失败");
        }
        paymentRefundsService.postVerifiedRefund(provider, callback);
    }
}
