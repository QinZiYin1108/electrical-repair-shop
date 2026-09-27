package com.example.backend.model.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/** 管理端发起渠道退款的请求。 */
@Data
public class RefundApplyRequest {
    /** 退款金额，必须大于 0 且不超过支付单剩余可退金额。 */
    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    /** 退款原因。 */
    private String reason;

    /** 幂等键。相同幂等键重复提交不会重复退款；不传则按支付单与金额生成。 */
    private String idempotencyKey;
}
