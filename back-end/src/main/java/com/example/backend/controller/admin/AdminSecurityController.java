package com.example.backend.controller.admin;

import com.example.backend.common.Result;
import com.example.backend.model.admin.AdminChangeEmailRequest;
import com.example.backend.model.admin.AdminChangePasswordRequest;
import com.example.backend.model.admin.AdminSendChangeEmailCodeRequest;
import com.example.backend.service.AdminSecurityService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/security")
public class AdminSecurityController {

    private final AdminSecurityService adminSecurityService;

    public AdminSecurityController(AdminSecurityService adminSecurityService) {
        this.adminSecurityService = adminSecurityService;
    }

    @PostMapping("/password/change")
    public Result<Void> changePassword(@Valid @RequestBody AdminChangePasswordRequest request) {
        adminSecurityService.changePassword(request);
        return Result.success();
    }

    @PostMapping("/email/change/code/send")
    public Result<Void> sendChangeEmailCode(@Valid @RequestBody AdminSendChangeEmailCodeRequest request) {
        adminSecurityService.sendChangeEmailCode(request.getNewEmail());
        return Result.success();
    }

    @PostMapping("/email/change")
    public Result<Void> changeEmail(@Valid @RequestBody AdminChangeEmailRequest request) {
        adminSecurityService.changeEmail(request);
        return Result.success();
    }
}
