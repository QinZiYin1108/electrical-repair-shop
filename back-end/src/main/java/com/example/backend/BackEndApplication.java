package com.example.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;

@SpringBootApplication
@MapperScan("com.example.backend.mapper")
@EnableScheduling
public class BackEndApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(BackEndApplication.class, args);
        Environment environment = context.getEnvironment();

        String[] activeProfiles = environment.getActiveProfiles();
        String profileLabel = activeProfiles.length == 0
            ? Arrays.toString(environment.getDefaultProfiles())
            : Arrays.toString(activeProfiles);
        String port = environment.getProperty("local.server.port",
            environment.getProperty("server.port", "8080"));
        String contextPath = environment.getProperty("server.servlet.context-path", "");
        String baseUrl = environment.getProperty("app.base-url");
        String accessUrl = buildAccessUrl(baseUrl, port, contextPath);

        System.out.println("Backend started successfully.");
        System.out.println("Active profiles: " + profileLabel);
        System.out.println("Access URL: " + accessUrl);
    }

    private static String buildAccessUrl(String baseUrl, String port, String contextPath) {
        String normalizedContextPath = contextPath == null ? "" : contextPath.trim();
        if (baseUrl != null && !baseUrl.isBlank()) {
            return trimTrailingSlash(baseUrl) + normalizedContextPath;
        }
        return "http://localhost:" + port + normalizedContextPath;
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
