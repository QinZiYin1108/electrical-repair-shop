package com.example.backend.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户账号注销Apply请求")
@Data
public class UserAccountCancelApplyRequest {

    /** 注销原因（可选） */
    @Schema(description = "原因")
    private String reason;
}
