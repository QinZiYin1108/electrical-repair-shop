package com.example.backend.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户订单媒体Item响应")
@Data
public class UserOrderMediaItemResponse {

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
