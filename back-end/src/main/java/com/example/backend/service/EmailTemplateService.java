package com.example.backend.service;

import com.example.backend.model.admin.AdminEmailTemplateItemResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EmailTemplateService {

    List<AdminEmailTemplateItemResponse> listTemplates();

    AdminEmailTemplateItemResponse uploadTemplate(String type, MultipartFile file);

    String buildAuthCodeHtml(String code, int expireMinutes);
}