package com.example.backend.common.system;

import lombok.Getter;

@Getter
public class SystemConfigDefinition {

    private final String key;
    private final String groupName;
    private final String groupLabel;
    private final String label;
    private final String description;
    private final Integer configType;
    private final String defaultValue;
    private final String unit;
    private final Long minValue;
    private final Long maxValue;
    private final Integer sortOrder;

    public SystemConfigDefinition(
        String key,
        String groupName,
        String groupLabel,
        String label,
        String description,
        Integer configType,
        String defaultValue,
        String unit,
        Long minValue,
        Long maxValue,
        Integer sortOrder
    ) {
        this.key = key;
        this.groupName = groupName;
        this.groupLabel = groupLabel;
        this.label = label;
        this.description = description;
        this.configType = configType;
        this.defaultValue = defaultValue;
        this.unit = unit;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.sortOrder = sortOrder;
    }

}
