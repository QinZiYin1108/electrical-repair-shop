package com.example.backend.service;

import com.example.backend.model.worker.WorkerLocationUpdateRequest;
import com.example.backend.model.worker.WorkerLocationUpdateResponse;

public interface WorkerLocationService {

    WorkerLocationUpdateResponse updateCurrentWorkerLocation(WorkerLocationUpdateRequest request);
}

