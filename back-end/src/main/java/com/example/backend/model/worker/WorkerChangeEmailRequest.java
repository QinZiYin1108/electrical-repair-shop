package com.example.backend.model.worker;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "师傅修改邮件请求")
@Data
public class WorkerChangeEmailRequest {

    @NotBlank(message = "新邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "是否新品邮箱")
    private String newEmail;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "编码")
    private String code;
}
