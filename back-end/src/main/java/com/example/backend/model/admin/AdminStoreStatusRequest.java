package com.example.backend.model.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminStoreStatusRequest {

    @NotNull(message = "营业状态不能为空")
    private Integer businessStatus;
}
