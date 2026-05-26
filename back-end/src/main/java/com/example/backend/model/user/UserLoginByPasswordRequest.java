package com.example.backend.model.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UserLoginByPasswordRequest {

    @NotBlank(message = "\u90ae\u7bb1\u4e0d\u80fd\u4e3a\u7a7a")
    @Email(message = "\u90ae\u7bb1\u683c\u5f0f\u4e0d\u6b63\u786e")
    private String email;

    @NotBlank(message = "\u5bc6\u7801\u4e0d\u80fd\u4e3a\u7a7a")
    private String password;

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
