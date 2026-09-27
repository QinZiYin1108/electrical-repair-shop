package com.example.backend.service;

public interface PenaltyAccountEnforcementService {

    Integer freezeAccount(String accountId, int accountType);

    void restoreAccount(String accountId, int accountType, Integer previousStatus);
}
