package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理员仪表盘商品PaymentItem响应")
@Data
public class AdminDashboardProductPaymentItemResponse {

    @Schema(description = "支付方式")
    private Integer paymentMethod;

    @Schema(description = "标签")
    private String label;

    @Schema(description = "数量")
    private Long count;

    @Schema(description = "金额")
    private BigDecimal amount;
}
