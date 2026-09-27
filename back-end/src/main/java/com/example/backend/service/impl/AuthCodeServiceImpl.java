package com.example.backend.service.impl;

import com.example.backend.common.ErrorCode;
import com.example.backend.exception.BusinessException;
import com.example.backend.service.AuthCodeService;
import com.example.backend.service.SmsService;
import com.example.backend.service.SystemConfigsService;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthCodeServiceImpl implements AuthCodeService {

    private static final Logger log = LoggerFactory.getLogger(AuthCodeServiceImpl.class);

    private static final String ADMIN_LOGIN_CODE_KEY = "admin:login:code:";
    private static final String ADMIN_RESET_CODE_KEY = "admin:reset:code:";
    private static final String ADMIN_CHANGE_PHONE_CODE_KEY = "admin:change-phone:code:";
    private static final String USER_BIND_PHONE_CODE_KEY = "user:bind-phone:code:";
    private static final String WORKER_LOGIN_CODE_KEY = "worker:login:code:";
    private static final String WORKER_RESET_PASSWORD_CODE_KEY = "worker:reset-password:code:";
    private static final String WORKER_CHANGE_PHONE_CODE_KEY = "worker:change-phone:code:";
    private static final String SMS_RATE_LIMIT_KEY = "sms:ratelimit:";
    private static final String SMS_DAILY_LIMIT_KEY = "sms:daily:";
    private static final String VERIFY_ATTEMPTS_KEY = "auth:code:attempts:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final SmsService smsService;
    private final SystemConfigsService systemConfigsService;

    public AuthCodeServiceImpl(
            StringRedisTemplate redisTemplate,
            SmsService smsService,
            SystemConfigsService systemConfigsService) {
        this.redisTemplate = redisTemplate;
        this.smsService = smsService;
        this.systemConfigsService = systemConfigsService;
    }

    @Override
    public void sendCode(String phone, String type) {
        // 频率限制：同一手机号 N 秒内只能发一次
        String rateLimitKey = SMS_RATE_LIMIT_KEY + phone;
        int intervalSeconds = getRateLimitIntervalSeconds();
        Boolean locked =
                redisTemplate
                        .opsForValue()
                        .setIfAbsent(rateLimitKey, "1", Duration.ofSeconds(intervalSeconds));
        if (locked == null || !locked) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR, "发送过于频繁，请 " + intervalSeconds + " 秒后再试");
        }

        enforceDailyLimit(phone);

        String code = generateCode();
        String keyPrefix = resolveKeyPrefix(type);
        String key = keyPrefix + phone;
        int expireMinutes = getCodeExpireMinutes();
        redisTemplate.opsForValue().set(key, code, Duration.ofMinutes(expireMinutes));
        redisTemplate.delete(attemptKey(phone, type));

        log.info("准备发送短信验证码: phone={}, type={}", phone, type);
        smsService.sendCode(phone, code, expireMinutes, type);
        log.info("短信验证码发送成功: phone={}, type={}", phone, type);
    }

    @Override
    public void verifyCode(String phone, String type, String code) {
        String keyPrefix = resolveKeyPrefix(type);
        String key = keyPrefix + phone;
        String cached = redisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(cached) || !cached.equals(code)) {
            registerFailedAttempt(phone, type, key);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "手机号或验证码有误");
        }
        redisTemplate.delete(List.of(key, attemptKey(phone, type)));
    }

    private String generateCode() {
        int val = SECURE_RANDOM.nextInt(900000) + 100000;
        return String.valueOf(val);
    }

    private void registerFailedAttempt(String phone, String type, String codeKey) {
        String attemptsKey = attemptKey(phone, type);
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(attemptsKey, Duration.ofMinutes(getCodeExpireMinutes()));
        }
        if (attempts != null && attempts >= getMaxVerifyAttempts()) {
            redisTemplate.delete(List.of(codeKey, attemptsKey));
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "验证码错误次数过多，请重新获取");
        }
    }

    private void enforceDailyLimit(String phone) {
        String key = SMS_DAILY_LIMIT_KEY + phone;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofDays(1));
        }
        if (count != null && count > getDailySendLimit()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "今日验证码发送次数已达上限");
        }
    }

    private String attemptKey(String phone, String type) {
        return VERIFY_ATTEMPTS_KEY + type.toUpperCase() + ":" + phone;
    }

    private String resolveKeyPrefix(String type) {
        if ("ADMIN_LOGIN".equalsIgnoreCase(type)) {
            return ADMIN_LOGIN_CODE_KEY;
        }
        if ("ADMIN_RESET_PASSWORD".equalsIgnoreCase(type)) {
            return ADMIN_RESET_CODE_KEY;
        }
        if ("ADMIN_CHANGE_PHONE".equalsIgnoreCase(type)) {
            return ADMIN_CHANGE_PHONE_CODE_KEY;
        }
        if ("USER_BIND_PHONE".equalsIgnoreCase(type)) {
            return USER_BIND_PHONE_CODE_KEY;
        }
        if ("WORKER_LOGIN".equalsIgnoreCase(type)) {
            return WORKER_LOGIN_CODE_KEY;
        }
        if ("WORKER_RESET_PASSWORD".equalsIgnoreCase(type)) {
            return WORKER_RESET_PASSWORD_CODE_KEY;
        }
        if ("WORKER_CHANGE_PHONE".equalsIgnoreCase(type)) {
            return WORKER_CHANGE_PHONE_CODE_KEY;
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的验证码类型");
    }

    private int getCodeExpireMinutes() {
        Integer value = systemConfigsService.getIntegerConfig("auth.code_expire_minutes", 5);
        return value == null || value <= 0 ? 5 : value;
    }

    private int getRateLimitIntervalSeconds() {
        Integer value = systemConfigsService.getIntegerConfig("auth.sms_rate_limit_seconds", 60);
        return value == null || value <= 0 ? 60 : value;
    }

    private int getMaxVerifyAttempts() {
        Integer value = systemConfigsService.getIntegerConfig("auth.code_max_verify_attempts", 5);
        return value == null || value <= 0 ? 5 : value;
    }

    private int getDailySendLimit() {
        Integer value = systemConfigsService.getIntegerConfig("auth.sms_daily_limit", 10);
        return value == null || value <= 0 ? 10 : value;
    }
}
