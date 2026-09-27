package com.example.backend.model.worker;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 师傅端账号注销状态 */
@Schema(description = "师傅账号注销状态响应")
@Data
public class WorkerAccountCancelStatusResponse {

    /** 是否处于注销申请（反悔期）中 */
    @Schema(description = "canceling")
    private Boolean canceling;

    /** 注销申请时间戳 */
    @Schema(description = "取消申请时间")
    private Long cancelApplyTime;

    /** 注销生效时间戳（反悔期截止） */
    @Schema(description = "取消截止时间")
    private Long cancelDeadlineTime;

    /** 是否可申请注销 */
    @Schema(description = "can申请")
    private Boolean canApply;

    /** 是否可撤销注销 */
    @Schema(description = "canRevoke")
    private Boolean canRevoke;

    /** 提示文案 */
    @Schema(description = "tip")
    private String tip;
}
