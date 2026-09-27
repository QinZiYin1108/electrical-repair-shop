package com.example.backend.service.notification;

/** 通知 outbox 状态常量。 */
public final class NotificationOutboxStatus {
    /** 待发送。 */
    public static final int PENDING = 1;

    /** 发送中。 */
    public static final int SENDING = 2;

    /** 已发送。 */
    public static final int SENT = 3;

    /** 失败待重试。 */
    public static final int RETRY = 4;

    /** 死信（超过最大重试次数）。 */
    public static final int DEAD_LETTER = 5;

    private NotificationOutboxStatus() {}
}
