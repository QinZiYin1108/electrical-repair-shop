package com.example.backend.service;

import java.util.Map;

public interface PaymentCallbackService {
    void handle(int provider, Map<String, String> headers, String body);

    void handleRefund(int provider, Map<String, String> headers, String body);
}
