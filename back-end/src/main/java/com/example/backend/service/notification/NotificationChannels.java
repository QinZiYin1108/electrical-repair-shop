package com.example.backend.service.notification;

/** 通知发送渠道常量。渠道可扩展，新增渠道只需实现 {@link NotificationSender} 并注册为 Bean。 */
public final class NotificationChannels {
    /** 站内通知（写入 system_messages），默认始终可用。 */
    public static final String IN_APP = "IN_APP";

    /** 短信（需配置并启用）。 */
    public static final String SMS = "SMS";

    /** 微信订阅消息（预留）。 */
    public static final String WECHAT = "WECHAT";

    /** 邮件（预留）。 */
    public static final String EMAIL = "EMAIL";

    private NotificationChannels() {}
}
