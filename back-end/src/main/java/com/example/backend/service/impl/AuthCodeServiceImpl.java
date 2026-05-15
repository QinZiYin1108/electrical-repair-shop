package com.example.backend.service.impl;

import com.example.backend.common.ErrorCode;
import com.example.backend.exception.BusinessException;
import com.example.backend.service.AuthCodeService;
import com.example.backend.service.EmailTemplateService;
import com.example.backend.service.SystemConfigsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Random;

@Service
public class AuthCodeServiceImpl implements AuthCodeService {

    private static final String ADMIN_LOGIN_CODE_KEY = "admin:login:code:";
    private static final String ADMIN_RESET_CODE_KEY = "admin:reset:code:";
    private static final String ADMIN_CHANGE_EMAIL_CODE_KEY = "admin:change-email:code:";
    private static final String USER_BIND_EMAIL_CODE_KEY = "user:bind-email:code:";
    private static final String WORKER_LOGIN_CODE_KEY = "worker:login:code:";
    private static final String WORKER_RESET_PASSWORD_CODE_KEY = "worker:reset-password:code:";
    private static final String WORKER_CHANGE_EMAIL_CODE_KEY = "worker:change-email:code:";

    @Value("${spring.mail.username}")
    private String fromEmail;

    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;
    private final SystemConfigsService systemConfigsService;
    private final EmailTemplateService emailTemplateService;

    public AuthCodeServiceImpl(
        StringRedisTemplate redisTemplate,
        JavaMailSender mailSender,
        SystemConfigsService systemConfigsService,
        EmailTemplateService emailTemplateService
    ) {
        this.redisTemplate = redisTemplate;
        this.mailSender = mailSender;
        this.systemConfigsService = systemConfigsService;
        this.emailTemplateService = emailTemplateService;
    }

    @Override
    public void sendCode(String email, String type) {
        String code = generateCode();
        String keyPrefix = resolveKeyPrefix(type);
        String key = keyPrefix + email;
        int expireMinutes = getCodeExpireMinutes();
        redisTemplate.opsForValue().set(key, code, Duration.ofMinutes(expireMinutes));

        String subject = resolveSubject(type);
        String html = emailTemplateService.buildAuthCodeHtml(code, expireMinutes);
        sendHtmlMail(email, subject, html);
    }

    @Override
    public void verifyCode(String email, String type, String code) {
        String keyPrefix = resolveKeyPrefix(type);
        String key = keyPrefix + email;
        String cached = redisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(cached) || !cached.equals(code)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "邮箱或验证码有误");
        }
        redisTemplate.delete(key);
    }

    private String generateCode() {
        Random random = new Random();
        int val = random.nextInt(900000) + 100000;
        return String.valueOf(val);
    }

    private String resolveKeyPrefix(String type) {
        if ("ADMIN_LOGIN".equalsIgnoreCase(type)) {
            return ADMIN_LOGIN_CODE_KEY;
        }
        if ("ADMIN_RESET_PASSWORD".equalsIgnoreCase(type)) {
            return ADMIN_RESET_CODE_KEY;
        }
        if ("ADMIN_CHANGE_EMAIL".equalsIgnoreCase(type)) {
            return ADMIN_CHANGE_EMAIL_CODE_KEY;
        }
        if ("USER_BIND_EMAIL".equalsIgnoreCase(type)) {
            return USER_BIND_EMAIL_CODE_KEY;
        }
        if ("WORKER_LOGIN".equalsIgnoreCase(type)) {
            return WORKER_LOGIN_CODE_KEY;
        }
        if ("WORKER_RESET_PASSWORD".equalsIgnoreCase(type)) {
            return WORKER_RESET_PASSWORD_CODE_KEY;
        }
        if ("WORKER_CHANGE_EMAIL".equalsIgnoreCase(type)) {
            return WORKER_CHANGE_EMAIL_CODE_KEY;
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的验证码类型");
    }

    private String resolveSubject(String type) {
        if ("ADMIN_LOGIN".equalsIgnoreCase(type)) {
            return "管理员登录验证码";
        }
        if ("ADMIN_RESET_PASSWORD".equalsIgnoreCase(type)) {
            return "管理员重置密码验证码";
        }
        if ("ADMIN_CHANGE_EMAIL".equalsIgnoreCase(type)) {
            return "管理员修改邮箱验证码";
        }
        if ("USER_BIND_EMAIL".equalsIgnoreCase(type)) {
            return "邮箱绑定验证码";
        }
        if ("WORKER_LOGIN".equalsIgnoreCase(type)) {
            return "师傅登录验证码";
        }
        if ("WORKER_RESET_PASSWORD".equalsIgnoreCase(type)) {
            return "师傅重置密码验证码";
        }
        if ("WORKER_CHANGE_EMAIL".equalsIgnoreCase(type)) {
            return "师傅修改邮箱验证码";
        }
        return "验证码";
    }

    private void sendHtmlMail(String email, String subject, String htmlContent) {
        try {
            var mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "发送验证码邮件失败");
        }
    }

    private int getCodeExpireMinutes() {
        Integer value = systemConfigsService.getIntegerConfig("auth.code_expire_minutes", 5);
        return value == null || value <= 0 ? 5 : value;
    }
}