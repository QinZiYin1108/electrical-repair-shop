package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;

/** 存疑复审列表项 — 汇总文字和图片/视频的待复审记录 */
@Schema(description = "管理员Watch评价Item响应")
public class AdminWatchReviewItemResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "复审类型")
    private String reviewType; // "text" / "image" / "file"

    @Schema(description = "账号ID")
    private String accountId;

    @Schema(description = "账号类型")
    private Integer accountType;

    @Schema(description = "内容")
    private String content; // 文字内容或文件URL

    @Schema(description = "建议")
    private String suggestion; // 阿里云原始建议（watch）

    @Schema(description = "标签")
    private String label; // 阿里云标签

    @Schema(description = "标签描述")
    private String labelDesc; // 阿里云标签描述

    @Schema(description = "图片URL")
    private String imageUrl; // 图片URL（图片类型时有值）

    @Schema(description = "创建时间")
    private Long createdTime;

    // ==================== getter / setter ====================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReviewType() {
        return reviewType;
    }

    public void setReviewType(String reviewType) {
        this.reviewType = reviewType;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public Integer getAccountType() {
        return accountType;
    }

    public void setAccountType(Integer accountType) {
        this.accountType = accountType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabelDesc() {
        return labelDesc;
    }

    public void setLabelDesc(String labelDesc) {
        this.labelDesc = labelDesc;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Long createdTime) {
        this.createdTime = createdTime;
    }
}
