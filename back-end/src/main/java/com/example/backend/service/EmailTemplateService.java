package com.example.backend.service;

import com.example.backend.model.admin.AdminEmailTemplateItemResponse;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface EmailTemplateService {

    List<AdminEmailTemplateItemResponse> listTemplates();

    AdminEmailTemplateItemResponse uploadTemplate(String type, MultipartFile file);

    String buildAuthCodeHtml(String code, int expireMinutes);
}
