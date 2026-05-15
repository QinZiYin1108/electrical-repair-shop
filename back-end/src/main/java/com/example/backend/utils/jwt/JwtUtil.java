package com.example.backend.utils.jwt;

import com.example.backend.common.ErrorCode;
import com.example.backend.exception.BusinessException;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 * 负责生成和解析基于 HS256 的 JSON Web Token
 */
@Component
public class JwtUtil {

    private static final String HMAC_ALG = "HmacSHA256";

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 签名用密钥
     */
    private final String secret;

    /**
     * token 过期时间（秒）
     */
    private final long expireSeconds;

    public JwtUtil(
        @Value("${security.jwt.secret:change-me}") String secret,
        @Value("${security.jwt.expire-seconds:2592000}") long expireSeconds
    ) {
        this.secret = secret;
        this.expireSeconds = expireSeconds;
    }

    /**
     * 生成包含账号ID和角色的 JWT 字符串
     */
    public String generateToken(String accountId, AccountRole role) {
        long now = Instant.now().getEpochSecond();
        long exp = now + expireSeconds;

        Map<String, Object> header = new HashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new HashMap<>();
        payload.put("sub", accountId);
        payload.put("role", role.name());
        payload.put("iat", now);
        payload.put("exp", exp);

        try {
            String headerJson = objectMapper.writeValueAsString(header);
            String payloadJson = objectMapper.writeValueAsString(payload);

            String headerPart = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
            String payloadPart = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));

            String content = headerPart + "." + payloadPart;
            String signaturePart = sign(content);

            return content + "." + signaturePart;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成token失败");
        }
    }

    public LoginUserInfo parseToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "token格式错误");
            }
            String headerPart = parts[0];
            String payloadPart = parts[1];
            String signaturePart = parts[2];

            String content = headerPart + "." + payloadPart;
            String expectedSignature = sign(content);
            if (!expectedSignature.equals(signaturePart)) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "token无效");
            }

            String payloadJson = new String(base64UrlDecode(payloadPart), StandardCharsets.UTF_8);
            Map<String, Object> payload = objectMapper.readValue(
                payloadJson,
                new TypeReference<Map<String, Object>>() {}
            );

            Object expObj = payload.get("exp");
            long exp = toLong(expObj);
            long now = Instant.now().getEpochSecond();
            if (exp < now) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "token已过期");
            }

            String accountId = (String) payload.get("sub");
            String roleStr = (String) payload.get("role");
            if (accountId == null || roleStr == null) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "token缺少必要信息");
            }
            AccountRole role = AccountRole.valueOf(roleStr);

            LoginUserInfo userInfo = new LoginUserInfo();
            userInfo.setAccountId(accountId);
            userInfo.setRole(role);
            return userInfo;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "token解析失败");
        }
    }

    private String sign(String content) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALG);
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALG);
        mac.init(keySpec);
        byte[] sigBytes = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
        return base64UrlEncode(sigBytes);
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] base64UrlDecode(String str) {
        return Base64.getUrlDecoder().decode(str);
    }

    private long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
