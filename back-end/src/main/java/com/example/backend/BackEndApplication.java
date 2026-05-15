package com.example.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.example.backend.mapper")
@EnableScheduling
public class BackEndApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackEndApplication.class, args);

        // ANSI颜色代码
        String green = "\u001B[32m";
        String yellow = "\u001B[33m";
        String blue = "\u001B[34m";
        String reset = "\u001B[0m";

        System.out.println(green + "═══════════════════════════════════════" + reset);
        System.out.println(green + "     " + yellow + "后端应用启动成功！" + green + "                 " + reset);
        System.out.println(green + "     " + blue + "访问地址: http://localhost:8080" + green + "    " + reset);
        System.out.println(green + "═══════════════════════════════════════" + reset);
    }
}
