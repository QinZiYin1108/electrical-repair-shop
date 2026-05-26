package com.example.backend.model.admin;

import lombok.Data;

import java.util.List;

@Data
public class AdminSystemSettingGroupResponse {

    private String groupName;
    private String groupLabel;
    private List<AdminSystemSettingItemResponse> items;
}
