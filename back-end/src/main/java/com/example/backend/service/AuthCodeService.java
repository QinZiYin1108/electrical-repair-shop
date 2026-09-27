package com.example.backend.service;

public interface AuthCodeService {

    void sendCode(String phone, String type);

    void verifyCode(String phone, String type, String code);
}
