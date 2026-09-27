package com.example.backend.model.worker;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "师傅技能批量Create请求")
@Data
public class WorkerSkillBatchCreateRequest {

    private List<String> serviceTypeIds = new ArrayList<>();
}
