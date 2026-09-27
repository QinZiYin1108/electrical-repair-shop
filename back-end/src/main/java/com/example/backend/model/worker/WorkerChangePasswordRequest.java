package com.example.backend.model.worker;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "师傅修改密码请求")
@Data
public class WorkerChangePasswordRequest {

    @NotBlank(message = "旧密码不能为空")
    @Schema(description = "old密码")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "新密码长度需为6-64位")
    @Schema(description = "是否新品密码")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    @Size(min = 6, max = 64, message = "确认密码长度需为6-64位")
    @Schema(description = "确认密码")
    private String confirmPassword;
}
