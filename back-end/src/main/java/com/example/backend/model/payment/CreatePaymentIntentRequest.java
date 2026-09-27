package com.example.backend.model.payment;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class CreatePaymentIntentRequest {
    /** 目前开放 3-钱包充值；维修和商城订单将在各自状态机接入后开放。 */
    @NotNull private Integer orderType;

    private String orderId;

    /** 1-微信，2-支付宝。 */
    @NotNull private Integer provider;

    @NotNull
    @DecimalMin(value = "0.01")
    @DecimalMax(value = "50000.00")
    private BigDecimal amount;
}
