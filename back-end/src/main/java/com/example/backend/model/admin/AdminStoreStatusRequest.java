package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理员门店状态请求")
@Data
public class AdminStoreStatusRequest {

    @NotNull(message = "营业状态不能为空")
    @Schema(description = "营业状态")
    private Integer businessStatus;
}
