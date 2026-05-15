package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.AccountCancelRecords;
import com.example.backend.entity.AdminAccounts;
import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.entity.UserAccounts;
import com.example.backend.entity.UserProfiles;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.UserAccountsMapper;
import com.example.backend.model.user.UserBindEmailRequest;
import com.example.backend.model.user.UserLoginByPasswordRequest;
import com.example.backend.model.user.UserLoginResponse;
import com.example.backend.model.user.UserSendCodeRequest;
import com.example.backend.model.user.UserWxLoginRequest;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.security.token.JwtTokenService;
import com.example.backend.service.AccountCancelRecordsService;
import com.example.backend.service.AdminAccountsService;
import com.example.backend.service.AuthCodeService;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.service.UserAccountsService;
import com.example.backend.service.UserProfilesService;
import com.example.backend.utils.PasswordUtil;
import com.example.backend.utils.id.SnowflakeIdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/pass/auth/user")
public class UserAuthController {

    private static final String USER_BIND_EMAIL_CODE_TYPE = "USER_BIND_EMAIL";
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    private final UserAccountsService userAccountsService;
    private final UserAccountsMapper userAccountsMapper;
    private final UserProfilesService userProfilesService;
    private final JwtTokenService jwtTokenService;
    private final AuthCodeService authCodeService;
    private final AccountCancelRecordsService accountCancelRecordsService;
    private final AdminAccountsService adminAccountsService;
    private final TechnicianAccountsService technicianAccountsService;
    private final HttpServletRequest request;

    @Value("${wx.mini.appid}")
    private String appid;

    @Value("${wx.mini.secret}")
    private String secret;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserAuthController(
        UserAccountsService userAccountsService,
        UserAccountsMapper userAccountsMapper,
        UserProfilesService userProfilesService,
        JwtTokenService jwtTokenService,
        AuthCodeService authCodeService,
        AccountCancelRecordsService accountCancelRecordsService,
        AdminAccountsService adminAccountsService,
        TechnicianAccountsService technicianAccountsService,
        HttpServletRequest request
    ) {
        this.userAccountsService = userAccountsService;
        this.userAccountsMapper = userAccountsMapper;
        this.userProfilesService = userProfilesService;
        this.jwtTokenService = jwtTokenService;
        this.authCodeService = authCodeService;
        this.accountCancelRecordsService = accountCancelRecordsService;
        this.adminAccountsService = adminAccountsService;
        this.technicianAccountsService = technicianAccountsService;
        this.request = request;
    }

    @PostMapping("/login")
    @Transactional(rollbackFor = Exception.class)
    public Result<UserLoginResponse> wxLogin(@Valid @RequestBody UserWxLoginRequest request) {
        WxSessionInfo sessionInfo = resolveWxSession(request.getCode());
        UserAccounts account = userAccountsMapper.selectByWxOpenidIncludeDeleted(sessionInfo.getOpenid());
        boolean newAccountCreated = false;

        if (account == null) {
            account = createWechatUserAccount(sessionInfo);
            newAccountCreated = true;
        } else {
            if (safeInt(account.getIsDelete()) != 0) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "该微信对应账号已注销，无法登录");
            }
            if (StringUtils.hasText(sessionInfo.getUnionid())
                && !sessionInfo.getUnionid().equals(account.getWxUnionid())) {
                account.setWxUnionid(sessionInfo.getUnionid());
                account.setUpdatedTime(System.currentTimeMillis());
                userAccountsService.updateById(account);
            }
        }

        return buildLoginResponse(account, request.getConfirmCancel(), newAccountCreated);
    }

    @PostMapping("/login/password")
    public Result<UserLoginResponse> loginByPassword(@Valid @RequestBody UserLoginByPasswordRequest request) {
        String email = safeTrim(request.getEmail());
        UserAccounts account = userAccountsMapper.selectByEmailIncludeDeleted(email);
        if (account == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "\u90ae\u7bb1\u6216\u8005\u5bc6\u7801\u6709\u8bef");
        }
        if (safeInt(account.getIsDelete()) != 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "\u8be5\u90ae\u7bb1\u5bf9\u5e94\u8d26\u53f7\u5df2\u6ce8\u9500\uff0c\u65e0\u6cd5\u767b\u5f55");
        }
        if (!StringUtils.hasText(account.getPassword()) || !StringUtils.hasText(account.getSalt())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "\u90ae\u7bb1\u6216\u8005\u5bc6\u7801\u6709\u8bef");
        }

        String expectedHash = PasswordUtil.hashPassword(request.getPassword(), account.getSalt());
        if (!expectedHash.equals(account.getPassword())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "\u90ae\u7bb1\u6216\u8005\u5bc6\u7801\u6709\u8bef");
        }

        return buildLoginResponse(account, request.getConfirmCancel(), false);
    }

    @PostMapping("/bind-email")
    public Result<Void> bindEmail(@Valid @RequestBody UserBindEmailRequest request) {
        String email = safeTrim(request.getEmail());
        authCodeService.verifyCode(email, USER_BIND_EMAIL_CODE_TYPE, safeTrim(request.getCode()));

        UserAccounts account = requireCurrentUserAccount();
        if (StringUtils.hasText(account.getEmail())) {
            if (email.equalsIgnoreCase(account.getEmail())) {
                return Result.success();
            }
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前账号已绑定其他邮箱");
        }

        ensureEmailAvailable(email, account.getId());
        Long now = System.currentTimeMillis();
        Integer version = account.getVersion();
        UpdateWrapper<UserAccounts> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", account.getId());
        if (version != null) {
            wrapper.eq("version", version);
        }

        UserAccounts updateEntity = new UserAccounts();
        updateEntity.setId(account.getId());
        updateEntity.setEmail(email);
        updateEntity.setUpdatedTime(now);
        if (version != null) {
            updateEntity.setVersion(version);
        }

        boolean ok = userAccountsService.update(updateEntity, wrapper);
        if (!ok) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "邮箱绑定失败，请重试");
        }
        return Result.success();
    }

    @PostMapping("/code/send")
    public Result<Void> sendBindEmailCode(@Valid @RequestBody UserSendCodeRequest request) {
        authCodeService.sendCode(request.getEmail(), USER_BIND_EMAIL_CODE_TYPE);
        return Result.success();
    }

    private Result<UserLoginResponse> buildLoginResponse(
        UserAccounts account,
        Boolean confirmCancel,
        boolean newAccountCreated
    ) {
        UserLoginResponse response = new UserLoginResponse();
        response.setNewAccountCreated(newAccountCreated);

        validateAccountStatus(account);
        handleCancellationBeforeLogin(account, confirmCancel, response);
        if (response.isNeedCancelConfirm()) {
            response.setToken("");
            fillLoginState(account, response);
            return Result.success(response);
        }

        ensureUserProfileExists(account);
        response.setToken(jwtTokenService.generateToken(account.getId(), AccountRole.USER));
        fillLoginState(account, response);
        return Result.success(response);
    }

    private UserAccounts createWechatUserAccount(WxSessionInfo sessionInfo) {
        long now = System.currentTimeMillis();
        UserAccounts account = new UserAccounts();
        account.setId(SnowflakeIdUtil.nextUserId());
        account.setWxOpenid(sessionInfo.getOpenid());
        account.setWxUnionid(sessionInfo.getUnionid());
        account.setUsername("用户" + randomSuffix());
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(1);
        account.setCreatedTime(now);
        account.setUpdatedTime(now);
        account.setIsDelete(0);
        userAccountsService.save(account);
        createUserProfile(account.getId(), now);
        return account;
    }

    private void createUserProfile(String accountId, long now) {
        UserProfiles profile = new UserProfiles();
        profile.setId(SnowflakeIdUtil.nextUserProfileId());
        profile.setAccountId(accountId);
        profile.setCreatedTime(now);
        profile.setUpdatedTime(now);
        profile.setIsDelete(0);
        userProfilesService.save(profile);
    }

    private void ensureUserProfileExists(UserAccounts account) {
        if (account == null || !StringUtils.hasText(account.getId())) {
            return;
        }
        UserProfiles profile = userProfilesService.getOne(
            new LambdaQueryWrapper<UserProfiles>()
                .eq(UserProfiles::getAccountId, account.getId())
                .eq(UserProfiles::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (profile != null) {
            return;
        }
        createUserProfile(account.getId(), System.currentTimeMillis());
    }

    private void validateAccountStatus(UserAccounts account) {
        if (account == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户账号不存在");
        }
        if (safeInt(account.getStatus()) == 2) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已冻结，无法登录");
        }
        if (safeInt(account.getStatus()) == 4) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已注销，无法登录");
        }
    }

    private void handleCancellationBeforeLogin(UserAccounts account, Boolean confirmCancel, UserLoginResponse response) {
        if (account == null || safeInt(account.getStatus()) != 3) {
            return;
        }
        if (safeInt(account.getIsDelete()) != 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已注销，无法登录");
        }

        AccountCancelRecords record = accountCancelRecordsService.getOne(
            new LambdaQueryWrapper<AccountCancelRecords>()
                .eq(AccountCancelRecords::getAccountId, account.getId())
                .eq(AccountCancelRecords::getCancelType, 1)
                .eq(AccountCancelRecords::getIsDelete, 0)
                .orderByDesc(AccountCancelRecords::getCreatedTime)
                .last("limit 1"),
            false
        );
        if (record == null || record.getCreatedTime() == null || record.getCreatedTime() <= 0L) {
            return;
        }

        long now = System.currentTimeMillis();
        Long deadline = record.getCancelTime();
        response.setCancelApplyTime(record.getCreatedTime());
        response.setCancelDeadlineTime(deadline);
        if (deadline == null || deadline <= 0L || now >= deadline) {
            finalizeCancellation(account, now);
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已注销，无法登录");
        }

        boolean confirmed = confirmCancel != null && confirmCancel;
        if (!confirmed) {
            response.setNeedCancelConfirm(true);
            return;
        }

        account.setStatus(1);
        account.setCancelApplyTime(null);
        account.setCancelTime(null);
        account.setUpdatedTime(now);
        userAccountsService.updateById(account);

        accountCancelRecordsService.update(
            null,
            new UpdateWrapper<AccountCancelRecords>()
                .set("is_delete", 1)
                .eq("account_id", account.getId())
                .eq("is_delete", 0)
                .eq("cancel_type", 1)
        );
        response.setCancelRevoked(true);
    }

    private void finalizeCancellation(UserAccounts account, long now) {
        if (account == null) {
            return;
        }
        account.setStatus(4);
        account.setCancelTime(now);
        account.setUpdatedTime(now);
        account.setIsDelete(1);
        userAccountsService.updateById(account);

        accountCancelRecordsService.update(
            null,
            new UpdateWrapper<AccountCancelRecords>()
                .set("cancel_type", 2)
                .set("cancel_time", now)
                .set("operator_id", "SYSTEM")
                .eq("account_id", account.getId())
                .eq("cancel_type", 1)
                .eq("is_delete", 0)
        );
    }

    private void fillLoginState(UserAccounts account, UserLoginResponse response) {
        if (account == null || response == null) {
            return;
        }
        response.setEmailBound(StringUtils.hasText(account.getEmail()));
        response.setWechatBound(StringUtils.hasText(account.getWxOpenid()));
        response.setPasswordSet(
            StringUtils.hasText(account.getPassword()) && StringUtils.hasText(account.getSalt())
        );
    }

    private void ensureEmailAvailable(String email, String currentAccountId) {
        String normalizedEmail = safeTrim(email);
        if (!StringUtils.hasText(normalizedEmail) || !normalizedEmail.matches(EMAIL_PATTERN)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "邮箱格式不正确");
        }

        UserAccounts userExists = userAccountsMapper.selectByEmailIncludeDeleted(normalizedEmail);
        if (userExists != null && !userExists.getId().equals(currentAccountId)) {
            if (safeInt(userExists.getIsDelete()) != 0) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "该邮箱对应账号已注销，暂时无法使用");
            }
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该邮箱已被注册");
        }

        TechnicianAccounts technicianExists = technicianAccountsService.getOne(
            new LambdaQueryWrapper<TechnicianAccounts>()
                .eq(TechnicianAccounts::getEmail, normalizedEmail)
                .eq(TechnicianAccounts::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (technicianExists != null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该邮箱已被其他账号使用");
        }

        AdminAccounts adminExists = adminAccountsService.getOne(
            new LambdaQueryWrapper<AdminAccounts>()
                .eq(AdminAccounts::getEmail, normalizedEmail)
                .eq(AdminAccounts::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (adminExists != null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该邮箱已被其他账号使用");
        }
    }

    private UserAccounts requireCurrentUserAccount() {
        LoginUserInfo userInfo = AuthUserContext.get();
        if (userInfo == null || userInfo.getRole() != AccountRole.USER || !StringUtils.hasText(userInfo.getAccountId())) {
            String authHeader = request.getHeader("Authorization");
            if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
            }
            userInfo = jwtTokenService.parseToken(authHeader.substring(7));
        }

        if (userInfo.getRole() != AccountRole.USER || !StringUtils.hasText(userInfo.getAccountId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作");
        }

        UserAccounts account = userAccountsService.getById(userInfo.getAccountId());
        if (account == null || safeInt(account.getIsDelete()) != 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户不存在");
        }
        if (safeInt(account.getStatus()) == 4) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已注销");
        }
        return account;
    }

    private WxSessionInfo resolveWxSession(String code) {
        String url = "https://api.weixin.qq.com/sns/jscode2session"
            + "?appid=" + appid
            + "&secret=" + secret
            + "&js_code=" + code
            + "&grant_type=authorization_code";
        String responseBody;
        try {
            responseBody = restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信登录失败");
        }

        Map<?, ?> body;
        try {
            body = objectMapper.readValue(responseBody, Map.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信登录失败");
        }
        if (body == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信登录失败");
        }

        Object errCode = body.get("errcode");
        if (errCode instanceof Number && ((Number) errCode).intValue() != 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信登录失败");
        }

        String openid = body.get("openid") == null ? null : String.valueOf(body.get("openid"));
        if (!StringUtils.hasText(openid)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信登录失败");
        }

        WxSessionInfo sessionInfo = new WxSessionInfo();
        sessionInfo.setOpenid(openid);
        Object unionid = body.get("unionid");
        if (unionid != null) {
            sessionInfo.setUnionid(String.valueOf(unionid));
        }
        return sessionInfo;
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String randomSuffix() {
        Random random = new Random();
        int value = random.nextInt(900000) + 100000;
        return String.valueOf(value);
    }

    private static class WxSessionInfo {
        private String openid;
        private String unionid;

        public String getOpenid() {
            return openid;
        }

        public void setOpenid(String openid) {
            this.openid = openid;
        }

        public String getUnionid() {
            return unionid;
        }

        public void setUnionid(String unionid) {
            this.unionid = unionid;
        }
    }
}
