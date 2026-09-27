package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "管理员系统设置Group响应")
@Data
public class AdminSystemSettingGroupResponse {

    @Schema(description = "group名称")
    private String groupName;

    @Schema(description = "group标签")
    private String groupLabel;

    @Schema(description = "items")
    private List<AdminSystemSettingItemResponse> items;
}
