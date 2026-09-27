package com.example.backend.model.appointment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "预约停业/请假时段创建请求")
@Data
public class AppointmentClosureCreateRequest {

    @Schema(description = "范围：GLOBAL-全平台，TECHNICIAN-师傅，STORE-门店")
    private String ownerType;

    @Schema(description = "范围对象ID（GLOBAL 可为空）")
    private String ownerId;

    @Schema(description = "开始时间戳")
    private Long startTime;

    @Schema(description = "结束时间戳")
    private Long endTime;

    @Schema(description = "原因")
    private String reason;
}
