package com.example.backend.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户商城分类响应")
@Data
public class UserMallCategoryResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "路径Text")
    private String pathText;

    @Schema(description = "图标URL")
    private String iconUrl;

    @Schema(description = "商品数量")
    private Integer productCount;
}
