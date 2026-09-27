package com.example.backend.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户账号注销状态响应")
@Data
public class UserAccountCancelStatusResponse {

    /** 账号状态：1-正常，2-冻结，3-注销申请中，4-已注销 */
    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "取消申请时间")
    private Long cancelApplyTime;

    @Schema(description = "取消截止时间")
    private Long cancelDeadlineTime;

    /** 说明文案 */
    @Schema(description = "tip")
    private String tip;

    /** 是否可以申请注销 */
    @Schema(description = "can申请")
    private Boolean canApply;

    /** 是否可以撤销注销申请 */
    @Schema(description = "canRevoke")
    private Boolean canRevoke;
}
