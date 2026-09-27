package com.example.backend.model.payment;

import lombok.Data;

/** 退款单对外响应。 */
@Data
public class PaymentRefundResponse {
    private String refundNo;
    private String paymentNo;
    private String accountId;
    private String amount;
    private String currency;
    private Integer status;
    private String statusText;
    private Integer provider;
    private String providerRefundNo;
    private String reason;
    private Long initiatedTime;
    private Long completedTime;
}
