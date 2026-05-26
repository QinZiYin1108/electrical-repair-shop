package com.example.backend.service;

import com.example.backend.model.admin.AdminSystemSettingsResponse;
import com.example.backend.model.admin.AdminSystemSettingsUpdateRequest;

public interface AdminSystemSettingsService {

    AdminSystemSettingsResponse getSettings();

    AdminSystemSettingsResponse updateSettings(AdminSystemSettingsUpdateRequest request);
}
