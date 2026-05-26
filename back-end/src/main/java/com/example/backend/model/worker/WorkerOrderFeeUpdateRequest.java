package com.example.backend.model.worker;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WorkerOrderFeeUpdateRequest {

    private BigDecimal serviceFee;
    private BigDecimal materialFee;
}
