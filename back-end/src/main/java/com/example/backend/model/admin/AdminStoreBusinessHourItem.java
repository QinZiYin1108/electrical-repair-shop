package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理员门店BusinessHour列表项")
@Data
public class AdminStoreBusinessHourItem {

    @Schema(description = "星期")
    private Integer dayOfWeek;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;

    @Schema(description = "是否可用")
    private Integer isAvailable;
}
