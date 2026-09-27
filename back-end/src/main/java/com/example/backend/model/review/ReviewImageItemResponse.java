package com.example.backend.model.review;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "评价图片Item响应")
@Data
public class ReviewImageItemResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "URL")
    private String url;

    @Schema(description = "thumbnailURL")
    private String thumbnailUrl;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "宽度")
    private Integer width;

    @Schema(description = "高度")
    private Integer height;
}
