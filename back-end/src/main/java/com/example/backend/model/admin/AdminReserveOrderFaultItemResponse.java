package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "管理员预约订单故障Item响应")
@Data
public class AdminReserveOrderFaultItemResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "故障现象ID")
    private String faultPhenomenonId;

    @Schema(description = "故障现象名称")
    private String faultPhenomenonName;

    @Schema(description = "故障现象Description")
    private String faultPhenomenonDescription;

    @Schema(description = "故障Description")
    private String faultDescription;

    private List<AdminReserveOrderMediaItemResponse> images = new ArrayList<>();
    private List<AdminReserveOrderMediaItemResponse> videos = new ArrayList<>();
}
