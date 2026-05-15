package com.example.backend.model.admin;

import lombok.Data;

@Data
public class AdminSystemSettingItemResponse {

    private String configKey;
    private String label;
    private String description;
    private Integer configType;
    private String configValue;
    private String defaultValue;
    private String unit;
    private Long minValue;
    private Long maxValue;
    private Boolean usingDefault;
}
