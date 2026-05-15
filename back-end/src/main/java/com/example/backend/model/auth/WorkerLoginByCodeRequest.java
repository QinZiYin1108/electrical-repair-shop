package com.example.backend.model.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class WorkerLoginByCodeRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String code;

    /**
     * 账号处于注销反悔期时，是否确认继续登录（继续登录将撤销注销）
     */
    private Boolean confirmCancel;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Boolean getConfirmCancel() {
        return confirmCancel;
    }

    public void setConfirmCancel(Boolean confirmCancel) {
        this.confirmCancel = confirmCancel;
    }
}
