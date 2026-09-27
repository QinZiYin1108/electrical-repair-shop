package com.example.backend.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "用户订单故障Item响应")
@Data
public class UserOrderFaultItemResponse {

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

    private List<UserOrderMediaItemResponse> images = new ArrayList<>();
    private List<UserOrderMediaItemResponse> videos = new ArrayList<>();
}
