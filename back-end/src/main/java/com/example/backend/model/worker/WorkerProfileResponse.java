package com.example.backend.model.worker;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "师傅个人资料响应")
@Data
public class WorkerProfileResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "username")
    private String username;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "账号状态")
    private Integer accountStatus;

    @Schema(description = "工作状态")
    private Integer workStatus;

    @Schema(description = "评分")
    private BigDecimal rating;

    @Schema(description = "订单数量")
    private Integer orderCount;

    @Schema(description = "完成率")
    private BigDecimal completionRate;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "IDCard")
    private String idCard;

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
