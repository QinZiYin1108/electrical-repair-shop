package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理员协议Item响应")
@Data
public class AdminProtocolItemResponse {

    @Schema(description = "类型")
    private String type;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "文件ID")
    private String fileId;

    @Schema(description = "文件名称")
    private String fileName;

    @Schema(description = "文件URL")
    private String fileUrl;

    @Schema(description = "更新时间")
    private Long updatedTime;

    @Schema(description = "uploaded")
    private Boolean uploaded;
}
