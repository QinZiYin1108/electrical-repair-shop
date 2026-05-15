package com.example.backend.controller.worker;

import com.example.backend.common.Result;
import com.example.backend.model.worker.WorkerWorkTimeItem;
import com.example.backend.model.worker.WorkerWorkTimesUpdateRequest;
import com.example.backend.service.WorkerWorkTimeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/worker/work-times")
public class WorkerWorkTimeController {

    private final WorkerWorkTimeService workerWorkTimeService;

    public WorkerWorkTimeController(WorkerWorkTimeService workerWorkTimeService) {
        this.workerWorkTimeService = workerWorkTimeService;
    }

    @GetMapping
    public Result<List<WorkerWorkTimeItem>> listCurrentWorkerWorkTimes() {
        return Result.success(workerWorkTimeService.getCurrentWorkerWorkTimes());
    }

    @PostMapping
    public Result<Void> updateCurrentWorkerWorkTimes(@RequestBody WorkerWorkTimesUpdateRequest request) {
        workerWorkTimeService.updateCurrentWorkerWorkTimes(request);
        return Result.success();
    }
}
