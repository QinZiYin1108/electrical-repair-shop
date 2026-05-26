package com.example.backend.model.admin;

public class AdminWorkerSkillItemResponse {

    private String id;
    private String serviceTypeId;
    private String serviceTypeName;
    private Integer serviceMode;
    private String serviceModeText;
    private String categoryId;
    private String categoryPath;
    private Integer skillLevel;
    private String skillLevelText;
    private Integer isActive;
    private Long updatedTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getServiceTypeId() {
        return serviceTypeId;
    }

    public void setServiceTypeId(String serviceTypeId) {
        this.serviceTypeId = serviceTypeId;
    }

    public String getServiceTypeName() {
        return serviceTypeName;
    }

    public void setServiceTypeName(String serviceTypeName) {
        this.serviceTypeName = serviceTypeName;
    }

    public Integer getServiceMode() {
        return serviceMode;
    }

    public void setServiceMode(Integer serviceMode) {
        this.serviceMode = serviceMode;
    }

    public String getServiceModeText() {
        return serviceModeText;
    }

    public void setServiceModeText(String serviceModeText) {
        this.serviceModeText = serviceModeText;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryPath() {
        return categoryPath;
    }

    public void setCategoryPath(String categoryPath) {
        this.categoryPath = categoryPath;
    }

    public Integer getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(Integer skillLevel) {
        this.skillLevel = skillLevel;
    }

    public String getSkillLevelText() {
        return skillLevelText;
    }

    public void setSkillLevelText(String skillLevelText) {
        this.skillLevelText = skillLevelText;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public Long getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(Long updatedTime) {
        this.updatedTime = updatedTime;
    }
}

