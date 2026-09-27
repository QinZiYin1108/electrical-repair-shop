package com.example.backend.model.worker;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "师傅订单提交媒体列表项")
@Data
public class WorkerOrderSubmitMediaItem {

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
