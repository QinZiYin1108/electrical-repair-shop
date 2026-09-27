package com.example.backend.model.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 师傅端登录响应 */
@Schema(description = "师傅登录响应")
@Data
public class WorkerLoginResponse {

    @Schema(description = "令牌")
    private String token;

    /** 是否需要注销确认（账号处于反悔期，继续登录将撤销注销） */
    @Schema(description = "需要确认注销")
    private Boolean needCancelConfirm;

    @Schema(description = "取消申请时间")
    private Long cancelApplyTime;

    @Schema(description = "取消截止时间")
    private Long cancelDeadlineTime;

    /** 本次登录是否已撤销注销 */
    @Schema(description = "已撤销注销")
    private Boolean cancelRevoked;
}
