package com.example.backend.service;

import com.example.backend.model.worker.WorkerWorkTimeItem;
import com.example.backend.model.worker.WorkerWorkTimesUpdateRequest;

import java.util.List;

public interface WorkerWorkTimeService {

    List<WorkerWorkTimeItem> getCurrentWorkerWorkTimes();

    void updateCurrentWorkerWorkTimes(WorkerWorkTimesUpdateRequest request);
}
