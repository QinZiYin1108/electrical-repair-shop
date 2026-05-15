package com.example.backend.model.worker;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class WorkerOrderInspectionSubmitRequest {

    private String inspectionDiagnosis;
    private String repairPlan;
    private BigDecimal serviceFee;
    private BigDecimal materialFee;
    private List<WorkerOrderSubmitMediaItem> images = new ArrayList<>();
    private WorkerOrderSubmitMediaItem video;
}
