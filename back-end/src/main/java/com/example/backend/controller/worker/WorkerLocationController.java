package com.example.backend.controller.worker;

import com.example.backend.common.Result;
import com.example.backend.model.worker.WorkerLocationUpdateRequest;
import com.example.backend.model.worker.WorkerLocationUpdateResponse;
import com.example.backend.service.WorkerLocationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/worker/location")
public class WorkerLocationController {

    private final WorkerLocationService workerLocationService;

    public WorkerLocationController(WorkerLocationService workerLocationService) {
        this.workerLocationService = workerLocationService;
    }

    @PostMapping("/update")
    public Result<WorkerLocationUpdateResponse> update(@RequestBody WorkerLocationUpdateRequest request) {
        return Result.success(workerLocationService.updateCurrentWorkerLocation(request));
    }
}

