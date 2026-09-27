package com.example.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.backend.service.BusinessMetrics;
import com.example.backend.service.PaymentCallbackService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class PaymentCallbackControllerTests {

    @Test
    void successfulWechatCallbackReturnsSuccessAcknowledgement() {
        PaymentCallbackService service = mock(PaymentCallbackService.class);
        PaymentCallbackController controller =
                new PaymentCallbackController(service, mock(BusinessMetrics.class));

        ResponseEntity<?> response = controller.wechatNotify(new HttpHeaders(), "body");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(service).handle(eq(1), any(), eq("body"));
    }

    @Test
    void failedWechatCallbackReturnsServerErrorSoWechatRetries() {
        PaymentCallbackService service = mock(PaymentCallbackService.class);
        doThrow(new IllegalStateException("failed")).when(service).handle(eq(1), any(), eq("body"));
        PaymentCallbackController controller =
                new PaymentCallbackController(service, mock(BusinessMetrics.class));

        ResponseEntity<?> response = controller.wechatNotify(new HttpHeaders(), "body");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
