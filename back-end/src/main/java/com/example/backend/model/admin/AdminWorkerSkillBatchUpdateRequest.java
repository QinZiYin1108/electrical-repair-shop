package com.example.backend.model.admin;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class AdminWorkerSkillBatchUpdateRequest {

    @NotEmpty(message = "serviceTypeIds 不能为空")
    private List<String> serviceTypeIds;

    public List<String> getServiceTypeIds() {
        return serviceTypeIds;
    }

    public void setServiceTypeIds(List<String> serviceTypeIds) {
        this.serviceTypeIds = serviceTypeIds;
    }
}

