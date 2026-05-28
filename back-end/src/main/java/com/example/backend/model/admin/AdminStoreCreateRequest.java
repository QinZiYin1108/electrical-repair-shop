package com.example.backend.model.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminStoreCreateRequest {

    @NotBlank(message = "门店名称不能为空")
    private String name;

    private String logoImageId;

    @NotBlank(message = "联系电话不能为空")
    private String contactPhone;

    private String address;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private String description;

    private String businessLicense;

    // ===== 门店管理员账号信息（超级管理员创建门店时必填） =====

    @NotBlank(message = "门店管理员姓名不能为空")
    private String adminName;

    @NotBlank(message = "门店管理员手机号不能为空")
    private String adminPhone;

    @NotBlank(message = "门店管理员邮箱不能为空")
    private String adminEmail;

    @NotBlank(message = "门店管理员登录密码不能为空")
    private String adminPassword;
}
