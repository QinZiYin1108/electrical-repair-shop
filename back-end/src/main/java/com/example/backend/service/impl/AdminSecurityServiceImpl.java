package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.AdminAccounts;
import com.example.backend.entity.AdminProfiles;
import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.entity.UserAccounts;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.admin.AdminChangeEmailRequest;
import com.example.backend.model.admin.AdminChangePasswordRequest;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.AdminAccountsService;
import com.example.backend.service.AdminProfilesService;
import com.example.backend.service.AdminSecurityService;
import com.example.backend.service.AuthCodeService;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.service.UserAccountsService;
import com.example.backend.utils.PasswordUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminSecurityServiceImpl implements AdminSecurityService {

    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    private final AdminAccountsService adminAccountsService;
    private final AdminProfilesService adminProfilesService;
    private final UserAccountsService userAccountsService;
    private final TechnicianAccountsService technicianAccountsService;
    private final AuthCodeService authCodeService;

    public AdminSecurityServiceImpl(
        AdminAccountsService adminAccountsService,
        AdminProfilesService adminProfilesService,
        UserAccountsService userAccountsService,
        TechnicianAccountsService technicianAccountsService,
        AuthCodeService authCodeService
    ) {
        this.adminAccountsService = adminAccountsService;
        this.adminProfilesService = adminProfilesService;
        this.userAccountsService = userAccountsService;
        this.technicianAccountsService = technicianAccountsService;
        this.authCodeService = authCodeService;
    }

    @Override
    public void changePassword(AdminChangePasswordRequest request) {
        LoginUserInfo user = requireAdmin();
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数不能为空");
        }
        String oldPassword = safeTrim(request.getOldPassword());
        String newPassword = safeTrim(request.getNewPassword());
        String confirmPassword = safeTrim(request.getConfirmPassword());
        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword) || !StringUtils.hasText(confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码不能为空");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "两次输入的新密码不一致");
        }
        if (newPassword.length() < 6) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新密码至少6位");
        }
        if (newPassword.equals(oldPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新密码不能与旧密码相同");
        }

        AdminAccounts admin = getCurrentAdmin(user.getAccountId());
        String salt = admin.getSalt();
        String hash = admin.getPasswordHash();
        if (!StringUtils.hasText(salt) || !StringUtils.hasText(hash)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前账号未设置密码，请使用忘记密码重置");
        }
        String expectedHash = PasswordUtil.hashPassword(oldPassword, salt);
        if (!hash.equals(expectedHash)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "旧密码不正确");
        }
        admin.setPasswordHash(PasswordUtil.hashPassword(newPassword, salt));
        admin.setUpdatedTime(System.currentTimeMillis());
        adminAccountsService.updateById(admin);
    }

    @Override
    public void sendChangeEmailCode(String newEmail) {
        LoginUserInfo user = requireAdmin();
        String email = safeTrim(newEmail);
        if (!StringUtils.hasText(email)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新邮箱不能为空");
        }
        AdminAccounts admin = getCurrentAdmin(user.getAccountId());
        if (!email.matches(EMAIL_PATTERN)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "邮箱格式不正确");
        }
        if (email.equalsIgnoreCase(safeTrim(admin.getEmail()))) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新邮箱不能与当前邮箱相同");
        }
        ensureEmailAvailable(email, admin.getId());
        authCodeService.sendCode(email, "ADMIN_CHANGE_EMAIL");
    }

    @Override
    public void changeEmail(AdminChangeEmailRequest request) {
        LoginUserInfo user = requireAdmin();
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数不能为空");
        }
        String newEmail = safeTrim(request.getNewEmail());
        String code = safeTrim(request.getCode());
        if (!StringUtils.hasText(newEmail) || !StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新邮箱和验证码不能为空");
        }
        if (!newEmail.matches(EMAIL_PATTERN)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "邮箱格式不正确");
        }
        AdminAccounts admin = getCurrentAdmin(user.getAccountId());
        if (newEmail.equalsIgnoreCase(safeTrim(admin.getEmail()))) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新邮箱不能与当前邮箱相同");
        }
        ensureEmailAvailable(newEmail, admin.getId());
        authCodeService.verifyCode(newEmail, "ADMIN_CHANGE_EMAIL", code);

        long now = System.currentTimeMillis();
        admin.setEmail(newEmail);
        admin.setUpdatedTime(now);
        adminAccountsService.updateById(admin);

        AdminProfiles profile = adminProfilesService.getOne(
            new LambdaQueryWrapper<AdminProfiles>()
                .eq(AdminProfiles::getAccountId, admin.getId())
                .eq(AdminProfiles::getIsDelete, 0),
            false
        );
        if (profile != null) {
            profile.setEmail(newEmail);
            profile.setUpdatedTime(now);
            adminProfilesService.updateById(profile);
        }
    }

    private void ensureEmailAvailable(String email, String currentAdminId) {
        AdminAccounts adminExists = adminAccountsService.getOne(
            new LambdaQueryWrapper<AdminAccounts>()
                .eq(AdminAccounts::getEmail, email)
                .eq(AdminAccounts::getIsDelete, 0),
            false
        );
        if (adminExists != null && !adminExists.getId().equals(currentAdminId)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该邮箱已被注册");
        }

        UserAccounts userExists = userAccountsService.getOne(
            new LambdaQueryWrapper<UserAccounts>()
                .eq(UserAccounts::getEmail, email)
                .eq(UserAccounts::getIsDelete, 0),
            false
        );
        if (userExists != null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该邮箱已被注册");
        }

        TechnicianAccounts technicianExists = technicianAccountsService.getOne(
            new LambdaQueryWrapper<TechnicianAccounts>()
                .eq(TechnicianAccounts::getEmail, email)
                .eq(TechnicianAccounts::getIsDelete, 0),
            false
        );
        if (technicianExists != null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该邮箱已被注册");
        }
    }

    private AdminAccounts getCurrentAdmin(String accountId) {
        AdminAccounts admin = adminAccountsService.getById(accountId);
        if (admin == null || (admin.getIsDelete() != null && admin.getIsDelete() != 0)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "管理员账号不存在");
        }
        return admin;
    }

    private LoginUserInfo requireAdmin() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问管理员接口");
        }
        if (!StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return user;
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }
}
