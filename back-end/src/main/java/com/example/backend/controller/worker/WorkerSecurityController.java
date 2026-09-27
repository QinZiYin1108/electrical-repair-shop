package com.example.backend.controller.worker;

import com.example.backend.common.Result;
import com.example.backend.model.worker.WorkerChangeEmailRequest;
import com.example.backend.model.worker.WorkerChangePasswordRequest;
import com.example.backend.model.worker.WorkerResetPasswordRequest;
import com.example.backend.model.worker.WorkerSendChangeEmailCodeRequest;
import com.example.backend.service.WorkerSecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "师傅端/安全设置")
@RequestMapping("/worker/security")
public class WorkerSecurityController {

    private final WorkerSecurityService workerSecurityService;

    public WorkerSecurityController(WorkerSecurityService workerSecurityService) {
        this.workerSecurityService = workerSecurityService;
    }

    @Operation(summary = "提交修改密码")
    @PostMapping("/password/change")
    public Result<Void> changePassword(@Valid @RequestBody WorkerChangePasswordRequest request) {
        workerSecurityService.changePassword(request);
        return Result.success();
    }

    @Operation(summary = "发送发送重置密码验证码")
    @PostMapping("/password/reset/code/send")
    public Result<Void> sendResetPasswordCode() {
        workerSecurityService.sendResetPasswordCode();
        return Result.success();
    }

    @Operation(summary = "提交重置密码")
    @PostMapping("/password/reset")
    public Result<Void> resetPassword(@RequestBody WorkerResetPasswordRequest request) {
        workerSecurityService.resetPasswordByCode(request);
        return Result.success();
    }

    @Operation(summary = "发送发送修改邮箱验证码")
    @PostMapping("/email/change/code/send")
    public Result<Void> sendChangeEmailCode(
            @Valid @RequestBody WorkerSendChangeEmailCodeRequest request) {
        workerSecurityService.sendChangeEmailCode(request == null ? null : request.getNewEmail());
        return Result.success();
    }

    @Operation(summary = "提交修改邮箱")
    @PostMapping("/email/change")
    public Result<Void> changeEmail(@Valid @RequestBody WorkerChangeEmailRequest request) {
        workerSecurityService.changeEmail(request);
        return Result.success();
    }
}
