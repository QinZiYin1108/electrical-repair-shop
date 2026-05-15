package com.example.backend.security.token;

import com.example.backend.security.model.LoginUserInfo;

/**
 * token 解析服务接口
 */
public interface TokenService {

    /**
     * 根据原始 token 解析出登录用户信息
     */
    LoginUserInfo parseToken(String token);
}
