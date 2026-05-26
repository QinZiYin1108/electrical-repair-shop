package com.example.backend.model.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class AdminFaultPhenomenonBatchCopyRequest {

    @NotEmpty(message = "sourceIds is required")
    private List<String> sourceIds;

    @NotBlank(message = "targetServiceTypeId is required")
    private String targetServiceTypeId;

    public List<String> getSourceIds() {
        return sourceIds;
    }

    public void setSourceIds(List<String> sourceIds) {
        this.sourceIds = sourceIds;
    }

    public String getTargetServiceTypeId() {
        return targetServiceTypeId;
    }

    public void setTargetServiceTypeId(String targetServiceTypeId) {
        this.targetServiceTypeId = targetServiceTypeId;
    }
}
