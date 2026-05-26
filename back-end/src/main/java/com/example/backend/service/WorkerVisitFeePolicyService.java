package com.example.backend.service;

import com.example.backend.model.worker.WorkerVisitFeePoliciesUpdateRequest;
import com.example.backend.model.worker.WorkerVisitFeePolicyItem;

import java.util.List;

public interface WorkerVisitFeePolicyService {

    List<WorkerVisitFeePolicyItem> getCurrentWorkerPolicies();

    void updateCurrentWorkerPolicies(WorkerVisitFeePoliciesUpdateRequest request);
}
