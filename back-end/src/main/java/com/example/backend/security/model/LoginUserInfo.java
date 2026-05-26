package com.example.backend.security.model;

/**
 * 当前登录账号的基础信息
 */
public class LoginUserInfo {

    /**
     * 账号主键ID
     */
    private String accountId;

    /**
     * 账号角色
     */
    private AccountRole role;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public AccountRole getRole() {
        return role;
    }

    public void setRole(AccountRole role) {
        this.role = role;
    }
}

