package com.example.backend.model.admin;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminWorkerPerformancePageResponse {

    private Long pageNum;
    private Long pageSize;
    private Long total;
    private List<AdminWorkerPerformanceItemResponse> list = new ArrayList<>();
    private AdminWorkerPerformanceSummaryResponse summary;
}
