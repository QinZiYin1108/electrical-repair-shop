package com.example.backend.model.auth;

import lombok.Data;

/**
 * 师傅端登录响应
 */
@Data
public class WorkerLoginResponse {

    private String token;

    /**
     * 是否需要注销确认（账号处于反悔期，继续登录将撤销注销）
     */
    private Boolean needCancelConfirm;

    private Long cancelApplyTime;

    private Long cancelDeadlineTime;

    /**
     * 本次登录是否已撤销注销
     */
    private Boolean cancelRevoked;
}

