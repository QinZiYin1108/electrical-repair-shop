package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Schema(description = "管理员故障现象Update请求")
public class AdminFaultPhenomenonUpdateRequest {

    @NotBlank(message = "serviceTypeId is required")
    @Schema(description = "服务类型ID")
    private String serviceTypeId;

    @NotBlank(message = "name is required")
    @Schema(description = "名称")
    private String name;

    @Schema(description = "description")
    private String description;

    @Schema(description = "estimated价格Min")
    private BigDecimal estimatedPriceMin;

    @Schema(description = "estimated价格Max")
    private BigDecimal estimatedPriceMax;

    @Schema(description = "is是否启用")
    private Integer isActive;

    @Schema(description = "排序")
    private Integer sortOrder;

    public String getServiceTypeId() {
        return serviceTypeId;
    }

    public void setServiceTypeId(String serviceTypeId) {
        this.serviceTypeId = serviceTypeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getEstimatedPriceMin() {
        return estimatedPriceMin;
    }

    public void setEstimatedPriceMin(BigDecimal estimatedPriceMin) {
        this.estimatedPriceMin = estimatedPriceMin;
    }

    public BigDecimal getEstimatedPriceMax() {
        return estimatedPriceMax;
    }

    public void setEstimatedPriceMax(BigDecimal estimatedPriceMax) {
        this.estimatedPriceMax = estimatedPriceMax;
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
