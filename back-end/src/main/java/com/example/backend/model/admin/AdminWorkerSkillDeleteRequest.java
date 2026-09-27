package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "管理员师傅技能Delete请求")
public class AdminWorkerSkillDeleteRequest {

    @NotBlank(message = "serviceTypeId 不能为空")
    @Schema(description = "服务类型ID")
    private String serviceTypeId;

    public String getServiceTypeId() {
        return serviceTypeId;
    }

    public void setServiceTypeId(String serviceTypeId) {
        this.serviceTypeId = serviceTypeId;
    }
}
