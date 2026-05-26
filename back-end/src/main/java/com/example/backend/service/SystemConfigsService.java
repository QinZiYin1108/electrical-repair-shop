package com.example.backend.service;

import com.example.backend.entity.SystemConfigs;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.common.system.SystemConfigDefinition;

import java.util.Collection;
import java.util.Map;

/**
* @author Administrator
* @description 针对表【system_configs(系统配置表)】的数据库操作Service
* @createDate 2026-03-03 11:26:16
*/
public interface SystemConfigsService extends IService<SystemConfigs> {

    String getStringConfig(String key, String defaultValue);

    Integer getIntegerConfig(String key, Integer defaultValue);

    Long getLongConfig(String key, Long defaultValue);

    Boolean getBooleanConfig(String key, Boolean defaultValue);

    Map<String, SystemConfigs> getConfigMap(Collection<String> keys);

    SystemConfigs saveOrUpdateConfig(SystemConfigDefinition definition, String value);
}
