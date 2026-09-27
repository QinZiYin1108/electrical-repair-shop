package com.example.backend.service;

import com.example.backend.payment.VerifiedPaymentCallback;

public interface PaymentPostingService {
    void postVerifiedPayment(int provider, VerifiedPaymentCallback callback);
}
