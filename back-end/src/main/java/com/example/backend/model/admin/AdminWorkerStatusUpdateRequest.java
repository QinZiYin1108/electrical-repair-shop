package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "管理员师傅状态Update请求")
public class AdminWorkerStatusUpdateRequest {

    @NotNull(message = "状态不能为空")
    @Schema(description = "账号状态")
    private Integer accountStatus;

    public Integer getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(Integer accountStatus) {
        this.accountStatus = accountStatus;
    }
}
