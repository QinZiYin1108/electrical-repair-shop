package com.example.backend.model.worker;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/** 维修人员提交提现申请。 */
@Data
public class TechnicianWithdrawalApplyRequest {
    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    /** 收款账户说明（脱敏展示用，例如「微信零钱」或尾号）。 */
    private String payoutAccount;

    /** 客户端幂等键，重复提交不会重复冻结。 */
    private String idempotencyKey;
}
