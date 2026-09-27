package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理员仪表盘商品TopItem响应")
@Data
public class AdminDashboardProductTopItemResponse {

    @Schema(description = "商品ID")
    private String productId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "商品图片")
    private String productImage;

    @Schema(description = "quantity")
    private Long quantity;

    @Schema(description = "销量金额")
    private BigDecimal salesAmount;

    @Schema(description = "订单数量")
    private Long orderCount;
}
