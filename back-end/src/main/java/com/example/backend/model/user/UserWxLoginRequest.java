package com.example.backend.model.user;

import jakarta.validation.constraints.NotBlank;

public class UserWxLoginRequest {

    @NotBlank(message = "code不能为空")
    private String code;

    /**
     * 当账号处于“注销申请中”时，是否确认继续登录（继续登录会撤销注销申请）
     */
    private Boolean confirmCancel;

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
