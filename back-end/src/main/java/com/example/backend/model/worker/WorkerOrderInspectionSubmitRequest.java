package com.example.backend.model.worker;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "师傅订单验机提交请求")
@Data
public class WorkerOrderInspectionSubmitRequest {

    @Schema(description = "inspectionDiagnosis")
    private String inspectionDiagnosis;

    @Schema(description = "维修Plan")
    private String repairPlan;

    @Schema(description = "服务费用")
    private BigDecimal serviceFee;

    @Schema(description = "material费用")
    private BigDecimal materialFee;

    private List<WorkerOrderSubmitMediaItem> images = new ArrayList<>();

    @Schema(description = "视频")
    private WorkerOrderSubmitMediaItem video;
}
