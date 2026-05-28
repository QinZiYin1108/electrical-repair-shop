package com.example.backend.service;

import com.example.backend.model.worker.WorkerLocationUpdateRequest;
import com.example.backend.model.worker.WorkerLocationUpdateResponse;
import com.example.backend.model.worker.WorkerStoreAddressRequest;

public interface WorkerLocationService {

    WorkerLocationUpdateResponse updateCurrentWorkerLocation(WorkerLocationUpdateRequest request);

    WorkerLocationUpdateResponse updateStoreAddress(WorkerStoreAddressRequest request);
}

