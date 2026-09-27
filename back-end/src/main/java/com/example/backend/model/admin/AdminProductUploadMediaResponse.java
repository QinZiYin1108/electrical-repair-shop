package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理员商品上传媒体响应")
@Data
public class AdminProductUploadMediaResponse {

    @Schema(description = "URL")
    private String url;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "文件每页数量")
    private Long fileSize;

    @Schema(description = "MIME类型类型")
    private String mimeType;

    @Schema(description = "媒体类型")
    private String mediaType;

    @Schema(description = "宽度")
    private Integer width;

    @Schema(description = "高度")
    private Integer height;
}
