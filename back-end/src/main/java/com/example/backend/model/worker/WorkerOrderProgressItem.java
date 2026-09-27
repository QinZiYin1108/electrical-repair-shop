package com.example.backend.model.worker;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "师傅订单Progress列表项")
@Data
public class WorkerOrderProgressItem {

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
