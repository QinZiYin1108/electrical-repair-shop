package com.example.backend.model.worker;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "师傅上门费用PoliciesUpdate请求")
@Data
public class WorkerVisitFeePoliciesUpdateRequest {

    @Schema(description = "policies")
    private List<WorkerVisitFeePolicyItem> policies;
}
