package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理员预约订单媒体Item响应")
@Data
public class AdminReserveOrderMediaItemResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "URL")
    private String url;

    @Schema(description = "thumbnailURL")
    private String thumbnailUrl;

    @Schema(description = "MIME类型类型")
    private String mimeType;

    @Schema(description = "文件每页数量")
    private Long fileSize;

    @Schema(description = "宽度")
    private Integer width;

    @Schema(description = "高度")
    private Integer height;

    @Schema(description = "时长")
    private Integer duration;
}
