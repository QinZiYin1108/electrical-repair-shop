package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理员门店Create请求")
@Data
public class AdminStoreCreateRequest {

    @NotBlank(message = "门店名称不能为空")
    @Schema(description = "名称")
    private String name;

    @Schema(description = "Logo图片ID")
    private String logoImageId;

    @NotBlank(message = "联系电话不能为空")
    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "description")
    private String description;

    @Schema(description = "营业执照")
    private String businessLicense;

    // ===== 门店管理员账号信息（超级管理员创建门店时必填） =====

    @NotBlank(message = "门店管理员姓名不能为空")
    @Schema(description = "管理员名称")
    private String adminName;

    @NotBlank(message = "门店管理员手机号不能为空")
    @Schema(description = "管理员手机")
    private String adminPhone;

    @NotBlank(message = "门店管理员邮箱不能为空")
    @Schema(description = "管理员邮箱")
    private String adminEmail;

    @NotBlank(message = "门店管理员登录密码不能为空")
    @Schema(description = "管理员密码")
    private String adminPassword;
}
