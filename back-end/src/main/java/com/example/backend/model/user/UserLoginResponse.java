package com.example.backend.model.user;

import lombok.Data;

@Data
public class UserLoginResponse {

    private String token;
    private boolean emailBound;
    private boolean wechatBound;
    private boolean passwordSet;
    private boolean newAccountCreated;
    private boolean needCancelConfirm;
    private Long cancelApplyTime;
    private Long cancelDeadlineTime;
    private boolean cancelRevoked;
}
