package com.example.backend.model.payment;

import java.util.Collections;
import java.util.Map;
import lombok.Data;

@Data
public class PaymentIntentResponse {
    private String paymentNo;
    private String orderId;
    private Integer orderType;
    private Integer provider;
    private Integer status;
    private String statusText;
    private String amount;
    private String currency;
    private Long expiresAt;
    private Map<String, String> invokeParameters = Collections.emptyMap();
}
