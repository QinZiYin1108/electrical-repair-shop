package com.example.backend.model.worker;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "师傅工作TimesUpdate请求")
@Data
public class WorkerWorkTimesUpdateRequest {

    @Schema(description = "workTimes")
    private List<WorkerWorkTimeItem> workTimes;
}
