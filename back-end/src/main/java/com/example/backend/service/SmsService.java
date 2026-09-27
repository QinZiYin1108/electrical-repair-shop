package com.example.backend.service;

import java.util.List;

/** 短信发送服务接口 */
public interface SmsService {

    /**
     * 发送短信验证码
     *
     * @param phone 手机号（11 位中国大陆手机号）
     * @param code 6 位数字验证码
     * @param expireMinutes 有效期（分钟），用于短信模板变量
     * @param type 验证码类型（ADMIN_LOGIN / ADMIN_RESET_PASSWORD / ...），用于选择对应短信模板
     */
    void sendCode(String phone, String code, int expireMinutes, String type);

    /**
     * 发送业务通知短信。
     *
     * @param phone 手机号（11 位中国大陆手机号）
     * @param templateId 短信模板 ID
     * @param params 模板参数，顺序与模板占位符一致；可为空
     */
    void sendNotification(String phone, String templateId, List<String> params);
}
