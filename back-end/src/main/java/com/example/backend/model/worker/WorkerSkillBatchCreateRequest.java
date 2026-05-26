package com.example.backend.model.worker;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WorkerSkillBatchCreateRequest {

    private List<String> serviceTypeIds = new ArrayList<>();
}
