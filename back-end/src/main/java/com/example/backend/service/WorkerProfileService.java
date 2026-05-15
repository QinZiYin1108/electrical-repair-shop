package com.example.backend.service;

import com.example.backend.model.worker.WorkerProfileResponse;
import com.example.backend.model.worker.WorkerCertificationRequest;
import com.example.backend.model.worker.WorkerUpdateProfileRequest;
import org.springframework.web.multipart.MultipartFile;

public interface WorkerProfileService {

    WorkerProfileResponse getCurrentWorkerProfile();

    void updateCurrentWorkerProfile(WorkerUpdateProfileRequest request);

    void submitCertification(WorkerCertificationRequest request);

    String uploadCurrentWorkerAvatar(MultipartFile file);
}
