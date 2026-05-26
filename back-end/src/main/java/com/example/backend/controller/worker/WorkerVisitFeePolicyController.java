package com.example.backend.controller.worker;

import com.example.backend.common.Result;
import com.example.backend.model.worker.WorkerVisitFeePoliciesUpdateRequest;
import com.example.backend.model.worker.WorkerVisitFeePolicyItem;
import com.example.backend.service.WorkerVisitFeePolicyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/worker/visit-fee-policies")
public class WorkerVisitFeePolicyController {

    private final WorkerVisitFeePolicyService workerVisitFeePolicyService;

    public WorkerVisitFeePolicyController(WorkerVisitFeePolicyService workerVisitFeePolicyService) {
        this.workerVisitFeePolicyService = workerVisitFeePolicyService;
    }

    @GetMapping
    public Result<List<WorkerVisitFeePolicyItem>> listCurrentWorkerPolicies() {
        return Result.success(workerVisitFeePolicyService.getCurrentWorkerPolicies());
    }

    @PostMapping
    public Result<Void> updateCurrentWorkerPolicies(@RequestBody WorkerVisitFeePoliciesUpdateRequest request) {
        workerVisitFeePolicyService.updateCurrentWorkerPolicies(request);
        return Result.success();
    }
}
