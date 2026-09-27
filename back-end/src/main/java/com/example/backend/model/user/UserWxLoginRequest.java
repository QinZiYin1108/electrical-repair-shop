package com.example.backend.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "用户Wx登录请求")
public class UserWxLoginRequest {

    @NotBlank(message = "code不能为空")
    @Schema(description = "编码")
    private String code;

    /** 当账号处于“注销申请中”时，是否确认继续登录（继续登录会撤销注销申请） */
    @Schema(description = "确认取消")
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
