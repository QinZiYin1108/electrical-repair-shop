package com.example.backend.controller.admin;

import com.example.backend.common.Result;
import com.example.backend.model.admin.AdminChangeEmailRequest;
import com.example.backend.model.admin.AdminChangePasswordRequest;
import com.example.backend.model.admin.AdminSendChangeEmailCodeRequest;
import com.example.backend.service.AdminSecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "管理员端/安全设置")
@RequestMapping("/admin/security")
public class AdminSecurityController {

    private final AdminSecurityService adminSecurityService;

    public AdminSecurityController(AdminSecurityService adminSecurityService) {
        this.adminSecurityService = adminSecurityService;
    }

    @Operation(summary = "提交修改密码")
    @PostMapping("/password/change")
    public Result<Void> changePassword(@Valid @RequestBody AdminChangePasswordRequest request) {
        adminSecurityService.changePassword(request);
        return Result.success();
    }

    @Operation(summary = "发送发送修改邮箱验证码")
    @PostMapping("/email/change/code/send")
    public Result<Void> sendChangeEmailCode(
            @Valid @RequestBody AdminSendChangeEmailCodeRequest request) {
        adminSecurityService.sendChangeEmailCode(request.getNewEmail());
        return Result.success();
    }

    @Operation(summary = "提交修改邮箱")
    @PostMapping("/email/change")
    public Result<Void> changeEmail(@Valid @RequestBody AdminChangeEmailRequest request) {
        adminSecurityService.changeEmail(request);
        return Result.success();
    }
}
