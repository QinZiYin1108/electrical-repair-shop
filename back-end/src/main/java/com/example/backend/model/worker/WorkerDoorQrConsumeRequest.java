package com.example.backend.model.worker;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "师傅上门二维码核销请求")
@Data
public class WorkerDoorQrConsumeRequest {

    @Schema(description = "令牌")
    private String token;
}
