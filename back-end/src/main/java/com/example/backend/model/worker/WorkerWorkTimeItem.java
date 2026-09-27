package com.example.backend.model.worker;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "师傅工作时间列表项")
@Data
public class WorkerWorkTimeItem {

    @Schema(description = "ID")
    private String id;

    /** 1-7 (周一到周日) */
    @Schema(description = "星期")
    private Integer dayOfWeek;

    /** HH:mm:ss */
    @Schema(description = "开始时间")
    private String startTime;

    /** HH:mm:ss */
    @Schema(description = "结束时间")
    private String endTime;

    /** 0-不可接单，1-可接单 */
    @Schema(description = "是否可用")
    private Integer isAvailable;
}
