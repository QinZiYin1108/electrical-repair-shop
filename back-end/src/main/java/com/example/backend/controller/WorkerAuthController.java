package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.AccountCancelRecords;
import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.entity.TechnicianProfiles;
import com.example.backend.entity.TechnicianVisitFeePolicies;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.TechnicianAccountsMapper;
import com.example.backend.model.auth.WorkerLoginByCodeRequest;
import com.example.backend.model.auth.WorkerLoginByPasswordRequest;
import com.example.backend.model.auth.WorkerLoginResponse;
import com.example.backend.model.auth.WorkerResetPasswordByPhoneRequest;
import com.example.backend.model.auth.WorkerSendCodeRequest;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.token.JwtTokenService;
import com.example.backend.security.token.TokenVersions;
import com.example.backend.service.AccountCancelRecordsService;
import com.example.backend.service.AuthCodeService;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.service.TechnicianProfilesService;
import com.example.backend.service.TechnicianVisitFeePoliciesService;
import com.example.backend.utils.PasswordUtil;
import com.example.backend.utils.id.SnowflakeIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "公开/登录认证/师傅端")
@RequestMapping("/pass/auth/worker")
public class WorkerAuthController {

    private static final int ACCOUNT_STATUS_FROZEN = 3;
    private static final int ACCOUNT_STATUS_LEFT = 4;
    private static final long CANCEL_GRACE_MILLIS = 7L * 24L * 60L * 60L * 1000L;

    private final TechnicianAccountsService technicianAccountsService;
    private final TechnicianAccountsMapper technicianAccountsMapper;
    private final TechnicianProfilesService technicianProfilesService;
    private final TechnicianVisitFeePoliciesService technicianVisitFeePoliciesService;
    private final AccountCancelRecordsService accountCancelRecordsService;
    private final JwtTokenService jwtTokenService;
    private final AuthCodeService authCodeService;

    public WorkerAuthController(
            TechnicianAccountsService technicianAccountsService,
            TechnicianAccountsMapper technicianAccountsMapper,
            TechnicianProfilesService technicianProfilesService,
            TechnicianVisitFeePoliciesService technicianVisitFeePoliciesService,
            AccountCancelRecordsService accountCancelRecordsService,
            JwtTokenService jwtTokenService,
            AuthCodeService authCodeService) {
        this.technicianAccountsService = technicianAccountsService;
        this.technicianAccountsMapper = technicianAccountsMapper;
        this.technicianProfilesService = technicianProfilesService;
        this.technicianVisitFeePoliciesService = technicianVisitFeePoliciesService;
        this.accountCancelRecordsService = accountCancelRecordsService;
        this.jwtTokenService = jwtTokenService;
        this.authCodeService = authCodeService;
    }

    @Operation(summary = "手机号密码登录")
    @PostMapping("/login/password")
    public Result<WorkerLoginResponse> loginByPassword(
            @Parameter(
                            description = "手机号+密码登录请求（注销反悔期内需传 confirmCancel=true 撤销注销）",
                            required = true)
                    @Valid
                    @RequestBody
                    WorkerLoginByPasswordRequest request) {
        TechnicianAccounts technician =
                technicianAccountsMapper.selectByPhoneIncludeDeleted(request.getPhone());
        if (technician == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "手机号或者密码有误");
        }

        if (technician.getIsDelete() != null && technician.getIsDelete() != 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已注销，无法登录");
        }

        if (!StringUtils.hasText(technician.getPasswordHash())
                || !PasswordUtil.matches(
                        request.getPassword(),
                        technician.getPasswordHash(),
                        technician.getSalt())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "手机号或者密码有误");
        }
        if (PasswordUtil.needsRehash(technician.getPasswordHash())) {
            technician.setPasswordHash(
                    PasswordUtil.hashPassword(request.getPassword(), technician.getSalt()));
            technician.setUpdatedTime(System.currentTimeMillis());
            technicianAccountsService.updateById(technician);
        }
        if (technician.getAccountStatus() != null
                && technician.getAccountStatus() == ACCOUNT_STATUS_FROZEN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已冻结，无法登录");
        }
        if (technician.getAccountStatus() != null
                && technician.getAccountStatus() == ACCOUNT_STATUS_LEFT) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已离职，无法登录");
        }

        WorkerLoginResponse cancelResp =
                handleCancellationBeforeLogin(
                        technician,
                        request.getConfirmCancel() != null && request.getConfirmCancel());
        if (cancelResp != null && Boolean.TRUE.equals(cancelResp.getNeedCancelConfirm())) {
            return Result.success(cancelResp);
        }

        ensureTechnicianProfileExists(technician);
        ensureVisitPoliciesExist(technician.getId());

        String token =
                jwtTokenService.generateToken(
                        technician.getId(), AccountRole.WORKER, technician.getTokenVersion());
        WorkerLoginResponse resp = cancelResp == null ? new WorkerLoginResponse() : cancelResp;
        resp.setToken(token);
        return Result.success(resp);
    }

    @Operation(summary = "发送登录验证码")
    @PostMapping("/code/send")
    public Result<Void> sendLoginCode(
            @Parameter(description = "目标手机号", required = true) @Valid @RequestBody
                    WorkerSendCodeRequest request) {
        authCodeService.sendCode(request.getPhone(), "WORKER_LOGIN");
        return Result.success();
    }

    @Operation(summary = "验证码登录（新用户自动注册）")
    @PostMapping("/login/code")
    @Transactional(rollbackFor = Exception.class)
    public Result<WorkerLoginResponse> loginByCode(
            @Parameter(description = "手机号+验证码登录请求（注销反悔期内需传 confirmCancel=true）", required = true)
                    @Valid
                    @RequestBody
                    WorkerLoginByCodeRequest request) {
        String phone = request.getPhone();
        String code = request.getCode();
        authCodeService.verifyCode(phone, "WORKER_LOGIN", code);

        TechnicianAccounts technician = technicianAccountsMapper.selectByPhoneIncludeDeleted(phone);
        if (technician != null
                && technician.getIsDelete() != null
                && technician.getIsDelete() != 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已注销，无法登录");
        }
        if (technician != null
                && technician.getAccountStatus() != null
                && technician.getAccountStatus() == ACCOUNT_STATUS_FROZEN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已冻结，无法登录");
        }
        if (technician != null
                && technician.getAccountStatus() != null
                && technician.getAccountStatus() == ACCOUNT_STATUS_LEFT) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已离职，无法登录");
        }

        WorkerLoginResponse cancelResp = null;
        if (technician != null) {
            cancelResp =
                    handleCancellationBeforeLogin(
                            technician,
                            request.getConfirmCancel() != null && request.getConfirmCancel());
            if (cancelResp != null && Boolean.TRUE.equals(cancelResp.getNeedCancelConfirm())) {
                return Result.success(cancelResp);
            }
        }

        if (technician == null) {
            long now = System.currentTimeMillis();
            technician = new TechnicianAccounts();
            technician.setId(SnowflakeIdUtil.nextTechnicianId());
            technician.setPhone(phone);
            technician.setUsername("师傅" + (now % 1000000));
            technician.setAccountStatus(2);
            technician.setWorkStatus(0);
            technician.setCreatedTime(now);
            technician.setUpdatedTime(now);
            technician.setIsDelete(0);
            technicianAccountsService.save(technician);
            createTechnicianProfile(technician, now);
            createDefaultVisitPolicies(technician.getId(), now);
        } else {
            ensureTechnicianProfileExists(technician);
            ensureVisitPoliciesExist(technician.getId());
        }

        String token =
                jwtTokenService.generateToken(
                        technician.getId(), AccountRole.WORKER, technician.getTokenVersion());
        WorkerLoginResponse resp = cancelResp == null ? new WorkerLoginResponse() : cancelResp;
        resp.setToken(token);
        return Result.success(resp);
    }

    @Operation(summary = "发送重置密码验证码")
    @PostMapping("/password/reset/code/send")
    public Result<Void> sendResetPasswordCode(
            @Parameter(description = "已注册的师傅手机号", required = true) @Valid @RequestBody
                    WorkerSendCodeRequest request) {
        TechnicianAccounts technician =
                technicianAccountsService.getOne(
                        new LambdaQueryWrapper<TechnicianAccounts>()
                                .eq(TechnicianAccounts::getPhone, request.getPhone()),
                        false);
        if (technician == null
                || (technician.getIsDelete() != null && technician.getIsDelete() != 0)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "手机号不存在");
        }
        authCodeService.sendCode(request.getPhone(), "WORKER_RESET_PASSWORD");
        return Result.success();
    }

    @Operation(summary = "重置密码（需验证码，新密码≥6位）")
    @PostMapping("/password/reset")
    public Result<Void> resetPassword(
            @Parameter(description = "手机号+验证码+新密码+确认密码", required = true) @Valid @RequestBody
                    WorkerResetPasswordByPhoneRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "两次输入的新密码不一致");
        }
        if (request.getNewPassword().length() < 6) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新密码至少6位");
        }
        authCodeService.verifyCode(request.getPhone(), "WORKER_RESET_PASSWORD", request.getCode());

        TechnicianAccounts technician =
                technicianAccountsService.getOne(
                        new LambdaQueryWrapper<TechnicianAccounts>()
                                .eq(TechnicianAccounts::getPhone, request.getPhone()),
                        false);
        if (technician == null
                || (technician.getIsDelete() != null && technician.getIsDelete() != 0)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "手机号不存在");
        }

        String salt = technician.getSalt();
        if (!StringUtils.hasText(salt)) {
            salt = PasswordUtil.generateSalt(16);
            technician.setSalt(salt);
        }
        technician.setPasswordHash(PasswordUtil.hashPassword(request.getNewPassword(), salt));
        technician.setTokenVersion(TokenVersions.next(technician.getTokenVersion()));
        technician.setUpdatedTime(System.currentTimeMillis());
        technicianAccountsService.updateById(technician);
        return Result.success();
    }

    private WorkerLoginResponse handleCancellationBeforeLogin(
            TechnicianAccounts technician, boolean confirmCancel) {
        if (technician == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        AccountCancelRecords record =
                accountCancelRecordsService.getOne(
                        new LambdaQueryWrapper<AccountCancelRecords>()
                                .eq(AccountCancelRecords::getAccountId, technician.getId())
                                .eq(AccountCancelRecords::getCancelType, 1)
                                .eq(AccountCancelRecords::getIsDelete, 0)
                                .orderByDesc(AccountCancelRecords::getCreatedTime)
                                .last("limit 1"),
                        false);
        if (record == null || record.getCreatedTime() == null || record.getCreatedTime() <= 0L) {
            return null;
        }

        long applyTime = record.getCreatedTime();
        Long deadlineObj = record.getCancelTime();
        long deadline = deadlineObj == null ? 0L : deadlineObj;
        if (deadline <= 0L || now >= deadline) {
            finalizeCancellation(technician, now);
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已注销，无法登录");
        }

        if (!confirmCancel) {
            WorkerLoginResponse resp = new WorkerLoginResponse();
            resp.setNeedCancelConfirm(true);
            resp.setCancelApplyTime(applyTime);
            resp.setCancelDeadlineTime(deadline);
            resp.setCancelRevoked(false);
            return resp;
        }

        // Confirmed: revoke cancellation and continue to login
        accountCancelRecordsService.update(
                null,
                new UpdateWrapper<AccountCancelRecords>()
                        .set("is_delete", 1)
                        .eq("account_id", technician.getId())
                        .eq("cancel_type", 1)
                        .eq("is_delete", 0));
        technician.setUpdatedTime(now);
        technicianAccountsService.updateById(technician);

        WorkerLoginResponse resp = new WorkerLoginResponse();
        resp.setNeedCancelConfirm(false);
        resp.setCancelApplyTime(applyTime);
        resp.setCancelDeadlineTime(deadline);
        resp.setCancelRevoked(true);
        return resp;
    }

    private void finalizeCancellation(TechnicianAccounts technician, long now) {
        if (technician == null) {
            return;
        }
        technician.setUpdatedTime(now);
        technician.setIsDelete(1);
        technicianAccountsService.updateById(technician);

        // Mark cancel record as system canceled (keep it as main data).
        accountCancelRecordsService.update(
                null,
                new UpdateWrapper<AccountCancelRecords>()
                        .set("cancel_type", 2)
                        .set("cancel_time", now)
                        .set("operator_id", "SYSTEM")
                        .eq("account_id", technician.getId())
                        .eq("cancel_type", 1)
                        .eq("is_delete", 0));
    }

    private void ensureTechnicianProfileExists(TechnicianAccounts technician) {
        TechnicianProfiles profile =
                technicianProfilesService.getOne(
                        new LambdaQueryWrapper<TechnicianProfiles>()
                                .eq(TechnicianProfiles::getTechnicianAccountId, technician.getId())
                                .eq(TechnicianProfiles::getIsDelete, 0),
                        false);
        if (profile != null) {
            return;
        }
        createTechnicianProfile(technician, System.currentTimeMillis());
    }

    private void createTechnicianProfile(TechnicianAccounts technician, long now) {
        TechnicianProfiles profile = new TechnicianProfiles();
        profile.setId(SnowflakeIdUtil.nextTechnicianProfileId());
        profile.setTechnicianAccountId(technician.getId());
        profile.setWorkYears(0);
        profile.setResponseTime(0);
        profile.setCreatedTime(now);
        profile.setUpdatedTime(now);
        profile.setIsDelete(0);
        technicianProfilesService.save(profile);
    }

    private void ensureVisitPoliciesExist(String accountId) {
        long count =
                technicianVisitFeePoliciesService.count(
                        new LambdaQueryWrapper<TechnicianVisitFeePolicies>()
                                .eq(TechnicianVisitFeePolicies::getTechnicianAccountId, accountId)
                                .eq(TechnicianVisitFeePolicies::getIsDelete, 0));
        if (count > 0) {
            return;
        }
        createDefaultVisitPolicies(accountId, System.currentTimeMillis());
    }

    private void createDefaultVisitPolicies(String accountId, long now) {
        TechnicianVisitFeePolicies repairPolicy = buildDefaultPolicy(accountId, 1, now);
        TechnicianVisitFeePolicies installPolicy = buildDefaultPolicy(accountId, 2, now);
        technicianVisitFeePoliciesService.save(repairPolicy);
        technicianVisitFeePoliciesService.save(installPolicy);
    }

    private TechnicianVisitFeePolicies buildDefaultPolicy(
            String accountId, int serviceKind, long now) {
        TechnicianVisitFeePolicies policy = new TechnicianVisitFeePolicies();
        policy.setId(SnowflakeIdUtil.nextTechnicianVisitFeePolicyId());
        policy.setTechnicianAccountId(accountId);
        policy.setServiceKind(serviceKind);
        policy.setMinVisitFee(BigDecimal.ZERO);
        policy.setBaseRadiusKm(BigDecimal.ZERO);
        policy.setExtraFeePerKm(BigDecimal.ZERO);
        policy.setDistanceCalcType(1);
        policy.setRoundingRule(1);
        policy.setMaxVisitFee(null);
        policy.setIsActive(1);
        policy.setEffectiveTime(now);
        policy.setCreatedTime(now);
        policy.setUpdatedTime(now);
        policy.setIsDelete(0);
        return policy;
    }
}
