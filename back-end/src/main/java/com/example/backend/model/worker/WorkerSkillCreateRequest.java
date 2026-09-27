package com.example.backend.model.worker;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "师傅技能Create请求")
@Data
public class WorkerSkillCreateRequest {

    @Schema(description = "服务类型ID")
    private String serviceTypeId;
}
