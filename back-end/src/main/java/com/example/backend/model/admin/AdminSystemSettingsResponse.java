package com.example.backend.model.admin;

import lombok.Data;

import java.util.List;

@Data
public class AdminSystemSettingsResponse {

    private List<AdminSystemSettingGroupResponse> groups;
}
