package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理员门店Audit请求")
@Data
public class AdminStoreAuditRequest {

    @NotNull(message = "审核状态不能为空")
    @Schema(description = "审核状态")
    private Integer auditStatus;

    @Schema(description = "备注")
    private String remark;
}
