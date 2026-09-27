package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "管理员仪表盘商品售后响应")
@Data
public class AdminDashboardProductSalesResponse {

    @Schema(description = "总数排序数量")
    private Long totalOrderCount;

    @Schema(description = "总数Paid排序数量")
    private Long totalPaidOrderCount;

    @Schema(description = "todayPaid排序数量")
    private Long todayPaidOrderCount;

    @Schema(description = "pending配送排序数量")
    private Long pendingDeliveryOrderCount;

    @Schema(description = "refunded排序数量")
    private Long refundedOrderCount;

    @Schema(description = "总数SoldQuantity")
    private Long totalSoldQuantity;

    @Schema(description = "总数销量金额")
    private BigDecimal totalSalesAmount;

    @Schema(description = "today销量金额")
    private BigDecimal todaySalesAmount;

    @Schema(description = "总数退款金额")
    private BigDecimal totalRefundAmount;

    private List<AdminDashboardProductSalesTrendItemResponse> recentTrend = new ArrayList<>();
    private List<AdminDashboardStatusItemResponse> orderStatusDistribution = new ArrayList<>();
    private List<AdminDashboardProductTopItemResponse> topProducts = new ArrayList<>();
    private List<AdminDashboardProductPaymentItemResponse> paymentMethodDistribution =
            new ArrayList<>();
}
