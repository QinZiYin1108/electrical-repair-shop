package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "管理员商品分类响应")
@Data
public class AdminProductCategoryResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "父级ID")
    private String parentId;

    @Schema(description = "父级名称")
    private String parentName;

    @Schema(description = "级别")
    private Integer level;

    @Schema(description = "description")
    private String description;

    @Schema(description = "图标URL")
    private String iconUrl;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "is是否启用")
    private Integer isActive;

    @Schema(description = "创建时间")
    private Long createdTime;

    @Schema(description = "更新时间")
    private Long updatedTime;

    private List<AdminProductCategoryResponse> children = new ArrayList<>();
}
