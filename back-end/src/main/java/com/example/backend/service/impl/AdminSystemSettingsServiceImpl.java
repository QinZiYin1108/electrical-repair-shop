package com.example.backend.service.impl;

import com.example.backend.common.ErrorCode;
import com.example.backend.common.system.SystemConfigDefinition;
import com.example.backend.common.system.SystemConfigRegistry;
import com.example.backend.entity.SystemConfigs;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.admin.AdminSystemSettingGroupResponse;
import com.example.backend.model.admin.AdminSystemSettingItemResponse;
import com.example.backend.model.admin.AdminSystemSettingUpdateItemRequest;
import com.example.backend.model.admin.AdminSystemSettingsResponse;
import com.example.backend.model.admin.AdminSystemSettingsUpdateRequest;
import com.example.backend.service.AdminSystemSettingsService;
import com.example.backend.service.SystemConfigsService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AdminSystemSettingsServiceImpl implements AdminSystemSettingsService {

    private final SystemConfigsService systemConfigsService;

    public AdminSystemSettingsServiceImpl(SystemConfigsService systemConfigsService) {
        this.systemConfigsService = systemConfigsService;
    }

    @Override
    public AdminSystemSettingsResponse getSettings() {
        List<SystemConfigDefinition> definitions = SystemConfigRegistry.getDefinitions();
        Map<String, SystemConfigs> configMap = systemConfigsService.getConfigMap(
            definitions.stream().map(SystemConfigDefinition::getKey).collect(Collectors.toList())
        );

        Map<String, AdminSystemSettingGroupResponse> groupMap = new LinkedHashMap<>();
        for (SystemConfigDefinition definition : definitions) {
            AdminSystemSettingGroupResponse group = groupMap.computeIfAbsent(definition.getGroupName(), key -> {
                AdminSystemSettingGroupResponse response = new AdminSystemSettingGroupResponse();
                response.setGroupName(definition.getGroupName());
                response.setGroupLabel(definition.getGroupLabel());
                response.setItems(new ArrayList<>());
                return response;
            });
            group.getItems().add(toItemResponse(definition, configMap.get(definition.getKey())));
        }

        AdminSystemSettingsResponse response = new AdminSystemSettingsResponse();
        response.setGroups(new ArrayList<>(groupMap.values()));
        return response;
    }

    @Override
    public AdminSystemSettingsResponse updateSettings(AdminSystemSettingsUpdateRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "未提供要更新的配置项");
        }

        Map<String, String> mergedValues = buildMergedValues(request.getItems());
        validateMergedValues(mergedValues);

        for (SystemConfigDefinition definition : SystemConfigRegistry.getDefinitions()) {
            String value = mergedValues.get(definition.getKey());
            if (value == null) {
                continue;
            }
            systemConfigsService.saveOrUpdateConfig(definition, value);
        }
        return getSettings();
    }

    private Map<String, String> buildMergedValues(List<AdminSystemSettingUpdateItemRequest> items) {
        List<SystemConfigDefinition> definitions = SystemConfigRegistry.getDefinitions();
        Map<String, SystemConfigs> configMap = systemConfigsService.getConfigMap(
            definitions.stream().map(SystemConfigDefinition::getKey).collect(Collectors.toList())
        );

        Map<String, String> merged = new LinkedHashMap<>();
        for (SystemConfigDefinition definition : definitions) {
            SystemConfigs config = configMap.get(definition.getKey());
            String currentValue = config == null ? definition.getDefaultValue() : config.getConfigValue();
            merged.put(definition.getKey(), normalizeValue(definition, currentValue));
        }

        for (AdminSystemSettingUpdateItemRequest item : items) {
            String key = trimToNull(item == null ? null : item.getConfigKey());
            if (!StringUtils.hasText(key) || !SystemConfigRegistry.contains(key)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "存在不支持的配置项");
            }
            SystemConfigDefinition definition = SystemConfigRegistry.getDefinition(key);
            merged.put(key, normalizeValue(definition, item.getConfigValue()));
        }
        return merged;
    }

    private void validateMergedValues(Map<String, String> mergedValues) {
        int defaultDays = parseRequiredPositiveInt(mergedValues.get(SystemConfigRegistry.ORDER_APPOINTMENT_DEFAULT_DAYS), "默认预约天数");
        int maxDays = parseRequiredPositiveInt(mergedValues.get(SystemConfigRegistry.ORDER_APPOINTMENT_MAX_DAYS), "最大预约天数");
        if (defaultDays > maxDays) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "默认预约天数不能大于最大预约天数");
        }
    }

    private AdminSystemSettingItemResponse toItemResponse(SystemConfigDefinition definition, SystemConfigs config) {
        String currentValue = config == null ? definition.getDefaultValue() : normalizeValue(definition, config.getConfigValue());
        AdminSystemSettingItemResponse response = new AdminSystemSettingItemResponse();
        response.setConfigKey(definition.getKey());
        response.setLabel(definition.getLabel());
        response.setDescription(definition.getDescription());
        response.setConfigType(definition.getConfigType());
        response.setConfigValue(currentValue);
        response.setDefaultValue(definition.getDefaultValue());
        response.setUnit(definition.getUnit());
        response.setMinValue(definition.getMinValue());
        response.setMaxValue(definition.getMaxValue());
        response.setUsingDefault(config == null || Objects.equals(currentValue, definition.getDefaultValue()));
        return response;
    }

    private String normalizeValue(SystemConfigDefinition definition, String rawValue) {
        String value = trimToNull(rawValue);
        if (!StringUtils.hasText(value)) {
            value = definition.getDefaultValue();
        }
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, definition.getLabel() + "不能为空");
        }
        if (Objects.equals(definition.getConfigType(), SystemConfigRegistry.TYPE_NUMBER)) {
            long parsed;
            try {
                parsed = Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, definition.getLabel() + "必须为数字");
            }
            if (definition.getMinValue() != null && parsed < definition.getMinValue()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, definition.getLabel() + "不能小于" + definition.getMinValue());
            }
            if (definition.getMaxValue() != null && parsed > definition.getMaxValue()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, definition.getLabel() + "不能大于" + definition.getMaxValue());
            }
            return String.valueOf(parsed);
        }
        if (Objects.equals(definition.getConfigType(), SystemConfigRegistry.TYPE_BOOLEAN)) {
            if ("true".equalsIgnoreCase(value) || "1".equals(value)) {
                return "true";
            }
            if ("false".equalsIgnoreCase(value) || "0".equals(value)) {
                return "false";
            }
            throw new BusinessException(ErrorCode.PARAM_ERROR, definition.getLabel() + "必须为布尔值");
        }
        return value;
    }

    private int parseRequiredPositiveInt(String value, String label) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, label + "必须大于0");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, label + "格式错误");
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
