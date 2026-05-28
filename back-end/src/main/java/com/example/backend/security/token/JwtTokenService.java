package com.example.backend.security.token;

import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.utils.jwt.JwtUtil;
import org.springframework.stereotype.Service;

/**
 * 基于 JWT 的 token 服务实现
 */
@Service
public class JwtTokenService implements TokenService {

    private final JwtUtil jwtUtil;

    public JwtTokenService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public LoginUserInfo parseToken(String token) {
        return jwtUtil.parseToken(token);
    }

    /**
     * 生成包含账号ID和角色信息的 JWT 字符串
     */
    public String generateToken(String accountId, AccountRole role) {
        return jwtUtil.generateToken(accountId, role);
    }

    /**
     * 生成包含额外 claims 的 JWT（用于管理员的 adminRole、storeId）
     */
    public String generateToken(String accountId, AccountRole role, java.util.Map<String, Object> extraClaims) {
        return jwtUtil.generateToken(accountId, role, extraClaims);
    }
}
