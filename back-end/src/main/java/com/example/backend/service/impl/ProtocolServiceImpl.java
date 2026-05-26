package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.system.SystemConfigDefinition;
import com.example.backend.entity.Files;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.admin.AdminProtocolItemResponse;
import com.example.backend.model.common.ProtocolContentResponse;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.FilesService;
import com.example.backend.service.ProtocolService;
import com.example.backend.service.SystemConfigsService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import com.example.backend.utils.oss.OssUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProtocolServiceImpl implements ProtocolService {

    private static final String TYPE_USER = "user";
    private static final String TYPE_PRIVACY = "privacy";
    private static final String BUSINESS_TYPE_PROTOCOL = "PROTOCOL_DOCUMENT";
    private static final int UPLOADER_TYPE_ADMIN = 3;
    private static final DateTimeFormatter OBJECT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final FilesService filesService;
    private final SystemConfigsService systemConfigsService;
    private final OssUtil ossUtil;

    public ProtocolServiceImpl(
        FilesService filesService,
        SystemConfigsService systemConfigsService,
        OssUtil ossUtil
    ) {
        this.filesService = filesService;
        this.systemConfigsService = systemConfigsService;
        this.ossUtil = ossUtil;
    }

    @Override
    public List<AdminProtocolItemResponse> listProtocols() {
        List<AdminProtocolItemResponse> list = new ArrayList<>();
        list.add(toAdminResponse(TYPE_USER));
        list.add(toAdminResponse(TYPE_PRIVACY));
        return list;
    }

    @Override
    public AdminProtocolItemResponse uploadProtocol(String type, MultipartFile file) {
        ProtocolMeta meta = requireMeta(type);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请上传 Markdown 文件");
        }
        String originalName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : meta.defaultFileName;
        String extension = resolveExtension(originalName);
        if (!".md".equals(extension) && !".markdown".equals(extension)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持上传 .md 或 .markdown 文件");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取上传文件失败");
        }
        if (bytes.length == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }

        String objectName = "protocols/" + meta.type + "/" + LocalDateTime.now().format(OBJECT_DATE_FORMAT)
            + "_" + UUID.randomUUID().toString().replace("-", "") + extension;
        String fileUrl = ossUtil.upload(objectName, new ByteArrayInputStream(bytes));

        LoginUserInfo user = AuthUserContext.get();
        long now = System.currentTimeMillis();

        Files entity = new Files();
        entity.setId(SnowflakeIdUtil.nextFileId());
        entity.setOriginalName(originalName);
        entity.setFileName(objectName.substring(objectName.lastIndexOf('/') + 1));
        entity.setFilePath(objectName);
        entity.setFileUrl(fileUrl);
        entity.setFileSize((long) bytes.length);
        entity.setMimeType(resolveMimeType(file.getContentType()));
        entity.setFileExtension(extension);
        entity.setUploaderId(user == null ? "SYSTEM" : user.getAccountId());
        entity.setUploaderType(UPLOADER_TYPE_ADMIN);
        entity.setBusinessType(BUSINESS_TYPE_PROTOCOL);
        entity.setBusinessId(meta.type);
        entity.setCreatedTime(now);
        entity.setVersion(1);
        entity.setIsDelete(0);
        if (!filesService.save(entity)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存协议文件记录失败");
        }

        systemConfigsService.saveOrUpdateConfig(meta.fileIdConfigDefinition(), entity.getId());
        return buildAdminResponse(meta, entity);
    }

    @Override
    public ProtocolContentResponse getProtocolContent(String type) {
        ProtocolMeta meta = requireMeta(type);
        Files file = getCurrentProtocolFile(meta);
        ProtocolContentResponse response = new ProtocolContentResponse();
        response.setType(meta.type);
        response.setTitle(meta.title);
        if (file == null) {
            response.setFileName(null);
            response.setContent(buildEmptyContent(meta));
            response.setUpdatedTime(null);
            response.setUploaded(false);
            return response;
        }

        response.setFileName(file.getOriginalName());
        response.setContent(ossUtil.downloadAsString(file.getFilePath()));
        response.setUpdatedTime(file.getCreatedTime());
        response.setUploaded(true);
        return response;
    }

    private AdminProtocolItemResponse toAdminResponse(String type) {
        ProtocolMeta meta = requireMeta(type);
        return buildAdminResponse(meta, getCurrentProtocolFile(meta));
    }

    private AdminProtocolItemResponse buildAdminResponse(ProtocolMeta meta, Files file) {
        AdminProtocolItemResponse response = new AdminProtocolItemResponse();
        response.setType(meta.type);
        response.setTitle(meta.title);
        if (file == null) {
            response.setFileId(null);
            response.setFileName(null);
            response.setFileUrl(null);
            response.setUpdatedTime(null);
            response.setUploaded(false);
            return response;
        }
        response.setFileId(file.getId());
        response.setFileName(file.getOriginalName());
        response.setFileUrl(file.getFileUrl());
        response.setUpdatedTime(file.getCreatedTime());
        response.setUploaded(true);
        return response;
    }

    private Files getCurrentProtocolFile(ProtocolMeta meta) {
        String fileId = systemConfigsService.getStringConfig(meta.fileIdConfigKey, null);
        if (!StringUtils.hasText(fileId)) {
            return filesService.getOne(
                new LambdaQueryWrapper<Files>()
                    .eq(Files::getBusinessType, BUSINESS_TYPE_PROTOCOL)
                    .eq(Files::getBusinessId, meta.type)
                    .eq(Files::getIsDelete, 0)
                    .orderByDesc(Files::getCreatedTime)
                    .last("limit 1"),
                false
            );
        }
        return filesService.getOne(
            new LambdaQueryWrapper<Files>()
                .eq(Files::getId, fileId)
                .eq(Files::getIsDelete, 0)
                .last("limit 1"),
            false
        );
    }

    private ProtocolMeta requireMeta(String type) {
        String normalized = normalizeType(type);
        if (TYPE_USER.equals(normalized)) {
            return new ProtocolMeta(TYPE_USER, "用户协议", "protocol.user_agreement_file_id", "user-agreement.md");
        }
        if (TYPE_PRIVACY.equals(normalized)) {
            return new ProtocolMeta(TYPE_PRIVACY, "隐私协议", "protocol.privacy_policy_file_id", "privacy-policy.md");
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "协议类型不支持");
    }

    private String normalizeType(String type) {
        return type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveExtension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return ".md";
        }
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index >= fileName.length() - 1) {
            return ".md";
        }
        return fileName.substring(index).toLowerCase(Locale.ROOT);
    }

    private String resolveMimeType(String mimeType) {
        if (StringUtils.hasText(mimeType)) {
            return mimeType;
        }
        return "text/markdown";
    }

    private String buildEmptyContent(ProtocolMeta meta) {
        return "# " + meta.title + "\n\n暂未上传" + meta.title + "内容。";
    }

    private static final class ProtocolMeta {
        private final String type;
        private final String title;
        private final String fileIdConfigKey;
        private final String defaultFileName;

        private ProtocolMeta(String type, String title, String fileIdConfigKey, String defaultFileName) {
            this.type = type;
            this.title = title;
            this.fileIdConfigKey = fileIdConfigKey;
            this.defaultFileName = defaultFileName;
        }

        private SystemConfigDefinition fileIdConfigDefinition() {
            return new SystemConfigDefinition(
                fileIdConfigKey,
                "protocol",
                "协议设置",
                title + "文件ID",
                title + "当前启用的文件ID",
                1,
                "",
                null,
                null,
                null,
                TYPE_USER.equals(type) ? 10 : 20
            );
        }
    }
}
