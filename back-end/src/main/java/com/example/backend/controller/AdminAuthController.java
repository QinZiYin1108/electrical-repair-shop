package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.AdminAccounts;
import com.example.backend.entity.Stores;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.auth.AdminLoginByCodeRequest;
import com.example.backend.model.auth.AdminLoginByPasswordRequest;
import com.example.backend.model.auth.AdminResetPasswordRequest;
import com.example.backend.model.auth.AdminSendCodeRequest;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.token.JwtTokenService;
import com.example.backend.security.token.TokenVersions;
import com.example.backend.service.AdminAccountsService;
import com.example.backend.service.AuthCodeService;
import com.example.backend.service.StoresService;
import com.example.backend.utils.PasswordUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "公开/登录认证/管理员端", description = "管理员验证码登录、密码登录、忘记密码")
@RestController
@RequestMapping("/pass/auth/admin")
public class AdminAuthController {

    private final AdminAccountsService adminAccountsService;
    private final JwtTokenService jwtTokenService;
    private final AuthCodeService authCodeService;
    private final StoresService storesService;

    public AdminAuthController(
            AdminAccountsService adminAccountsService,
            JwtTokenService jwtTokenService,
            AuthCodeService authCodeService,
            StoresService storesService) {
        this.adminAccountsService = adminAccountsService;
        this.jwtTokenService = jwtTokenService;
        this.authCodeService = authCodeService;
        this.storesService = storesService;
    }

    @Operation(summary = "密码登录")
    @PostMapping("/login/password")
    public Result<Map<String, Object>> loginByPassword(
            @Parameter(description = "手机号+密码登录请求", required = true) @Valid @RequestBody
                    AdminLoginByPasswordRequest request) {
        AdminAccounts admin =
                adminAccountsService.getOne(
                        new LambdaQueryWrapper<AdminAccounts>()
                                .eq(AdminAccounts::getPhone, request.getPhone())
                                .eq(AdminAccounts::getIsDelete, 0),
                        false);
        if (admin == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "手机号或者密码有误");
        }
        requireActiveAdmin(admin);
        if (!PasswordUtil.matches(
                request.getPassword(), admin.getPasswordHash(), admin.getSalt())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "手机号或者密码有误");
        }
        if (PasswordUtil.needsRehash(admin.getPasswordHash())) {
            admin.setPasswordHash(
                    PasswordUtil.hashPassword(request.getPassword(), admin.getSalt()));
            admin.setUpdatedTime(System.currentTimeMillis());
            adminAccountsService.updateById(admin);
        }
        String token =
                jwtTokenService.generateToken(
                        admin.getId(), AccountRole.ADMIN, buildAdminExtraClaims(admin));
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("adminRole", admin.getAdminRole());
        return Result.success(data);
    }

    @Operation(summary = "发送登录/重置密码验证码")
    @PostMapping("/code/send")
    public Result<Void> sendCode(
            @Parameter(
                            description = "手机号 + 验证码类型(type=ADMIN_LOGIN 或 ADMIN_RESET_PASSWORD)",
                            required = true)
                    @Valid
                    @RequestBody
                    AdminSendCodeRequest request) {
        AdminAccounts admin =
                adminAccountsService.getOne(
                        new LambdaQueryWrapper<AdminAccounts>()
                                .eq(AdminAccounts::getPhone, request.getPhone())
                                .eq(AdminAccounts::getIsDelete, 0),
                        false);
        if (admin == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该手机号未注册");
        }
        authCodeService.sendCode(request.getPhone(), request.getType());
        return Result.success();
    }

    @Operation(summary = "验证码登录")
    @PostMapping("/login/code")
    public Result<Map<String, Object>> loginByCode(
            @Parameter(description = "手机号+验证码登录请求", required = true) @Valid @RequestBody
                    AdminLoginByCodeRequest request) {
        authCodeService.verifyCode(request.getPhone(), "ADMIN_LOGIN", request.getCode());
        AdminAccounts admin =
                adminAccountsService.getOne(
                        new LambdaQueryWrapper<AdminAccounts>()
                                .eq(AdminAccounts::getPhone, request.getPhone())
                                .eq(AdminAccounts::getIsDelete, 0),
                        false);
        if (admin == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "手机号或者验证码有误");
        }
        requireActiveAdmin(admin);
        String token =
                jwtTokenService.generateToken(
                        admin.getId(), AccountRole.ADMIN, buildAdminExtraClaims(admin));
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("adminRole", admin.getAdminRole());
        return Result.success(data);
    }

    @Operation(summary = "重置密码（需验证码 + 邮箱）")
    @PostMapping("/password/reset")
    public Result<Void> resetPassword(
            @Parameter(description = "手机号+验证码+新密码", required = true) @Valid @RequestBody
                    AdminResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "两次输入的密码不一致");
        }
        authCodeService.verifyCode(request.getPhone(), "ADMIN_RESET_PASSWORD", request.getCode());
        AdminAccounts admin =
                adminAccountsService.getOne(
                        new LambdaQueryWrapper<AdminAccounts>()
                                .eq(AdminAccounts::getPhone, request.getPhone())
                                .eq(AdminAccounts::getIsDelete, 0),
                        false);
        if (admin == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "手机号不存在");
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
        updateEntity.setTokenVersion(TokenVersions.next(admin.getTokenVersion()));
        updateEntity.setUpdatedTime(System.currentTimeMillis());
        if (currentVersion != null) {
            updateEntity.setVersion(currentVersion);
        }
        adminAccountsService.update(updateEntity, wrapper);
        return Result.success();
    }

    /** 构建管理员 JWT 额外 claims（adminRole、storeId） */
    private Map<String, Object> buildAdminExtraClaims(AdminAccounts admin) {
        Map<String, Object> claims = new HashMap<>();
        Integer adminRole = admin.getAdminRole();
        claims.put("adminRole", adminRole != null ? adminRole : 1);
        claims.put(
                "tokenVersion",
                admin.getTokenVersion() == null || admin.getTokenVersion() < 1
                        ? 1
                        : admin.getTokenVersion());

        // 门店管理员：查询归属门店ID
        if (adminRole != null && adminRole == 2) {
            Stores store =
                    storesService.getOne(
                            new LambdaQueryWrapper<Stores>()
                                    .eq(Stores::getStoreAdminId, admin.getId())
                                    .eq(Stores::getIsDelete, 0));
            if (store != null) {
                claims.put("storeId", store.getId());
            }
        }
        return claims;
    }

    private void requireActiveAdmin(AdminAccounts admin) {
        if (admin == null || !Integer.valueOf(1).equals(admin.getAccountStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "管理员账号已冻结");
        }
    }
}
