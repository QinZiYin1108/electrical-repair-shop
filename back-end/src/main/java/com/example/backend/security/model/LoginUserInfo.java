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
     * 账号角色（ADMIN / WORKER / USER）
     */
    private AccountRole role;

    /**
     * 管理员角色：1-超级管理员，2-门店管理员，3-客服（仅 role=ADMIN 时有值）
     */
    private Integer adminRole;

    /**
     * 归属门店ID（仅 adminRole=2 门店管理员时有值）
     */
    private String storeId;

    // ==================== 超级管理员判断 ====================

    public boolean isSuperAdmin() {
        return role == AccountRole.ADMIN && (adminRole == null || adminRole == 1);
    }

    public boolean isStoreAdmin() {
        return role == AccountRole.ADMIN && adminRole != null && adminRole == 2;
    }

    // ==================== getter / setter ====================

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

    public Integer getAdminRole() {
        return adminRole;
    }

    public void setAdminRole(Integer adminRole) {
        this.adminRole = adminRole;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }
}
