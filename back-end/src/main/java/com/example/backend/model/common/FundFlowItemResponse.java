package com.example.backend.model.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "资金流水Item响应")
@Data
public class FundFlowItemResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "流水类型")
    private Integer flowType;

    @Schema(description = "流水类型Text")
    private String flowTypeText;

    @Schema(description = "金额")
    private String amount;

    @Schema(description = "余额Before")
    private String balanceBefore;

    @Schema(description = "余额售后")
    private String balanceAfter;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "业务ID")
    private String businessId;

    @Schema(description = "description")
    private String description;

    @Schema(description = "创建时间")
    private Long createdTime;
}
