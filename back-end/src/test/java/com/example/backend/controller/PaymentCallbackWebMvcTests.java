package com.example.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.service.PaymentCallbackService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 回调接口的 Web 层契约测试（不依赖数据库）：验证路由、成功应答与失败时应答体，确保渠道可重试。 需要真实数据库/Redis 的并发与事务测试由 Testcontainers 覆盖（需
 * Docker）， 当前环境不可用时跳过。
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentCallbackWebMvcTests {

    @Autowired private MockMvc mockMvc;

    @MockBean private PaymentCallbackService paymentCallbackService;

    @Test
    void wechatNotifyReturnsSuccessAcknowledgement() throws Exception {
        mockMvc.perform(
                        post("/pass/payments/wechat/notify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(header().exists("X-Request-Id"));

        verify(paymentCallbackService).handle(eq(1), any(), eq("{}"));
    }

    @Test
    void failedWechatNotifyReturnsServerErrorSoWechatRetries() throws Exception {
        doThrow(new IllegalStateException("boom"))
                .when(paymentCallbackService)
                .handle(eq(1), any(), anyString());

        mockMvc.perform(
                        post("/pass/payments/wechat/notify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("FAIL"));
    }

    @Test
    void wechatRefundNotifyRoutesToRefundHandler() throws Exception {
        mockMvc.perform(
                        post("/pass/payments/wechat/refund-notify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isOk());

        verify(paymentCallbackService).handleRefund(eq(1), any(), eq("{}"));
    }
}
