package com.example.backend.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StartupConfigurationValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupConfigurationValidator.class);

    private final Environment environment;

    public StartupConfigurationValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> errors = new ArrayList<>();

        validateJwtSecret(errors);
        if (isProductionProfile()) {
            validateProductionRequiredProperties(errors);
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException("启动配置不完整：\n - " + String.join("\n - ", errors));
        }

        if (!isProductionProfile()) {
            log.info("当前不是 prod 环境，仅强校验 JWT 密钥；外部服务缺失会在调用时暴露。");
        }
    }

    private void validateJwtSecret(List<String> errors) {
        String secret = environment.getProperty("security.jwt.secret", "");
        if (isPlaceholder(secret) || secret.length() < 32) {
            errors.add("security.jwt.secret 必须配置为至少 32 位的非默认随机字符串");
        }
    }

    private void validateProductionRequiredProperties(List<String> errors) {
        require(errors, "spring.datasource.url", "数据库连接地址");
        require(errors, "spring.datasource.username", "数据库用户名");
        require(errors, "spring.datasource.password", "数据库密码");
        require(errors, "spring.data.redis.host", "Redis 地址");

        require(errors, "spring.mail.username", "邮件账号");
        require(errors, "spring.mail.password", "邮件授权码");

        require(errors, "aliyun.oss.endpoint", "阿里云 OSS endpoint");
        require(errors, "aliyun.oss.bucket-name", "阿里云 OSS bucket");
        require(errors, "aliyun.oss.access-key-id", "阿里云 OSS accessKeyId");
        require(errors, "aliyun.oss.access-key-secret", "阿里云 OSS accessKeySecret");

        require(errors, "aliyun.access-key-id", "阿里云内容安全 accessKeyId");
        require(errors, "aliyun.access-key-secret", "阿里云内容安全 accessKeySecret");

        require(errors, "wx.mini.appid", "微信小程序 appid");
        require(errors, "wx.mini.secret", "微信小程序 secret");

        if (environment.getProperty("payment.wechat.enabled", Boolean.class, false)) {
            require(errors, "payment.wechat.app-id", "微信支付 appid");
            require(errors, "payment.wechat.merchant-id", "微信支付商户号");
            require(errors, "payment.wechat.merchant-serial-number", "微信支付商户证书序列号");
            require(errors, "payment.wechat.private-key-path", "微信支付商户私钥路径");
            require(errors, "payment.wechat.api-v3-key", "微信支付 API v3 密钥");
            require(errors, "payment.wechat.notify-url", "微信支付回调地址");
        }

        require(errors, "tencent.sms.secret-id", "腾讯云 secretId");
        require(errors, "tencent.sms.secret-key", "腾讯云 secretKey");
        require(errors, "tencent.sms.sdk-app-id", "腾讯云短信 sdkAppId");
        require(errors, "tencent.sms.sign-name", "腾讯云短信签名");
        require(errors, "tencent.sms.login-template-id", "腾讯云短信登录模板");
        require(errors, "tencent.sms.reset-password-template-id", "腾讯云短信重置密码模板");
        require(errors, "tencent.sms.change-phone-template-id", "腾讯云短信换绑手机号模板");

        require(errors, "tencent.map.key", "腾讯地图 key");
        require(errors, "app.base-url", "应用访问地址");
        require(errors, "app.door-qr.base-url", "上门二维码扫码地址");
    }

    private void require(List<String> errors, String key, String label) {
        String value = environment.getProperty(key, "");
        if (isPlaceholder(value)) {
            errors.add(label + " 未配置：" + key);
        }
    }

    private boolean isProductionProfile() {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
    }

    private boolean isPlaceholder(String value) {
        if (value == null) {
            return true;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return true;
        }
        String lower = trimmed.toLowerCase();
        return lower.contains("change-me")
                || lower.startsWith("<your")
                || lower.startsWith("your_")
                || lower.equals("your-password")
                || lower.equals("your-secret");
    }
}
