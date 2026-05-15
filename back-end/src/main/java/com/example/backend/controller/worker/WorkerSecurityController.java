package com.example.backend.controller.worker;

import com.example.backend.common.Result;
import com.example.backend.model.worker.WorkerChangeEmailRequest;
import com.example.backend.model.worker.WorkerChangePasswordRequest;
import com.example.backend.model.worker.WorkerResetPasswordRequest;
import com.example.backend.model.worker.WorkerSendChangeEmailCodeRequest;
import com.example.backend.service.WorkerSecurityService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/worker/security")
public class WorkerSecurityController {

    private final WorkerSecurityService workerSecurityService;

    public WorkerSecurityController(WorkerSecurityService workerSecurityService) {
        this.workerSecurityService = workerSecurityService;
    }

    @PostMapping("/password/change")
    public Result<Void> changePassword(@Valid @RequestBody WorkerChangePasswordRequest request) {
        workerSecurityService.changePassword(request);
        return Result.success();
    }

    @PostMapping("/password/reset/code/send")
    public Result<Void> sendResetPasswordCode() {
        workerSecurityService.sendResetPasswordCode();
        return Result.success();
    }

    @PostMapping("/password/reset")
    public Result<Void> resetPassword(@RequestBody WorkerResetPasswordRequest request) {
        workerSecurityService.resetPasswordByCode(request);
        return Result.success();
    }

    @PostMapping("/email/change/code/send")
    public Result<Void> sendChangeEmailCode(@Valid @RequestBody WorkerSendChangeEmailCodeRequest request) {
        workerSecurityService.sendChangeEmailCode(request == null ? null : request.getNewEmail());
        return Result.success();
    }

    @PostMapping("/email/change")
    public Result<Void> changeEmail(@Valid @RequestBody WorkerChangeEmailRequest request) {
        workerSecurityService.changeEmail(request);
        return Result.success();
    }
}
