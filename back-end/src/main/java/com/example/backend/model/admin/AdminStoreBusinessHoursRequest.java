package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.List;
import lombok.Data;

@Schema(description = "管理员门店BusinessHours请求")
@Data
public class AdminStoreBusinessHoursRequest {

    @Valid
    @Schema(description = "hours")
    private List<AdminStoreBusinessHourItem> hours;
}
