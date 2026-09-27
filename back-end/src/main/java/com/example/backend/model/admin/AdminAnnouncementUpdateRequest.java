package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "管理员公告Update请求")
public class AdminAnnouncementUpdateRequest {

    @NotNull(message = "channel is required")
    @Schema(description = "channel")
    private Integer channel;

    @NotNull(message = "contentType is required")
    @Schema(description = "内容类型")
    private Integer contentType;

    @NotBlank(message = "title is required")
    @Size(max = 60, message = "title max length is 60")
    @Schema(description = "标题")
    private String title;

    @Size(max = 120, message = "subtitle max length is 120")
    @Schema(description = "subtitle")
    private String subtitle;

    @Size(max = 300, message = "content max length is 300")
    @Schema(description = "内容")
    private String content;

    @Size(max = 16, message = "emoji max length is 16")
    @Schema(description = "emoji")
    private String emoji;

    @Schema(description = "is是否启用")
    private Integer isActive;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "开始时间")
    private Long startTime;

    @Schema(description = "结束时间")
    private Long endTime;

    public Integer getChannel() {
        return channel;
    }

    public void setChannel(Integer channel) {
        this.channel = channel;
    }

    public Integer getContentType() {
        return contentType;
    }

    public void setContentType(Integer contentType) {
        this.contentType = contentType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
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

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }
}
