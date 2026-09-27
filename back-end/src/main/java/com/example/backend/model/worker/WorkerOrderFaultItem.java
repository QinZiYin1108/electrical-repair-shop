package com.example.backend.model.worker;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "师傅订单故障列表项")
@Data
public class WorkerOrderFaultItem {

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

    @Schema(description = "images")
    private List<WorkerOrderMediaItem> images;

    @Schema(description = "videos")
    private List<WorkerOrderMediaItem> videos;
}
