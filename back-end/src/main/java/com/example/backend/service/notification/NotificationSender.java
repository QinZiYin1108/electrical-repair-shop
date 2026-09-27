package com.example.backend.service.notification;

import com.example.backend.entity.NotificationOutbox;

/** 通知发送器：按渠道投递 outbox 消息。投递失败请抛出 {@link NotificationDeliveryException}。 */
public interface NotificationSender {

    /** 本发送器负责的渠道，取值见 {@link NotificationChannels}。 */
    String channel();

    /** 投递一条消息；失败时抛出异常。 */
    void send(NotificationOutbox message);
}
