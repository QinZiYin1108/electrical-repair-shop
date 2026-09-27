package com.example.backend.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户售后售后详情响应")
@Data
public class UserAfterSalesDetailResponse {

    @Schema(description = "排序ID")
    private String orderId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "订单状态")
    private Integer orderStatus;

    @Schema(description = "排序状态Text")
    private String orderStatusText;

    @Schema(description = "服务类型名称")
    private String serviceTypeName;

    @Schema(description = "服务分类名称")
    private String serviceCategoryName;

    @Schema(description = "服务ModeText")
    private String serviceModeText;

    @Schema(description = "师傅名称")
    private String technicianName;

    @Schema(description = "can申请售后销量")
    private Boolean canApplyAfterSales;

    @Schema(description = "售后销量Tip")
    private String afterSalesTip;

    @Schema(description = "application")
    private UserAfterSalesApplicationDetailResponse application;
}
