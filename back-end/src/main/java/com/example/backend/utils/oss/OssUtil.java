package com.example.backend.utils.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.example.backend.common.ErrorCode;
import com.example.backend.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class OssUtil {

    private static final Logger log = LoggerFactory.getLogger(OssUtil.class);

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    @Value("${aliyun.oss.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret}")
    private String accessKeySecret;

    public String upload(String objectName, InputStream inputStream) {
        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            ossClient.putObject(bucketName, objectName, inputStream);
        } catch (Exception e) {
            log.error("上传文件到OSS失败: bucket={}, object={}, error={}", bucketName, objectName, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传文件到OSS失败");
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
        return "https://" + bucketName + "." + endpoint + "/" + objectName;
    }

    public String downloadAsString(String objectName) {
        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            try (InputStream inputStream = ossClient.getObject(bucketName, objectName).getObjectContent()) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.error("读取OSS文件失败: bucket={}, object={}, error={}", bucketName, objectName, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取OSS文件失败");
        } catch (Exception e) {
            log.error("从OSS获取文件失败: bucket={}, object={}, error={}", bucketName, objectName, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "从OSS获取文件失败");
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}
