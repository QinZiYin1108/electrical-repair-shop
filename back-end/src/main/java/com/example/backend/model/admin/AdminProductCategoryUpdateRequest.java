package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理员商品分类Update请求")
@Data
public class AdminProductCategoryUpdateRequest {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称长度不能超过100个字符")
    @Schema(description = "名称")
    private String name;

    @Schema(description = "父级ID")
    private String parentId;

    @Size(max = 5000, message = "分类描述长度不能超过5000个字符")
    @Schema(description = "description")
    private String description;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "is是否启用")
    private Integer isActive;
}
