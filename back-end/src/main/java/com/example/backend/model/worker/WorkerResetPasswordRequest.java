package com.example.backend.model.worker;

import lombok.Data;

@Data
public class WorkerResetPasswordRequest {

    private String code;
    private String newPassword;
    private String confirmPassword;
}

