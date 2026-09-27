package com.example.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("电器维修预约平台 API")
                                .description(
                                        "基于 SpringBoot 的线上电器维修预约系统接口文档。\n\n"
                                                + "**认证方式**：登录后获取 JWT Token，在请求头添加 `Authorization: Bearer <token>`。\n\n"
                                                + "**角色说明**：\n"
                                                + "- 管理员端 `/api/admin/` — 超级管理员、门店管理员\n"
                                                + "- 用户端 `/api/user/` — 普通用户（微信小程序）\n"
                                                + "- 师傅端 `/api/worker/` — 维修师傅\n"
                                                + "- 公开接口 — 无需认证")
                                .version("2.0.0")
                                .contact(new Contact().name("尹世超").email("3129036103@qq.com")));
    }
}
