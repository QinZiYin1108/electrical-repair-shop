package com.example.backend.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户登录响应")
@Data
public class UserLoginResponse {

    @Schema(description = "令牌")
    private String token;

    @Schema(description = "已绑定手机号")
    private boolean phoneBound;

    @Schema(description = "已绑定微信")
    private boolean wechatBound;

    @Schema(description = "已设置密码")
    private boolean passwordSet;

    @Schema(description = "是否新品账号创建")
    private boolean newAccountCreated;

    @Schema(description = "需要确认注销")
    private boolean needCancelConfirm;

    @Schema(description = "取消申请时间")
    private Long cancelApplyTime;

    @Schema(description = "取消截止时间")
    private Long cancelDeadlineTime;

    @Schema(description = "已撤销注销")
    private boolean cancelRevoked;

    @Schema(description = "需要绑定手机号（强制）")
    private boolean needBindPhone;
}
