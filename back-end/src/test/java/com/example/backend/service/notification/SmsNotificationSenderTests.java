package com.example.backend.service.notification;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.backend.entity.NotificationOutbox;
import com.example.backend.service.SmsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SmsNotificationSenderTests {

    @Test
    void disabledChannelFails() {
        SmsService smsService = mock(SmsService.class);
        SmsNotificationSender sender = new SmsNotificationSender(smsService, new ObjectMapper());

        NotificationOutbox message = new NotificationOutbox();
        message.setTemplateCode("TPL");
        message.setPayload("{\"phone\":\"13800000000\"}");

        assertThrows(NotificationDeliveryException.class, () -> sender.send(message));
    }

    @Test
    void enabledChannelSendsWithParams() {
        SmsService smsService = mock(SmsService.class);
        SmsNotificationSender sender = new SmsNotificationSender(smsService, new ObjectMapper());
        ReflectionTestUtils.setField(sender, "enabled", true);

        NotificationOutbox message = new NotificationOutbox();
        message.setTemplateCode("TPL");
        message.setPayload("{\"phone\":\"13800000000\",\"params\":[\"a\",\"b\"]}");

        sender.send(message);

        verify(smsService).sendNotification(eq("13800000000"), eq("TPL"), eq(List.of("a", "b")));
    }
}
