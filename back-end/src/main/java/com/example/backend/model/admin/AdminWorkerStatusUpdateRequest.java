package com.example.backend.model.admin;

import jakarta.validation.constraints.NotNull;

public class AdminWorkerStatusUpdateRequest {

    @NotNull(message = "状态不能为空")
    private Integer accountStatus;

    public Integer getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(Integer accountStatus) {
        this.accountStatus = accountStatus;
    }
}

