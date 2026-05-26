package com.example.backend.common.system;

import com.example.backend.entity.SystemConfigs;
import com.example.backend.service.SystemConfigsService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SystemConfigBootstrap implements ApplicationRunner {

    private final SystemConfigsService systemConfigsService;

    public SystemConfigBootstrap(SystemConfigsService systemConfigsService) {
        this.systemConfigsService = systemConfigsService;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<SystemConfigDefinition> definitions = SystemConfigRegistry.getDefinitions();
        Map<String, SystemConfigs> existingConfigMap = systemConfigsService.getConfigMap(
            definitions.stream().map(SystemConfigDefinition::getKey).collect(Collectors.toList())
        );

        for (SystemConfigDefinition definition : definitions) {
            if (existingConfigMap.containsKey(definition.getKey())) {
                continue;
            }
            systemConfigsService.saveOrUpdateConfig(definition, definition.getDefaultValue());
        }
    }
}
