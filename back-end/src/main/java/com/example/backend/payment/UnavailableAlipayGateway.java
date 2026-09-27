package com.example.backend.payment;

import com.example.backend.common.ErrorCode;
import com.example.backend.exception.BusinessException;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 小程序端不开放支付宝；保留明确关闭的适配器，避免降级为模拟支付。 */
@Component
public class UnavailableAlipayGateway implements PaymentGateway {
    @Override
    public int provider() {
        return 2;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public Map<String, String> prepay(PaymentPrepayRequest request) {
        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前客户端不支持支付宝支付");
    }

    @Override
    public VerifiedPaymentCallback verifyCallback(Map<String, String> headers, String body) {
        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "支付宝回调验签组件尚未配置");
    }
}
