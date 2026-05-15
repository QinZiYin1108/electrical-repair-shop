package com.example.backend.security.context;

import com.example.backend.security.model.LoginUserInfo;

/**
 * 基于 ThreadLocal 的登录用户上下文
 */
public class AuthUserContext {

    private static final ThreadLocal<LoginUserInfo> HOLDER = new ThreadLocal<>();

    /**
     * 设置当前请求关联的登录用户信息
     */
    public static void set(LoginUserInfo userInfo) {
        HOLDER.set(userInfo);
    }

    /**
     * 获取当前请求关联的登录用户信息
     */
    public static LoginUserInfo get() {
        return HOLDER.get();
    }

    /**
     * 清理当前线程保存的登录用户信息
     */
    public static void clear() {
        HOLDER.remove();
    }
}

