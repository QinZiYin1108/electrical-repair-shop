package com.example.backend.model.worker;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "师傅订单费用Update请求")
@Data
public class WorkerOrderFeeUpdateRequest {

    @Schema(description = "服务费用")
    private BigDecimal serviceFee;

    @Schema(description = "material费用")
    private BigDecimal materialFee;
}
