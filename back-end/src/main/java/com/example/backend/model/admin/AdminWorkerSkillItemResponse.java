package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "管理员师傅技能Item响应")
public class AdminWorkerSkillItemResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "服务类型ID")
    private String serviceTypeId;

    @Schema(description = "服务类型名称")
    private String serviceTypeName;

    @Schema(description = "服务Mode")
    private Integer serviceMode;

    @Schema(description = "服务ModeText")
    private String serviceModeText;

    @Schema(description = "分类ID")
    private String categoryId;

    @Schema(description = "分类路径")
    private String categoryPath;

    @Schema(description = "技能级别")
    private Integer skillLevel;

    @Schema(description = "技能级别Text")
    private String skillLevelText;

    @Schema(description = "is是否启用")
    private Integer isActive;

    @Schema(description = "更新时间")
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
