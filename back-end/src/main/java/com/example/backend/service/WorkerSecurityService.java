package com.example.backend.service;

import com.example.backend.model.worker.WorkerChangeEmailRequest;
import com.example.backend.model.worker.WorkerChangePasswordRequest;
import com.example.backend.model.worker.WorkerResetPasswordRequest;

public interface WorkerSecurityService {

    void changePassword(WorkerChangePasswordRequest request);

    void sendResetPasswordCode();

    void resetPasswordByCode(WorkerResetPasswordRequest request);

    void sendChangeEmailCode(String newEmail);

    void changeEmail(WorkerChangeEmailRequest request);
}

