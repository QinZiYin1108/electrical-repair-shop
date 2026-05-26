package com.example.backend;

import com.example.backend.common.system.SystemConfigBootstrap;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class BackEndApplicationTests {

    @MockBean
    private SystemConfigBootstrap systemConfigBootstrap;

    @Test
    void contextLoads() {
    }
}
