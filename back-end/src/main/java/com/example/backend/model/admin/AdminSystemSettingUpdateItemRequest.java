package com.example.backend.model.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminSystemSettingUpdateItemRequest {

    @NotBlank(message = "configKey 不能为空")
    private String configKey;

    @NotBlank(message = "configValue 不能为空")
    private String configValue;
}
