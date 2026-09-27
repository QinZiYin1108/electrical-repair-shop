package com.example.backend.model.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "协议内容响应")
@Data
public class ProtocolContentResponse {

    @Schema(description = "类型")
    private String type;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "文件名称")
    private String fileName;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "更新时间")
    private Long updatedTime;

    @Schema(description = "uploaded")
    private Boolean uploaded;
}
