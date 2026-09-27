package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理员仪表盘商品售后趋势Item响应")
@Data
public class AdminDashboardProductSalesTrendItemResponse {

    @Schema(description = "date标签")
    private String dateLabel;

    @Schema(description = "paid排序数量")
    private Long paidOrderCount;

    @Schema(description = "soldQuantity")
    private Long soldQuantity;

    @Schema(description = "销量金额")
    private BigDecimal salesAmount;
}
