package com.example.backend.model.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "发票申请请求")
@Data
public class InvoiceApplyRequest {

    @Schema(description = "订单类型：1-维修订单，2-商品订单")
    private Integer orderType;

    @Schema(description = "订单ID")
    private String orderId;

    @Schema(description = "开票金额")
    private BigDecimal amount;

    @Schema(description = "抬头类型：1-个人，2-企业")
    private Integer titleType;

    @Schema(description = "发票抬头")
    private String title;

    @Schema(description = "纳税人识别号")
    private String taxpayerNo;

    @Schema(description = "开票内容")
    private String contentType;

    @Schema(description = "申请备注")
    private String applyRemark;
}
