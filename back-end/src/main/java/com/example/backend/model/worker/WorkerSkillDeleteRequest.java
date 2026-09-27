package com.example.backend.model.worker;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "师傅技能Delete请求")
@Data
public class WorkerSkillDeleteRequest {

    @Schema(description = "服务类型ID")
    private String serviceTypeId;
}
