package com.example.backend.controller.admin;

import com.example.backend.common.Result;
import com.example.backend.model.admin.AdminSystemSettingsResponse;
import com.example.backend.model.admin.AdminSystemSettingsUpdateRequest;
import com.example.backend.service.AdminSystemSettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/system/settings")
public class AdminSystemSettingsController {

    private final AdminSystemSettingsService adminSystemSettingsService;

    public AdminSystemSettingsController(AdminSystemSettingsService adminSystemSettingsService) {
        this.adminSystemSettingsService = adminSystemSettingsService;
    }

    @GetMapping
    public Result<AdminSystemSettingsResponse> getSettings() {
        return Result.success(adminSystemSettingsService.getSettings());
    }

    @PostMapping("/update")
    public Result<AdminSystemSettingsResponse> updateSettings(@Valid @RequestBody AdminSystemSettingsUpdateRequest request) {
        return Result.success(adminSystemSettingsService.updateSettings(request));
    }
}
