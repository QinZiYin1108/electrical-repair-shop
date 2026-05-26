package com.example.backend.model.user;

import lombok.Getter;

@Getter
public class UserWxLoginResponse {

    private String token;
    private boolean emailBound;

    /**
     * 账号是否处于注销反悔期，需用户确认后才继续登录（继续登录会撤销注销申请）
     */
    private boolean needCancelConfirm;

    /**
     * 注销申请时间戳
     */
    private Long cancelApplyTime;

    /**
     * 计划注销时间戳（申请 + 7天）
     */
    private Long cancelDeadlineTime;

    /**
     * 是否在本次登录中撤销了注销申请
     */
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
