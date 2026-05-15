package com.example.backend.controller.worker;

import com.example.backend.common.Result;
import com.example.backend.model.worker.WorkerCertificationRequest;
import com.example.backend.model.worker.WorkerProfileResponse;
import com.example.backend.model.worker.WorkerUpdateProfileRequest;
import com.example.backend.service.WorkerProfileService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/worker/profile")
public class WorkerProfileController {

    private final WorkerProfileService workerProfileService;

    public WorkerProfileController(WorkerProfileService workerProfileService) {
        this.workerProfileService = workerProfileService;
    }

    @GetMapping("/me")
    public Result<WorkerProfileResponse> getMe() {
        return Result.success(workerProfileService.getCurrentWorkerProfile());
    }

    @PostMapping("/me")
    public Result<Void> updateMe(@RequestBody WorkerUpdateProfileRequest request) {
        workerProfileService.updateCurrentWorkerProfile(request);
        return Result.success();
    }

    @PostMapping("/certification")
    public Result<Void> submitCertification(@RequestBody WorkerCertificationRequest request) {
        workerProfileService.submitCertification(request);
        return Result.success();
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadAvatar(@RequestPart("file") MultipartFile file) {
        return Result.success(workerProfileService.uploadCurrentWorkerAvatar(file));
    }
}
