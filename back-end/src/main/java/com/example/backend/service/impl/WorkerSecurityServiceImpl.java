package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.AdminAccounts;
import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.entity.UserAccounts;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.worker.WorkerChangeEmailRequest;
import com.example.backend.model.worker.WorkerChangePasswordRequest;
import com.example.backend.model.worker.WorkerResetPasswordRequest;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.AdminAccountsService;
import com.example.backend.service.AuthCodeService;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.service.UserAccountsService;
import com.example.backend.service.WorkerSecurityService;
import com.example.backend.utils.PasswordUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WorkerSecurityServiceImpl implements WorkerSecurityService {

    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    private final TechnicianAccountsService technicianAccountsService;
    private final UserAccountsService userAccountsService;
    private final AdminAccountsService adminAccountsService;
    private final AuthCodeService authCodeService;

    public WorkerSecurityServiceImpl(
        TechnicianAccountsService technicianAccountsService,
        UserAccountsService userAccountsService,
        AdminAccountsService adminAccountsService,
        AuthCodeService authCodeService
    ) {
        this.technicianAccountsService = technicianAccountsService;
        this.userAccountsService = userAccountsService;
        this.adminAccountsService = adminAccountsService;
        this.authCodeService = authCodeService;
    }

    @Override
    public void changePassword(WorkerChangePasswordRequest request) {
        LoginUserInfo user = requireWorker();
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

        String accountId = user.getAccountId();
        TechnicianAccounts technician = technicianAccountsService.getById(accountId);
        if (technician == null || (technician.getIsDelete() != null && technician.getIsDelete() != 0)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "师傅账号不存在");
        }
        String salt = technician.getSalt();
        String hash = technician.getPasswordHash();
        if (!StringUtils.hasText(salt) || !StringUtils.hasText(hash)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前账号未设置密码，请使用忘记密码重置");
        }
        String expectedHash = PasswordUtil.hashPassword(oldPassword, salt);
        if (!hash.equals(expectedHash)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "旧密码不正确");
        }
        String newHash = PasswordUtil.hashPassword(newPassword, salt);
        technician.setPasswordHash(newHash);
        technician.setUpdatedTime(System.currentTimeMillis());
        technicianAccountsService.updateById(technician);
    }

    @Override
    public void sendResetPasswordCode() {
        LoginUserInfo user = requireWorker();
        TechnicianAccounts technician = technicianAccountsService.getById(user.getAccountId());
        if (technician == null || (technician.getIsDelete() != null && technician.getIsDelete() != 0)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "师傅账号不存在");
        }
        String email = safeTrim(technician.getEmail());
        if (!StringUtils.hasText(email)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前账号未绑定邮箱，无法找回密码");
        }
        authCodeService.sendCode(email, "WORKER_RESET_PASSWORD");
    }

    @Override
    public void resetPasswordByCode(WorkerResetPasswordRequest request) {
        LoginUserInfo user = requireWorker();
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数不能为空");
        }
        String code = safeTrim(request.getCode());
        String newPassword = safeTrim(request.getNewPassword());
        String confirmPassword = safeTrim(request.getConfirmPassword());
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码不能为空");
        }
        if (!StringUtils.hasText(newPassword) || !StringUtils.hasText(confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新密码不能为空");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "两次输入的新密码不一致");
        }
        if (newPassword.length() < 6) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新密码至少6位");
        }

        String accountId = user.getAccountId();
        TechnicianAccounts technician = technicianAccountsService.getById(accountId);
        if (technician == null || (technician.getIsDelete() != null && technician.getIsDelete() != 0)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "师傅账号不存在");
        }
        String email = safeTrim(technician.getEmail());
        if (!StringUtils.hasText(email)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前账号未绑定邮箱，无法找回密码");
        }
        authCodeService.verifyCode(email, "WORKER_RESET_PASSWORD", code);

        String salt = technician.getSalt();
        if (!StringUtils.hasText(salt)) {
            salt = PasswordUtil.generateSalt(16);
            technician.setSalt(salt);
        }
        technician.setPasswordHash(PasswordUtil.hashPassword(newPassword, salt));
        technician.setUpdatedTime(System.currentTimeMillis());
        technicianAccountsService.updateById(technician);
    }

    @Override
    public void sendChangeEmailCode(String newEmail) {
        LoginUserInfo user = requireWorker();
        String email = safeTrim(newEmail);
        if (!StringUtils.hasText(email)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新邮箱不能为空");
        }
        TechnicianAccounts technician = technicianAccountsService.getById(user.getAccountId());
        if (technician == null || (technician.getIsDelete() != null && technician.getIsDelete() != 0)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "师傅账号不存在");
        }
        if (!email.matches(EMAIL_PATTERN)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "邮箱格式不正确");
        }
        if (email.equalsIgnoreCase(safeTrim(technician.getEmail()))) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新邮箱不能与当前邮箱相同");
        }
        ensureEmailAvailable(email, user.getAccountId());
        authCodeService.sendCode(email, "WORKER_CHANGE_EMAIL");
    }

    @Override
    public void changeEmail(WorkerChangeEmailRequest request) {
        LoginUserInfo user = requireWorker();
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
        TechnicianAccounts technician = technicianAccountsService.getById(user.getAccountId());
        if (technician == null || (technician.getIsDelete() != null && technician.getIsDelete() != 0)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "师傅账号不存在");
        }
        if (newEmail.equalsIgnoreCase(safeTrim(technician.getEmail()))) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新邮箱不能与当前邮箱相同");
        }
        ensureEmailAvailable(newEmail, user.getAccountId());
        authCodeService.verifyCode(newEmail, "WORKER_CHANGE_EMAIL", code);
        technician.setEmail(newEmail);
        technician.setUpdatedTime(System.currentTimeMillis());
        technicianAccountsService.updateById(technician);
    }

    private void ensureEmailAvailable(String email, String currentWorkerId) {
        TechnicianAccounts technicianExists = technicianAccountsService.getOne(
            new LambdaQueryWrapper<TechnicianAccounts>()
                .eq(TechnicianAccounts::getEmail, email)
                .eq(TechnicianAccounts::getIsDelete, 0),
            false
        );
        if (technicianExists != null && !technicianExists.getId().equals(currentWorkerId)) {
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

        AdminAccounts adminExists = adminAccountsService.getOne(
            new LambdaQueryWrapper<AdminAccounts>()
                .eq(AdminAccounts::getEmail, email)
                .eq(AdminAccounts::getIsDelete, 0),
            false
        );
        if (adminExists != null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该邮箱已被注册");
        }
    }

    private LoginUserInfo requireWorker() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.WORKER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问师傅接口");
        }
        if (!StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return user;
    }

    private String safeTrim(String s) {
        return s == null ? null : s.trim();
    }
}
