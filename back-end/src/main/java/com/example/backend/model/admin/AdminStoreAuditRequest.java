package com.example.backend.model.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminStoreAuditRequest {

    @NotNull(message = "审核状态不能为空")
    private Integer auditStatus;

    private String remark;
}
