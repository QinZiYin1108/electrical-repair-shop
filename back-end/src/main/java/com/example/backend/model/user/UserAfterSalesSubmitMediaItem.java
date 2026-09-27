package com.example.backend.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户售后售后提交媒体列表项")
@Data
public class UserAfterSalesSubmitMediaItem {

    @Schema(description = "URL")
    private String url;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "文件每页数量")
    private Long fileSize;

    @Schema(description = "MIME类型类型")
    private String mimeType;

    @Schema(description = "宽度")
    private Integer width;

    @Schema(description = "高度")
    private Integer height;

    @Schema(description = "时长")
    private Integer duration;

    @Schema(description = "thumbnailURL")
    private String thumbnailUrl;
}
