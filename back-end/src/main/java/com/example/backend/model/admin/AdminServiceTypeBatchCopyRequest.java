package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理员服务类型批量复制请求")
public class AdminServiceTypeBatchCopyRequest {

    @NotEmpty(message = "sourceIds is required")
    @Schema(description = "来源Ids")
    private List<String> sourceIds;

    @NotNull(message = "targetType is required")
    @Schema(description = "目标类型")
    private Integer targetType;

    public List<String> getSourceIds() {
        return sourceIds;
    }

    public void setSourceIds(List<String> sourceIds) {
        this.sourceIds = sourceIds;
    }

    public Integer getTargetType() {
        return targetType;
    }

    public void setTargetType(Integer targetType) {
        this.targetType = targetType;
    }
}
