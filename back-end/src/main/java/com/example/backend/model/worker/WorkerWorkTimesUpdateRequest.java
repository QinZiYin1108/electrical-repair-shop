package com.example.backend.model.worker;

import lombok.Data;

import java.util.List;

@Data
public class WorkerWorkTimesUpdateRequest {

    private List<WorkerWorkTimeItem> workTimes;
}
