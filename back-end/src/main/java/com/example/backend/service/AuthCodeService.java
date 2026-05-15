package com.example.backend.service;

public interface AuthCodeService {

    void sendCode(String email, String type);

    void verifyCode(String email, String type, String code);
}
