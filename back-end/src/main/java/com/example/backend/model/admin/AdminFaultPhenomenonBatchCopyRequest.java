package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "管理员故障现象批量复制请求")
public class AdminFaultPhenomenonBatchCopyRequest {

    @NotEmpty(message = "sourceIds is required")
    @Schema(description = "来源Ids")
    private List<String> sourceIds;

    @NotBlank(message = "targetServiceTypeId is required")
    @Schema(description = "目标服务类型ID")
    private String targetServiceTypeId;

    public List<String> getSourceIds() {
        return sourceIds;
    }

    public void setSourceIds(List<String> sourceIds) {
        this.sourceIds = sourceIds;
    }

    public String getTargetServiceTypeId() {
        return targetServiceTypeId;
    }

    public void setTargetServiceTypeId(String targetServiceTypeId) {
        this.targetServiceTypeId = targetServiceTypeId;
    }
}
