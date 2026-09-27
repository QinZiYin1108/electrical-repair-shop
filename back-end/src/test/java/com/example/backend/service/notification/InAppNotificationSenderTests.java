package com.example.backend.service.notification;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.backend.entity.NotificationOutbox;
import com.example.backend.service.SystemMessagesService;
import org.junit.jupiter.api.Test;

class InAppNotificationSenderTests {

    @Test
    void sendPersistsSystemMessage() {
        SystemMessagesService service = mock(SystemMessagesService.class);
        InAppNotificationSender sender = new InAppNotificationSender(service);

        NotificationOutbox message = new NotificationOutbox();
        message.setReceiverId("TA1");
        message.setReceiverType(2);
        message.setTitle("标题");
        message.setContent("内容");
        message.setBizType("TECHNICIAN_WITHDRAWAL");
        message.setBizId("TW1");

        sender.send(message);

        verify(service)
                .createSystemMessage(
                        eq("TA1"),
                        eq(2),
                        eq("标题"),
                        eq("内容"),
                        eq(1),
                        eq("TECHNICIAN_WITHDRAWAL"),
                        eq("TW1"),
                        eq(2));
    }

    @Test
    void sendRejectsMissingReceiver() {
        SystemMessagesService service = mock(SystemMessagesService.class);
        InAppNotificationSender sender = new InAppNotificationSender(service);

        NotificationOutbox message = new NotificationOutbox();
        message.setTitle("标题");

        assertThrows(NotificationDeliveryException.class, () -> sender.send(message));
        verify(service, never())
                .createSystemMessage(any(), any(), any(), any(), any(), any(), any(), any());
    }
}
