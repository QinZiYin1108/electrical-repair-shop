package com.example.backend.model.admin;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class AdminServiceTypeBatchCopyRequest {

    @NotEmpty(message = "sourceIds is required")
    private List<String> sourceIds;

    @NotNull(message = "targetType is required")
    private Integer targetType;

    public List<String> getSourceIds() {
        return sourceIds;
    }

    public void setSourceIds(List<String> sourceIds) {
        this.sourceIds = sourceIds;
    }

    public Integer getTargetType() {
        return targetType;
    }

    public void setTargetType(Integer targetType) {
        this.targetType = targetType;
    }
}
