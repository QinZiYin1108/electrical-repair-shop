package com.example.backend.model.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AdminSystemSettingsUpdateRequest {

    @Valid
    @NotEmpty(message = "items 不能为空")
    private List<AdminSystemSettingUpdateItemRequest> items;
}
