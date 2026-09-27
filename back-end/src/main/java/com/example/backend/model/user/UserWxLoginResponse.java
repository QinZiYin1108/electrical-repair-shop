package com.example.backend.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "用户Wx登录响应")
public class UserWxLoginResponse {

    @Schema(description = "令牌")
    private String token;

    @Schema(description = "已绑定邮箱")
    private boolean emailBound;

    /** 账号是否处于注销反悔期，需用户确认后才继续登录（继续登录会撤销注销申请） */
    @Schema(description = "需要确认注销")
    private boolean needCancelConfirm;

    /** 注销申请时间戳 */
    @Schema(description = "取消申请时间")
    private Long cancelApplyTime;

    /** 计划注销时间戳（申请 + 7天） */
    @Schema(description = "取消截止时间")
    private Long cancelDeadlineTime;

    /** 是否在本次登录中撤销了注销申请 */
    @Schema(description = "已撤销注销")
    private boolean cancelRevoked;

    public void setToken(String token) {
        this.token = token;
    }

    public void setEmailBound(boolean emailBound) {
        this.emailBound = emailBound;
    }

    public void setNeedCancelConfirm(boolean needCancelConfirm) {
        this.needCancelConfirm = needCancelConfirm;
    }

    public void setCancelApplyTime(Long cancelApplyTime) {
        this.cancelApplyTime = cancelApplyTime;
    }

    public void setCancelDeadlineTime(Long cancelDeadlineTime) {
        this.cancelDeadlineTime = cancelDeadlineTime;
    }

    public void setCancelRevoked(boolean cancelRevoked) {
        this.cancelRevoked = cancelRevoked;
    }
}
