package com.example.backend.model.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "师傅登录By密码请求")
public class WorkerLoginByPasswordRequest {

    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号")
    private String phone;

    @NotBlank
    @Schema(description = "密码")
    private String password;

    /** 账号处于注销反悔期时，是否确认继续登录（继续登录将撤销注销） */
    @Schema(description = "确认取消")
    private Boolean confirmCancel;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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
