package com.example.backend.controller;

import com.example.backend.service.BusinessMetrics;
import com.example.backend.service.PaymentCallbackService;
import java.util.Collections;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pass/payments")
public class PaymentCallbackController {
    private static final Logger log = LoggerFactory.getLogger(PaymentCallbackController.class);

    private final PaymentCallbackService paymentCallbackService;
    private final BusinessMetrics businessMetrics;

    public PaymentCallbackController(
            PaymentCallbackService paymentCallbackService, BusinessMetrics businessMetrics) {
        this.paymentCallbackService = paymentCallbackService;
        this.businessMetrics = businessMetrics;
    }

    @PostMapping("/wechat/notify")
    public ResponseEntity<Map<String, String>> wechatNotify(
            @RequestHeader HttpHeaders headers, @RequestBody String body) {
        try {
            paymentCallbackService.handle(1, headers.toSingleValueMap(), body);
            return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "成功"));
        } catch (RuntimeException ex) {
            log.error("微信支付回调处理失败", ex);
            businessMetrics.paymentCallbackFailed(1);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", "FAIL", "message", "处理失败"));
        }
    }

    @PostMapping("/alipay/notify")
    public ResponseEntity<Map<String, String>> alipayNotify(
            @RequestHeader HttpHeaders headers, @RequestBody String body) {
        paymentCallbackService.handle(2, headers.toSingleValueMap(), body);
        return ResponseEntity.ok(Collections.singletonMap("result", "success"));
    }

    @PostMapping("/wechat/refund-notify")
    public ResponseEntity<Map<String, String>> wechatRefundNotify(
            @RequestHeader HttpHeaders headers, @RequestBody String body) {
        try {
            paymentCallbackService.handleRefund(1, headers.toSingleValueMap(), body);
            return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "成功"));
        } catch (RuntimeException ex) {
            log.error("微信退款回调处理失败", ex);
            businessMetrics.paymentCallbackFailed(1);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", "FAIL", "message", "处理失败"));
        }
    }
}
