package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "管理员内容检测日志Item响应")
public class AdminContentCheckLogItemResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "账号ID")
    private String accountId;

    @Schema(description = "账号类型")
    private Integer accountType;

    @Schema(description = "内容类型")
    private Integer contentType;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "检测结果")
    private Integer checkResult;

    @Schema(description = "阿里云建议")
    private String hitRuleId;

    @Schema(description = "阿里云标签")
    private String hitKeyword;

    @Schema(description = "来源")
    private Integer source;

    @Schema(description = "创建时间")
    private Long createdTime;

    // ==================== getter / setter ====================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Integer getContentType() {
        return contentType;
    }

    public void setContentType(Integer contentType) {
        this.contentType = contentType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getCheckResult() {
        return checkResult;
    }

    public void setCheckResult(Integer checkResult) {
        this.checkResult = checkResult;
    }

    public String getHitRuleId() {
        return hitRuleId;
    }

    public void setHitRuleId(String hitRuleId) {
        this.hitRuleId = hitRuleId;
    }

    public String getHitKeyword() {
        return hitKeyword;
    }

    public void setHitKeyword(String hitKeyword) {
        this.hitKeyword = hitKeyword;
    }

    public Integer getSource() {
        return source;
    }

    public void setSource(Integer source) {
        this.source = source;
    }

    public Long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Long createdTime) {
        this.createdTime = createdTime;
    }
}
