package com.example.backend.service.contentcheck;

import com.aliyun.green20220302.Client;
import com.aliyun.green20220302.models.MultiModalGuardRequest;
import com.aliyun.green20220302.models.MultiModalGuardResponse;
import com.aliyun.green20220302.models.MultiModalGuardResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 阿里云 AI 安全护栏 HTTP 客户端（使用官方 SDK，自动处理 V3 签名）。 支持文本、图片、文件（视频）三种内容类型的审核。 */
@Component
public class AliyunGreenClient {

    private static final Logger log = LoggerFactory.getLogger(AliyunGreenClient.class);

    private static final String TEXT_SERVICE = "query_security_check";
    private static final String IMAGE_SERVICE = "text_img_security_check";
    private static final String FILE_SERVICE = "file_security_sync_check";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Client client;
    private boolean configured;
    private final String failurePolicy;

    public AliyunGreenClient(
            @Value("${aliyun.access-key-id:}") String accessKeyId,
            @Value("${aliyun.access-key-secret:}") String accessKeySecret,
            @Value("${aliyun.green.endpoint:green-cip.cn-shanghai.aliyuncs.com}")
                    String rawEndpoint,
            @Value("${content-check.failure-policy:open}") String failurePolicy) {

        this.failurePolicy = failurePolicy == null ? "open" : failurePolicy.trim().toLowerCase();

        if (accessKeyId == null
                || accessKeyId.isEmpty()
                || accessKeySecret == null
                || accessKeySecret.isEmpty()) {
            this.configured = false;
            log.info("阿里云内容检测未配置（access-key-id 或 access-key-secret 为空），将跳过所有检测");
            return;
        }

        // 防御性处理：用户可能在配置中带上了 https:// 前缀
        String endpoint = rawEndpoint;
        if (endpoint.startsWith("https://")) endpoint = endpoint.substring(8);
        else if (endpoint.startsWith("http://")) endpoint = endpoint.substring(7);

        // 从 endpoint 自动提取 region，如 green-cip.cn-hangzhou.aliyuncs.com → cn-hangzhou
        String region = "cn-shanghai";
        if (endpoint.contains(".")) {
            int start = endpoint.indexOf('.') + 1;
            int end = endpoint.indexOf('.', start);
            if (end > start) region = endpoint.substring(start, end);
        }

        try {
            Config config = new Config();
            config.setAccessKeyId(accessKeyId);
            config.setAccessKeySecret(accessKeySecret);
            config.setRegionId(region);
            config.setEndpoint(endpoint);
            config.setConnectTimeout(5000);
            config.setReadTimeout(10000);
            this.client = new Client(config);
            this.configured = true;
            log.info("阿里云内容检测已配置: endpoint={}, region={}", endpoint, region);
        } catch (Exception e) {
            log.error("阿里云内容检测客户端初始化失败", e);
            this.configured = false;
        }
    }

    // ==================== 文本检测 ====================

    public CheckResult checkText(String text) {
        return doCheck(TEXT_SERVICE, text, null, null);
    }

    // ==================== 图片检测 ====================

    public CheckResult checkImage(String imageUrl) {
        return doCheck(IMAGE_SERVICE, null, Collections.singletonList(imageUrl), null);
    }

    // ==================== 文件/视频检测 ====================

    public CheckResult checkFile(String fileUrl) {
        return doCheck(FILE_SERVICE, null, null, Collections.singletonList(fileUrl));
    }

    // ==================== 核心请求 ====================

    private CheckResult doCheck(
            String service, String text, List<String> imageUrls, List<String> fileUrls) {
        if (!configured) {
            return failureResult("内容审核服务未配置");
        }
        try {
            Map<String, Object> params = new LinkedHashMap<>();
            if (text != null) {
                params.put("content", text);
            }
            if (imageUrls != null && !imageUrls.isEmpty()) {
                params.put("imageUrls", imageUrls);
            }
            if (fileUrls != null && !fileUrls.isEmpty()) {
                params.put("fileUrls", fileUrls);
            }

            MultiModalGuardRequest request = new MultiModalGuardRequest();
            request.setService(service);
            request.setServiceParameters(objectMapper.writeValueAsString(params));

            MultiModalGuardResponse response = client.multiModalGuard(request);
            if (response.getStatusCode() != 200) {
                log.warn("阿里云内容检测返回非200: status={}", response.getStatusCode());
                return failureResult("内容审核服务响应异常");
            }

            MultiModalGuardResponseBody body = response.getBody();
            log.info(
                    "阿里云原始响应 body: code={}, message={}, requestId={}",
                    body.getCode(),
                    body.getMessage(),
                    body.getRequestId());
            if (body.getCode() != 200) {
                log.warn("阿里云内容检测返回非200: code={}, message={}", body.getCode(), body.getMessage());
                return failureResult("内容审核服务业务响应异常");
            }

            return parseResponse(body);

        } catch (Exception e) {
            log.error("阿里云内容检测请求失败", e);
            return failureResult("内容审核服务暂不可用");
        }
    }

    // ==================== 响应解析 ====================

    private CheckResult parseResponse(MultiModalGuardResponseBody body) throws Exception {
        MultiModalGuardResponseBody.MultiModalGuardResponseBodyData data = body.getData();
        if (data == null) {
            return failureResult("内容审核服务返回空结果");
        }

        // 将 SDK 返回对象序列化为 JSON 后再解析
        String dataJson = objectMapper.writeValueAsString(data);
        log.info("阿里云原始响应 dataJson: {}", dataJson);
        JsonNode root = objectMapper.readTree(dataJson);
        // SDK 序列化后字段名为小写，注意大小写
        String suggestion = root.path("suggestion").asText("pass");

        String label = "";
        String labelDesc = "";
        JsonNode details = root.path("detail");
        if (details.isArray() && details.size() > 0) {
            for (JsonNode detail : details) {
                JsonNode results = detail.path("result");
                if (results.isArray() && results.size() > 0) {
                    JsonNode first = results.get(0);
                    label = first.path("label").asText("");
                    labelDesc = first.path("description").asText("");
                    // nonLabel 表示未检出风险，跳过继续找真正违规的标签
                    if (!label.isEmpty() && !"nonLabel".equals(label)) break;
                }
            }
        }

        return CheckResult.fromSuggestion(suggestion, label, labelDesc);
    }

    private CheckResult failureResult(String message) {
        if ("closed".equals(failurePolicy)) {
            return CheckResult.blocked("error", "service_unavailable", message);
        }
        if ("quarantine".equals(failurePolicy)) {
            return CheckResult.watch("error", "service_unavailable", message);
        }
        return CheckResult.passed();
    }
}
