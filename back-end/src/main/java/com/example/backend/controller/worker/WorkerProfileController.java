package com.example.backend.controller.worker;

import com.example.backend.common.Result;
import com.example.backend.model.worker.WorkerCertificationRequest;
import com.example.backend.model.worker.WorkerProfileResponse;
import com.example.backend.model.worker.WorkerUpdateProfileRequest;
import com.example.backend.service.WorkerProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "师傅端/个人信息", description = "查看/修改个人资料、上传头像、实名认证")
@RestController
@RequestMapping("/worker/profile")
public class WorkerProfileController {

    private final WorkerProfileService workerProfileService;

    public WorkerProfileController(WorkerProfileService workerProfileService) {
        this.workerProfileService = workerProfileService;
    }

    @Operation(summary = "查询当前用户")
    @GetMapping("/me")
    public Result<WorkerProfileResponse> getMe() {
        return Result.success(workerProfileService.getCurrentWorkerProfile());
    }

    @Operation(summary = "修改编辑当前用户")
    @PostMapping("/me")
    public Result<Void> updateMe(@RequestBody WorkerUpdateProfileRequest request) {
        workerProfileService.updateCurrentWorkerProfile(request);
        return Result.success();
    }

    @Operation(summary = "提交提交实名认证")
    @PostMapping("/certification")
    public Result<Void> submitCertification(@RequestBody WorkerCertificationRequest request) {
        workerProfileService.submitCertification(request);
        return Result.success();
    }

    @Operation(summary = "上传上传头像")
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadAvatar(@RequestPart("file") MultipartFile file) {
        return Result.success(workerProfileService.uploadCurrentWorkerAvatar(file));
    }
}
