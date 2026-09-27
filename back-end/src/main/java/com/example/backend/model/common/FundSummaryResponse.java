package com.example.backend.model.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "资金汇总响应")
@Data
public class FundSummaryResponse {

    @Schema(description = "余额")
    private String balance;

    @Schema(description = "是否冻结余额")
    private String frozenBalance;

    @Schema(description = "总数Income")
    private String totalIncome;

    @Schema(description = "总数Expense")
    private String totalExpense;
}
