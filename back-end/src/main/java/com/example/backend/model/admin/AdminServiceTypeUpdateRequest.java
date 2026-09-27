package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "管理员服务类型Update请求")
public class AdminServiceTypeUpdateRequest {

    @NotBlank(message = "name is required")
    @Schema(description = "名称")
    private String name;

    @NotNull(message = "type is required")
    @Schema(description = "类型")
    private Integer type;

    @NotBlank(message = "categoryId is required")
    @Schema(description = "分类ID")
    private String categoryId;

    @Schema(description = "description")
    private String description;

    @Schema(description = "base价格")
    private BigDecimal basePrice;

    @Schema(description = "is是否启用")
    private Integer isActive;

    @Schema(description = "排序")
    private Integer sortOrder;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
