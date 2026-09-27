package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "管理员操作日志Query请求")
public class AdminOperationLogQueryRequest {

    @Schema(description = "module名称")
    private String moduleName;

    @Schema(description = "操作类型")
    private String operationType;

    @Schema(description = "操作人类型")
    private Integer operatorType;

    @Schema(description = "操作人名称")
    private String operatorName;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "开始时间")
    private Long startTime;

    @Schema(description = "结束时间")
    private Long endTime;

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public Integer getOperatorType() {
        return operatorType;
    }

    public void setOperatorType(Integer operatorType) {
        this.operatorType = operatorType;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
