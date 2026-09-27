package com.example.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.entity.PaymentRefunds;
import com.example.backend.model.payment.PaymentRefundResponse;
import com.example.backend.model.payment.RefundApplyRequest;
import com.example.backend.payment.VerifiedRefundCallback;
import java.util.List;

/** 退款领域服务：发起渠道退款、处理渠道退款回调并完成幂等记账。 */
public interface PaymentRefundsService extends IService<PaymentRefunds> {
    /** 管理端发起一笔渠道退款。 */
    PaymentRefundResponse applyRefund(
            String paymentNo, RefundApplyRequest request, String operatorAccountId);

    /** 查询某支付单下的退款单。 */
    List<PaymentRefundResponse> listRefunds(String paymentNo);

    /** 处理渠道退款回调，仅在渠道确认退款成功后记账，且保证只记账一次。 */
    void postVerifiedRefund(int provider, VerifiedRefundCallback callback);

    /** 对订单的渠道（微信）支付部分发起原路退款，返回发起的退款笔数。 */
    int refundOrderChannelPayments(String orderId, String reason, String operatorAccountId);
}
