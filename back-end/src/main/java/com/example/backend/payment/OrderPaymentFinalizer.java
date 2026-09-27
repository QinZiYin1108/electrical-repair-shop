package com.example.backend.payment;

import com.example.backend.entity.PaymentRecords;

/**
 * 订单外部支付成功后的业务推进。渠道适配器只负责验签与报文解析；只有支付回调或可信渠道查单在金额、商户身份校验通过后，才会通过此接口推进订单、托管与资金。
 *
 * <p>实现类必须幂等：同一支付单重复推进不得重复扣款、叠加或重复入账。
 */
public interface OrderPaymentFinalizer {

    /** 支持的支付订单类型（payment_records.order_type）。 */
    int orderType();

    /**
     * 渠道支付成功后推进订单业务。
     *
     * @param payment 已标记为支付成功、并写入渠道交易号的支付单（已加行锁）
     * @param now 当前时间戳
     */
    void onPaymentSuccess(PaymentRecords payment, long now);
}
