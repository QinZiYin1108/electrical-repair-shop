package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "管理员用户状态Update请求")
public class AdminUserStatusUpdateRequest {

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态")
    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
