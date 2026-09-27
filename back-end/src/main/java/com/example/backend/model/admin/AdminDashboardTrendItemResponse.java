package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理员仪表盘趋势Item响应")
@Data
public class AdminDashboardTrendItemResponse {

    @Schema(description = "date标签")
    private String dateLabel;

    @Schema(description = "订单数量")
    private Long orderCount;

    @Schema(description = "completed数量")
    private Long completedCount;

    @Schema(description = "income")
    private BigDecimal income;
}
