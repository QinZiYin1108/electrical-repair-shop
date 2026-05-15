package com.example.backend.model.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class WorkerLoginByPasswordRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getConfirmCancel() {
        return confirmCancel;
    }

    public void setConfirmCancel(Boolean confirmCancel) {
        this.confirmCancel = confirmCancel;
    }
}

