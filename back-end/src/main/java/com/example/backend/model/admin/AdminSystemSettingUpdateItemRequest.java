package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理员系统设置UpdateItem请求")
@Data
public class AdminSystemSettingUpdateItemRequest {

    @NotBlank(message = "configKey 不能为空")
    @Schema(description = "配置键")
    private String configKey;

    @NotBlank(message = "configValue 不能为空")
    @Schema(description = "配置值")
    private String configValue;
}
