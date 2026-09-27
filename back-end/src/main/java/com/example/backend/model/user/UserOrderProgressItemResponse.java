package com.example.backend.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户订单ProgressItem响应")
@Data
public class UserOrderProgressItemResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "状态Text")
    private String statusText;

    @Schema(description = "description")
    private String description;

    @Schema(description = "操作人名称")
    private String operatorName;

    @Schema(description = "操作人类型")
    private Integer operatorType;

    @Schema(description = "创建时间")
    private Long createdTime;
}
