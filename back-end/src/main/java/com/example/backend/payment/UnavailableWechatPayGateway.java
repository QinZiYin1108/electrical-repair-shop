package com.example.backend.payment;

import com.example.backend.common.ErrorCode;
import com.example.backend.exception.BusinessException;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 默认关闭。接入微信支付 SDK、商户证书和回调验签后再替换此实现。 */
@Component
@ConditionalOnProperty(
        name = "payment.wechat.enabled",
        havingValue = "false",
        matchIfMissing = true)
public class UnavailableWechatPayGateway implements PaymentGateway {
    @Override
    public int provider() {
        return 1;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public Map<String, String> prepay(PaymentPrepayRequest request) {
        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "微信支付渠道尚未配置，暂不可用");
    }

    @Override
    public VerifiedPaymentCallback verifyCallback(Map<String, String> headers, String body) {
        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "微信支付回调验签组件尚未配置");
    }
}
