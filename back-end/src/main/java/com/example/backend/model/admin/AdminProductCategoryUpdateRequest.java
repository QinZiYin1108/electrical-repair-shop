package com.example.backend.model.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminProductCategoryUpdateRequest {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称长度不能超过100个字符")
    private String name;

    private String parentId;

    @Size(max = 5000, message = "分类描述长度不能超过5000个字符")
    private String description;

    private Integer sortOrder;

    private Integer isActive;
}
