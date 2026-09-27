package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理员系统设置Item响应")
@Data
public class AdminSystemSettingItemResponse {

    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "标签")
    private String label;

    @Schema(description = "description")
    private String description;

    @Schema(description = "配置类型")
    private Integer configType;

    @Schema(description = "配置值")
    private String configValue;

    @Schema(description = "default值")
    private String defaultValue;

    @Schema(description = "unit")
    private String unit;

    @Schema(description = "min值")
    private Long minValue;

    @Schema(description = "max值")
    private Long maxValue;

    @Schema(description = "usingDefault")
    private Boolean usingDefault;
}
