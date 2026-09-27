package com.example.backend.model.worker;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "师傅上门二维码核销Result")
@Data
public class WorkerDoorQrConsumeResult {

    @Schema(description = "排序ID")
    private String orderId;

    /** Service mode: 1 onsite repair, 2 onsite install */
    @Schema(description = "服务Mode")
    private Integer serviceMode;

    @Schema(description = "from状态")
    private Integer fromStatus;

    @Schema(description = "目标状态")
    private Integer targetStatus;
}
