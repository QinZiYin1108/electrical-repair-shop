package com.example.backend.service.notification;

import com.example.backend.entity.NotificationOutbox;
import com.example.backend.service.SystemMessagesService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 站内通知发送器：把 outbox 消息落库为 system_messages。始终可用，无外部依赖。 */
@Service
public class InAppNotificationSender implements NotificationSender {

    private static final int MESSAGE_TYPE_SYSTEM = 1;
    private static final int PRIORITY_MEDIUM = 2;

    private final SystemMessagesService systemMessagesService;

    public InAppNotificationSender(SystemMessagesService systemMessagesService) {
        this.systemMessagesService = systemMessagesService;
    }

    @Override
    public String channel() {
        return NotificationChannels.IN_APP;
    }

    @Override
    public void send(NotificationOutbox message) {
        if (!StringUtils.hasText(message.getReceiverId())) {
            throw new NotificationDeliveryException("站内通知缺少接收人");
        }
        systemMessagesService.createSystemMessage(
                message.getReceiverId(),
                message.getReceiverType(),
                message.getTitle(),
                message.getContent(),
                MESSAGE_TYPE_SYSTEM,
                message.getBizType(),
                message.getBizId(),
                PRIORITY_MEDIUM);
    }
}
