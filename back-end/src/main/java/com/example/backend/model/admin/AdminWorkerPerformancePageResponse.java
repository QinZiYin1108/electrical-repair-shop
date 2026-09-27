package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "管理员师傅绩效Page响应")
@Data
public class AdminWorkerPerformancePageResponse {

    @Schema(description = "页码Num")
    private Long pageNum;

    @Schema(description = "页码每页数量")
    private Long pageSize;

    @Schema(description = "总数")
    private Long total;

    private List<AdminWorkerPerformanceItemResponse> list = new ArrayList<>();

    @Schema(description = "summary")
    private AdminWorkerPerformanceSummaryResponse summary;
}
