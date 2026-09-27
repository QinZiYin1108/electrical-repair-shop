package com.example.backend.service.notification;

import com.example.backend.entity.NotificationOutbox;
import com.example.backend.service.SmsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 短信通知发送器。默认关闭（{@code notification.sms.enabled=false}），关闭时投递将失败并按退避重试， 待运营开启后再投递。模板变量从 outbox 的
 * payload（JSON）读取：{@code {"phone":"138...","params":["a","b"]}}。
 */
@Service
public class SmsNotificationSender implements NotificationSender {

    private final SmsService smsService;
    private final ObjectMapper objectMapper;

    @Value("${notification.sms.enabled:false}")
    private boolean enabled;

    public SmsNotificationSender(SmsService smsService, ObjectMapper objectMapper) {
        this.smsService = smsService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String channel() {
        return NotificationChannels.SMS;
    }

    @Override
    public void send(NotificationOutbox message) {
        if (!enabled) {
            throw new NotificationDeliveryException("短信渠道未启用");
        }
        if (!StringUtils.hasText(message.getTemplateCode())) {
            throw new NotificationDeliveryException("短信通知缺少模板编码");
        }
        JsonNode payload = readPayload(message.getPayload());
        String phone = payload == null ? null : payload.path("phone").asText(null);
        if (!StringUtils.hasText(phone)) {
            throw new NotificationDeliveryException("短信通知缺少手机号");
        }
        smsService.sendNotification(phone, message.getTemplateCode(), readParams(payload));
    }

    private JsonNode readPayload(String payload) {
        if (!StringUtils.hasText(payload)) {
            return null;
        }
        try {
            return objectMapper.readTree(payload);
        } catch (Exception ex) {
            throw new NotificationDeliveryException("短信模板变量解析失败", ex);
        }
    }

    private List<String> readParams(JsonNode payload) {
        List<String> params = new ArrayList<>();
        if (payload == null) {
            return params;
        }
        JsonNode array = payload.path("params");
        if (array.isArray()) {
            for (JsonNode item : array) {
                params.add(item.asText());
            }
        }
        return params;
    }
}
