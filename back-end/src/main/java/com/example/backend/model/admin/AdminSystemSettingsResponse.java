package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "管理员系统Settings响应")
@Data
public class AdminSystemSettingsResponse {

    @Schema(description = "groups")
    private List<AdminSystemSettingGroupResponse> groups;
}
