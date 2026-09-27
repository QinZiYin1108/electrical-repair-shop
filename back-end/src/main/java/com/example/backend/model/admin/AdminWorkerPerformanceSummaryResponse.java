package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理员师傅绩效汇总响应")
@Data
public class AdminWorkerPerformanceSummaryResponse {

    @Schema(description = "总数Workers")
    private Long totalWorkers;

    @Schema(description = "是否启用Workers")
    private Long activeWorkers;

    @Schema(description = "总数Orders")
    private Long totalOrders;

    @Schema(description = "pendingOrders")
    private Long pendingOrders;

    @Schema(description = "completedOrders")
    private Long completedOrders;

    @Schema(description = "grossIncome")
    private BigDecimal grossIncome;

    @Schema(description = "退款金额")
    private BigDecimal refundAmount;

    @Schema(description = "netIncome")
    private BigDecimal netIncome;

    @Schema(description = "average评分")
    private BigDecimal averageRating;
}
