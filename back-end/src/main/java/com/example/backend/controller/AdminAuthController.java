package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.backend.common.Result;
import com.example.backend.entity.AdminAccounts;
import com.example.backend.exception.BusinessException;
import com.example.backend.common.ErrorCode;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.token.JwtTokenService;
import com.example.backend.service.AdminAccountsService;
import com.example.backend.service.AuthCodeService;
import com.example.backend.model.auth.AdminLoginByPasswordRequest;
import com.example.backend.model.auth.AdminSendCodeRequest;
import com.example.backend.model.auth.AdminLoginByCodeRequest;
import com.example.backend.model.auth.AdminResetPasswordRequest;
import com.example.backend.utils.PasswordUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/pass/auth/admin")
public class AdminAuthController {

    private final AdminAccountsService adminAccountsService;
    private final JwtTokenService jwtTokenService;
    private final AuthCodeService authCodeService;

    public AdminAuthController(
        AdminAccountsService adminAccountsService,
        JwtTokenService jwtTokenService,
        AuthCodeService authCodeService
    ) {
        this.adminAccountsService = adminAccountsService;
        this.jwtTokenService = jwtTokenService;
        this.authCodeService = authCodeService;
    }

    @PostMapping("/login/password")
    public Result<Map<String, Object>> loginByPassword(@Valid @RequestBody AdminLoginByPasswordRequest request) {
        AdminAccounts admin = adminAccountsService.getOne(
            new LambdaQueryWrapper<AdminAccounts>()
                .eq(AdminAccounts::getEmail, request.getEmail())
                .eq(AdminAccounts::getIsDelete, 0)
            , false
        );
        if (admin == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "邮箱或者密码有误");
        }
        String expectedHash = PasswordUtil.hashPassword(request.getPassword(), admin.getSalt());
        if (!expectedHash.equals(admin.getPasswordHash())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "邮箱或者密码有误");
        }
        String token = jwtTokenService.generateToken(admin.getId(), AccountRole.ADMIN);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        return Result.success(data);
    }

    @PostMapping("/code/send")
    public Result<Void> sendCode(@Valid @RequestBody AdminSendCodeRequest request) {
        authCodeService.sendCode(request.getEmail(), request.getType());
        return Result.success();
    }

    @PostMapping("/login/code")
    public Result<Map<String, Object>> loginByCode(@Valid @RequestBody AdminLoginByCodeRequest request) {
        authCodeService.verifyCode(request.getEmail(), "ADMIN_LOGIN", request.getCode());
        AdminAccounts admin = adminAccountsService.getOne(
            new LambdaQueryWrapper<AdminAccounts>()
                .eq(AdminAccounts::getEmail, request.getEmail())
                .eq(AdminAccounts::getIsDelete, 0)
            , false
        );
        if (admin == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "邮箱或者验证码有误");
        }
        String token = jwtTokenService.generateToken(admin.getId(), AccountRole.ADMIN);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        return Result.success(data);
    }

    @PostMapping("/password/reset")
    public Result<Void> resetPassword(@Valid @RequestBody AdminResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "两次输入的密码不一致");
        }
        authCodeService.verifyCode(request.getEmail(), "ADMIN_RESET_PASSWORD", request.getCode());
        AdminAccounts admin = adminAccountsService.getOne(
            new LambdaQueryWrapper<AdminAccounts>()
                .eq(AdminAccounts::getEmail, request.getEmail())
                .eq(AdminAccounts::getIsDelete, 0),
            false
        );
        if (admin == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "邮箱不存在");
        }
        String salt = PasswordUtil.generateSalt(16);
        String passwordHash = PasswordUtil.hashPassword(request.getNewPassword(), salt);
        UpdateWrapper<AdminAccounts> wrapper = new UpdateWrapper<>();
        Integer currentVersion = admin.getVersion();
        wrapper.eq("id", admin.getId());
        if (currentVersion != null) {
            wrapper.eq("version", currentVersion);
        }
        AdminAccounts updateEntity = new AdminAccounts();
        updateEntity.setPasswordHash(passwordHash);
        updateEntity.setSalt(salt);
        updateEntity.setUpdatedTime(System.currentTimeMillis());
        if (currentVersion != null) {
            updateEntity.setVersion(currentVersion);
        }
        adminAccountsService.update(updateEntity, wrapper);
        return Result.success();
    }
}
