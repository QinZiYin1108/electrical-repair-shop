package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Schema(description = "管理员系统SettingsUpdate请求")
@Data
public class AdminSystemSettingsUpdateRequest {

    @Valid
    @NotEmpty(message = "items 不能为空")
    @Schema(description = "items")
    private List<AdminSystemSettingUpdateItemRequest> items;
}
