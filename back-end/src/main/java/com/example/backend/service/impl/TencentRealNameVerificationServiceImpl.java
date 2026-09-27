package com.example.backend.service.impl;

import com.example.backend.service.RealNameVerificationService;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.faceid.v20180301.FaceidClient;
import com.tencentcloudapi.faceid.v20180301.models.IdCardVerificationRequest;
import com.tencentcloudapi.faceid.v20180301.models.IdCardVerificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TencentRealNameVerificationServiceImpl implements RealNameVerificationService {

    private static final Logger log =
            LoggerFactory.getLogger(TencentRealNameVerificationServiceImpl.class);

    /** Result code meaning "verified / passed" */
    private static final String RESULT_PASSED = "0";

    @Value("${tencent.sms.secret-id}")
    private String secretId;

    @Value("${tencent.sms.secret-key}")
    private String secretKey;

    @Value("${tencent.faceid.endpoint}")
    private String endpoint;

    @Override
    public VerificationResult verify(String realName, String idCard) {
        try {
            Credential cred = new Credential(secretId, secretKey);

            com.tencentcloudapi.common.profile.HttpProfile httpProfile =
                    new com.tencentcloudapi.common.profile.HttpProfile();
            httpProfile.setEndpoint(endpoint);

            com.tencentcloudapi.common.profile.ClientProfile clientProfile =
                    new com.tencentcloudapi.common.profile.ClientProfile();
            clientProfile.setHttpProfile(httpProfile);

            FaceidClient client = new FaceidClient(cred, "ap-guangzhou", clientProfile);

            IdCardVerificationRequest req = new IdCardVerificationRequest();
            req.setIdCard(idCard);
            req.setName(realName);

            IdCardVerificationResponse resp = client.IdCardVerification(req);

            String result = resp.getResult();
            String description = resp.getDescription();
            boolean passed = RESULT_PASSED.equals(result);

            log.info(
                    "实名核验结果: realName={}, result={}, description={}",
                    realName,
                    result,
                    description);

            return new VerificationResult(passed, result, description);

        } catch (TencentCloudSDKException e) {
            log.error("实名核验 API 异常: realName={}, error={}", realName, e.getMessage(), e);
            return new VerificationResult(false, "API_ERROR", "认证服务暂不可用，请稍后重试");
        }
    }
}
