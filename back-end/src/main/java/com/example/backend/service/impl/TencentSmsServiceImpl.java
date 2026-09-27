package com.example.backend.service.impl;

import com.example.backend.common.ErrorCode;
import com.example.backend.exception.BusinessException;
import com.example.backend.service.SmsService;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TencentSmsServiceImpl implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(TencentSmsServiceImpl.class);

    @Value("${tencent.sms.secret-id}")
    private String secretId;

    @Value("${tencent.sms.secret-key}")
    private String secretKey;

    @Value("${tencent.sms.sdk-app-id}")
    private String sdkAppId;

    @Value("${tencent.sms.sign-name}")
    private String signName;

    @Value("${tencent.sms.login-template-id}")
    private String loginTemplateId;

    @Value("${tencent.sms.reset-password-template-id}")
    private String resetPasswordTemplateId;

    @Value("${tencent.sms.change-phone-template-id}")
    private String changePhoneTemplateId;

    @Value("${tencent.sms.endpoint}")
    private String endpoint;

    @Override
    public void sendCode(String phone, String code, int expireMinutes, String type) {
        // 模板参数：验证码、有效期（分钟）
        String[] templateParamSet = {code, String.valueOf(expireMinutes)};
        execute(phone, resolveTemplateId(type), templateParamSet, "发送短信验证码失败");
    }

    @Override
    public void sendNotification(String phone, String templateId, java.util.List<String> params) {
        if (templateId == null || templateId.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "短信模板ID不能为空");
        }
        String[] templateParamSet = params == null ? new String[0] : params.toArray(new String[0]);
        execute(phone, templateId, templateParamSet, "发送业务通知短信失败");
    }

    /** 统一的短信发送执行：构造客户端、下发短信、记录结果。 */
    private void execute(
            String phone, String templateId, String[] templateParamSet, String failMessage) {
        try {
            Credential cred = new Credential(secretId, secretKey);
            com.tencentcloudapi.common.profile.HttpProfile httpProfile =
                    new com.tencentcloudapi.common.profile.HttpProfile();
            httpProfile.setEndpoint(endpoint);
            com.tencentcloudapi.common.profile.ClientProfile clientProfile =
                    new com.tencentcloudapi.common.profile.ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            SmsClient client = new SmsClient(cred, "ap-guangzhou", clientProfile);

            SendSmsRequest req = new SendSmsRequest();
            req.setSmsSdkAppId(sdkAppId);
            req.setSignName(signName);
            req.setTemplateId(templateId);

            req.setTemplateParamSet(templateParamSet);

            // 手机号格式：+86 前缀
            String[] phoneNumberSet = {"+86" + phone};
            req.setPhoneNumberSet(phoneNumberSet);

            SendSmsResponse resp = client.SendSms(req);
            log.info(
                    "短信发送结果: phone={}, templateId={}, requestId={}, status={}",
                    phone,
                    templateId,
                    resp.getRequestId(),
                    java.util.Arrays.toString(resp.getSendStatusSet()));

        } catch (TencentCloudSDKException e) {
            log.error("发送短信失败: phone={}, error={}", phone, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, failMessage);
        }
    }

    /** 根据验证码类型选择对应的短信模板 ID */
    private String resolveTemplateId(String type) {
        if (type == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码类型不能为空");
        }
        String upper = type.toUpperCase();
        if (upper.equals("ADMIN_LOGIN") || upper.equals("WORKER_LOGIN")) {
            return loginTemplateId;
        }
        if (upper.equals("ADMIN_RESET_PASSWORD") || upper.equals("WORKER_RESET_PASSWORD")) {
            return resetPasswordTemplateId;
        }
        // ADMIN_CHANGE_PHONE / WORKER_CHANGE_PHONE / USER_BIND_PHONE
        return changePhoneTemplateId;
    }
}
