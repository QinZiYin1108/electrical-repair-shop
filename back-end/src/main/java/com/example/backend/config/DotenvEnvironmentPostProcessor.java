package com.example.backend.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * 在 Spring 加载配置之前将 .env 文件中的变量注入环境。
 * 通过 spring.factories 自动注册，无需额外配置。
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // 从 back-end/ 往上一级找到项目根目录的 .env
        Dotenv dotenv = Dotenv.configure()
                .directory("../")
                .ignoreIfMissing()
                .load();

        Map<String, Object> envMap = new HashMap<>();
        dotenv.entries().forEach(e -> {
            // 只添加 Spring 环境中尚未存在的变量（命令行 > .env > 默认值）
            if (!environment.containsProperty(e.getKey())) {
                envMap.put(e.getKey(), e.getValue());
            }
        });

        if (!envMap.isEmpty()) {
            environment.getPropertySources()
                    .addFirst(new MapPropertySource("dotenv", envMap));
        }
    }
}
