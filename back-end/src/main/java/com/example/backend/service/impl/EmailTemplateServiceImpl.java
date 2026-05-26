package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.system.SystemConfigDefinition;
import com.example.backend.entity.Files;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.admin.AdminEmailTemplateItemResponse;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.EmailTemplateService;
import com.example.backend.service.FilesService;
import com.example.backend.service.SystemConfigsService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import com.example.backend.utils.oss.OssUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class EmailTemplateServiceImpl implements EmailTemplateService {

    private static final Logger log = LoggerFactory.getLogger(EmailTemplateServiceImpl.class);

    private static final String TYPE_AUTH_CODE = "auth_code";
    private static final String BUSINESS_TYPE_EMAIL_TEMPLATE = "EMAIL_TEMPLATE";
    private static final int UPLOADER_TYPE_ADMIN = 3;
    private static final String DEFAULT_AUTH_CODE_TEMPLATE_LOCATION = "classpath:auth-code-template.html";
    private static final DateTimeFormatter OBJECT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final FilesService filesService;
    private final SystemConfigsService systemConfigsService;
    private final OssUtil ossUtil;
    private final ResourceLoader resourceLoader;

    private volatile String cachedDefaultAuthCodeTemplate;

    public EmailTemplateServiceImpl(
        FilesService filesService,
        SystemConfigsService systemConfigsService,
        OssUtil ossUtil,
        ResourceLoader resourceLoader
    ) {
        this.filesService = filesService;
        this.systemConfigsService = systemConfigsService;
        this.ossUtil = ossUtil;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public List<AdminEmailTemplateItemResponse> listTemplates() {
        List<AdminEmailTemplateItemResponse> list = new ArrayList<>();
        list.add(toAdminResponse(TYPE_AUTH_CODE));
        return list;
    }

    @Override
    public AdminEmailTemplateItemResponse uploadTemplate(String type, MultipartFile file) {
        EmailTemplateMeta meta = requireMeta(type);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请上传 HTML 模板文件");
        }
        String originalName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : meta.defaultFileName;
        String extension = resolveExtension(originalName);
        if (!".html".equals(extension) && !".htm".equals(extension)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持上传 .html 或 .htm 模板文件");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取上传模板文件失败");
        }
        if (bytes.length == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传模板文件不能为空");
        }

        String objectName = "email-templates/" + meta.type + "/" + LocalDateTime.now().format(OBJECT_DATE_FORMAT)
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
        entity.setBusinessType(BUSINESS_TYPE_EMAIL_TEMPLATE);
        entity.setBusinessId(meta.type);
        entity.setCreatedTime(now);
        entity.setVersion(1);
        entity.setIsDelete(0);
        if (!filesService.save(entity)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存邮件模板文件记录失败");
        }

        systemConfigsService.saveOrUpdateConfig(meta.fileIdConfigDefinition(), entity.getId());
        return buildAdminResponse(meta, entity);
    }

    @Override
    public String buildAuthCodeHtml(String code, int expireMinutes) {
        EmailTemplateMeta meta = requireMeta(TYPE_AUTH_CODE);
        Files file = getCurrentTemplateFile(meta);
        log.info("构建邮件模板: type={}, hasCustomTemplate={}", TYPE_AUTH_CODE, file != null);
        if (file != null) {
            log.info("使用自定义模板: fileId={}, filePath={}", file.getId(), file.getFilePath());
        }
        String template = file == null ? loadDefaultAuthCodeTemplate() : ossUtil.downloadAsString(file.getFilePath());
        return renderAuthCodeTemplate(template, code, expireMinutes);
    }

    private AdminEmailTemplateItemResponse toAdminResponse(String type) {
        EmailTemplateMeta meta = requireMeta(type);
        return buildAdminResponse(meta, getCurrentTemplateFile(meta));
    }

    private AdminEmailTemplateItemResponse buildAdminResponse(EmailTemplateMeta meta, Files file) {
        AdminEmailTemplateItemResponse response = new AdminEmailTemplateItemResponse();
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

    private Files getCurrentTemplateFile(EmailTemplateMeta meta) {
        String fileId = systemConfigsService.getStringConfig(meta.fileIdConfigKey, null);
        if (!StringUtils.hasText(fileId)) {
            return filesService.getOne(
                new LambdaQueryWrapper<Files>()
                    .eq(Files::getBusinessType, BUSINESS_TYPE_EMAIL_TEMPLATE)
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

    private String loadDefaultAuthCodeTemplate() {
        if (cachedDefaultAuthCodeTemplate != null) {
            return cachedDefaultAuthCodeTemplate;
        }
        try {
            Resource resource = resourceLoader.getResource(DEFAULT_AUTH_CODE_TEMPLATE_LOCATION);
            if (!resource.exists()) {
                throw new IllegalStateException("默认验证码邮件模板不存在");
            }
            try (InputStream inputStream = resource.getInputStream()) {
                cachedDefaultAuthCodeTemplate = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                return cachedDefaultAuthCodeTemplate;
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "加载验证码邮件模板失败");
        }
    }

    private String renderAuthCodeTemplate(String template, String code, int expireMinutes) {
        String rendered = template;
        rendered = replacePlaceholder(rendered, "code", code);
        rendered = replacePlaceholder(rendered, "expireMinutes", String.valueOf(expireMinutes));
        rendered = replacePlaceholder(rendered, "expire_minutes", String.valueOf(expireMinutes));

        rendered = rendered.replace("123456", code);
        rendered = rendered.replace(">5</strong>", ">" + expireMinutes + "</strong>");
        return rendered;
    }

    private String replacePlaceholder(String template, String key, String value) {
        return template
            .replace("{{" + key + "}}", value)
            .replace("{{ " + key + " }}", value);
    }

    private EmailTemplateMeta requireMeta(String type) {
        String normalized = normalizeType(type);
        if (TYPE_AUTH_CODE.equals(normalized)) {
            return new EmailTemplateMeta(TYPE_AUTH_CODE, "验证码邮件模板", "email_template.auth_code_file_id", "auth-code-template.html");
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的邮件模板类型");
    }

    private String normalizeType(String type) {
        return type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveExtension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return ".html";
        }
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index >= fileName.length() - 1) {
            return ".html";
        }
        return fileName.substring(index).toLowerCase(Locale.ROOT);
    }

    private String resolveMimeType(String mimeType) {
        if (StringUtils.hasText(mimeType)) {
            return mimeType;
        }
        return "text/html";
    }

    private static final class EmailTemplateMeta {
        private final String type;
        private final String title;
        private final String fileIdConfigKey;
        private final String defaultFileName;

        private EmailTemplateMeta(String type, String title, String fileIdConfigKey, String defaultFileName) {
            this.type = type;
            this.title = title;
            this.fileIdConfigKey = fileIdConfigKey;
            this.defaultFileName = defaultFileName;
        }

        private SystemConfigDefinition fileIdConfigDefinition() {
            return new SystemConfigDefinition(
                fileIdConfigKey,
                "email_template",
                "邮件模板设置",
                title + "文件ID",
                title + "当前启用的文件ID",
                1,
                "",
                null,
                null,
                null,
                10
            );
        }
    }
}