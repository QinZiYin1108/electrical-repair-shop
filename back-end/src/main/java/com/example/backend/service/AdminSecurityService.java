package com.example.backend.service;

import com.example.backend.model.admin.AdminChangeEmailRequest;
import com.example.backend.model.admin.AdminChangePasswordRequest;

public interface AdminSecurityService {

    void changePassword(AdminChangePasswordRequest request);

    void sendChangeEmailCode(String newEmail);

    void changeEmail(AdminChangeEmailRequest request);
}
