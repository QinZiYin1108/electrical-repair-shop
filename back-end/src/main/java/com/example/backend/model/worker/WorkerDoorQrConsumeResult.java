package com.example.backend.model.worker;

import lombok.Data;

@Data
public class WorkerDoorQrConsumeResult {

    private String orderId;

    /**
     * Service mode: 1 onsite repair, 2 onsite install
     */
    private Integer serviceMode;

    private Integer fromStatus;

    private Integer targetStatus;
}

