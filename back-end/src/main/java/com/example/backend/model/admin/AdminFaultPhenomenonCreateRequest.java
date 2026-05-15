package com.example.backend.model.admin;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public class AdminFaultPhenomenonCreateRequest {

    @NotBlank(message = "serviceTypeId is required")
    private String serviceTypeId;

    @NotBlank(message = "name is required")
    private String name;

    private String description;

    private BigDecimal estimatedPriceMin;

    private BigDecimal estimatedPriceMax;

    private Integer isActive;

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

