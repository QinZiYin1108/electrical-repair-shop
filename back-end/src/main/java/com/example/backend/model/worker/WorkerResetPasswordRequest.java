package com.example.backend.model.worker;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "师傅重置密码请求")
@Data
public class WorkerResetPasswordRequest {

    @Schema(description = "编码")
    private String code;

    @Schema(description = "是否新品密码")
    private String newPassword;

    @Schema(description = "确认密码")
    private String confirmPassword;
}
