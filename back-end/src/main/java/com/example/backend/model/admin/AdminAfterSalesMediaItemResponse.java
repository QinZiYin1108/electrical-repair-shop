package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理员售后售后媒体Item响应")
@Data
public class AdminAfterSalesMediaItemResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "URL")
    private String url;

    @Schema(description = "thumbnailURL")
    private String thumbnailUrl;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "MIME类型类型")
    private String mimeType;

    @Schema(description = "时长")
    private Integer duration;
}
