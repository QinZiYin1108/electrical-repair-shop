package com.example.backend.service.notification;

/** 通知投递失败（可重试）。发送渠道抛出的运行时异常会被发送器记为失败并按退避策略重试。 */
public class NotificationDeliveryException extends RuntimeException {

    public NotificationDeliveryException(String message) {
        super(message);
    }

    public NotificationDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
