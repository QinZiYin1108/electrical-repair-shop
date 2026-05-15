package com.example.backend.controller.admin;

import com.example.backend.common.Result;
import com.example.backend.model.admin.AdminEmailTemplateItemResponse;
import com.example.backend.service.EmailTemplateService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/admin/system/settings/email-templates")
public class AdminEmailTemplateController {

    private final EmailTemplateService emailTemplateService;

    public AdminEmailTemplateController(EmailTemplateService emailTemplateService) {
        this.emailTemplateService = emailTemplateService;
    }

    @GetMapping
    public Result<List<AdminEmailTemplateItemResponse>> listTemplates() {
        return Result.success(emailTemplateService.listTemplates());
    }

    @PostMapping(value = "/{type}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<AdminEmailTemplateItemResponse> uploadTemplate(
        @PathVariable("type") String type,
        @RequestPart("file") MultipartFile file
    ) {
        return Result.success(emailTemplateService.uploadTemplate(type, file));
    }
}