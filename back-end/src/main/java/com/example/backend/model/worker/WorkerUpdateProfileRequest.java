package com.example.backend.model.worker;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "师傅Update个人资料请求")
@Data
public class WorkerUpdateProfileRequest {

    @Schema(description = "username")
    private String username;

    @Schema(description = "性别")
    private Integer gender;

    @Schema(description = "生日")
    private Long birthday;

    @Schema(description = "workYears")
    private Integer workYears;

    @Schema(description = "education")
    private String education;

    @Schema(description = "introduction")
    private String introduction;

    @Schema(description = "响应时间")
    private Integer responseTime;
}
