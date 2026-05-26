package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.system.SystemConfigDefinition;
import com.example.backend.entity.SystemConfigs;
import com.example.backend.exception.BusinessException;
import com.example.backend.service.SystemConfigsService;
import com.example.backend.mapper.SystemConfigsMapper;
import com.example.backend.utils.id.SnowflakeIdUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
* @author Administrator
* @description 针对表【system_configs(系统配置表)】的数据库操作Service实现
* @createDate 2026-03-03 11:26:16
*/
@Service
public class SystemConfigsServiceImpl extends ServiceImpl<SystemConfigsMapper, SystemConfigs>
    implements SystemConfigsService{

    private final Map<String, Optional<SystemConfigs>> configCache = new ConcurrentHashMap<>();

    @Override
    public String getStringConfig(String key, String defaultValue) {
        Optional<SystemConfigs> configOptional = getConfigOptional(key);
        if (configOptional.isEmpty() || !StringUtils.hasText(configOptional.get().getConfigValue())) {
            return defaultValue;
        }
        return configOptional.get().getConfigValue().trim();
    }

    @Override
    public Integer getIntegerConfig(String key, Integer defaultValue) {
        String value = getStringConfig(key, defaultValue == null ? null : String.valueOf(defaultValue));
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    @Override
    public Long getLongConfig(String key, Long defaultValue) {
        String value = getStringConfig(key, defaultValue == null ? null : String.valueOf(defaultValue));
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    @Override
    public Boolean getBooleanConfig(String key, Boolean defaultValue) {
        String value = getStringConfig(key, defaultValue == null ? null : String.valueOf(defaultValue));
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        String normalized = value.trim().toLowerCase();
        if ("true".equals(normalized) || "1".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized) || "0".equals(normalized)) {
            return false;
        }
        return defaultValue;
    }

    @Override
    public Map<String, SystemConfigs> getConfigMap(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, SystemConfigs> result = new LinkedHashMap<>();
        for (String key : keys) {
            Optional<SystemConfigs> optional = getConfigOptional(key);
            optional.ifPresent(systemConfigs -> result.put(key, systemConfigs));
        }
        return result;
    }

    @Override
    public synchronized SystemConfigs saveOrUpdateConfig(SystemConfigDefinition definition, String value) {
        if (definition == null || !StringUtils.hasText(definition.getKey())) {
            throw new IllegalArgumentException("definition is invalid");
        }
        String key = definition.getKey().trim();
        long now = System.currentTimeMillis();
        String normalizedValue = value == null ? "" : value.trim();

        SystemConfigs entity = getConfigOptional(key).orElse(null);
        if (entity == null) {
            entity = new SystemConfigs();
            entity.setId(SnowflakeIdUtil.nextSystemConfigId());
            entity.setConfigKey(key);
            entity.setCreatedTime(now);
            entity.setVersion(1);
            entity.setIsDelete(0);
            entity.setIsEncrypted(0);
        }
        entity.setConfigValue(normalizedValue);
        entity.setConfigType(definition.getConfigType());
        entity.setDescription(definition.getDescription());
        entity.setGroupName(definition.getGroupName());
        entity.setIsSystem(1);
        entity.setUpdatedTime(now);

        boolean success;
        if (StringUtils.hasText(entity.getId()) && getById(entity.getId()) != null) {
            success = updateById(entity);
        } else {
            success = save(entity);
        }
        if (!success) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存系统配置失败");
        }
        configCache.put(key, Optional.of(entity));
        return entity;
    }

    private Optional<SystemConfigs> getConfigOptional(String key) {
        if (!StringUtils.hasText(key)) {
            return Optional.empty();
        }
        String normalizedKey = key.trim();
        Optional<SystemConfigs> cached = configCache.get(normalizedKey);
        if (cached != null) {
            return cached;
        }
        Optional<SystemConfigs> loaded = queryConfigOptional(normalizedKey);
        configCache.put(normalizedKey, loaded);
        return loaded;
    }

    private Optional<SystemConfigs> queryConfigOptional(String key) {
        List<SystemConfigs> list = list(
            new LambdaQueryWrapper<SystemConfigs>()
                .eq(SystemConfigs::getConfigKey, key)
                .eq(SystemConfigs::getIsDelete, 0)
                .orderByDesc(SystemConfigs::getUpdatedTime)
                .last("limit 1")
        );
        if (list == null || list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(list.get(0));
    }
}




