package com.example.backend.model.admin;

import jakarta.validation.constraints.NotBlank;

public class AdminWorkerSkillDeleteRequest {

    @NotBlank(message = "serviceTypeId 不能为空")
    private String serviceTypeId;

    public String getServiceTypeId() {
        return serviceTypeId;
    }

    public void setServiceTypeId(String serviceTypeId) {
        this.serviceTypeId = serviceTypeId;
    }
}

