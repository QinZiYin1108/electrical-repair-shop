package com.example.backend.service;

import com.example.backend.model.payment.CreatePaymentIntentRequest;
import com.example.backend.model.payment.PaymentIntentResponse;

public interface PaymentApplicationService {
    PaymentIntentResponse createIntent(String accountId, CreatePaymentIntentRequest request);

    PaymentIntentResponse getIntent(String accountId, String paymentNo);
}
