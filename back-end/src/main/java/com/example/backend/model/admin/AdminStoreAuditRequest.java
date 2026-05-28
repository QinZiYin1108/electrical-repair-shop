package com.example.backend.model.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminStoreAuditRequest {

    @NotNull(message = "审核状态不能为空")
    private Integer auditStatus;

    @NotBlank(message = "审核备注不能为空")
    private String remark;
}
