package com.example.backend.model.user;

import lombok.Data;

@Data
public class UserAccountCancelApplyRequest {

    /**
     * 注销原因（可选）
     */
    private String reason;
}

