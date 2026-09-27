package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "管理员师傅技能批量Update请求")
public class AdminWorkerSkillBatchUpdateRequest {

    @NotEmpty(message = "serviceTypeIds 不能为空")
    @Schema(description = "服务类型Ids")
    private List<String> serviceTypeIds;

    public List<String> getServiceTypeIds() {
        return serviceTypeIds;
    }

    public void setServiceTypeIds(List<String> serviceTypeIds) {
        this.serviceTypeIds = serviceTypeIds;
    }
}
