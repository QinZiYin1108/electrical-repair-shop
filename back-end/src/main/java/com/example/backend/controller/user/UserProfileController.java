package com.example.backend.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.Images;
import com.example.backend.entity.UserAccounts;
import com.example.backend.entity.UserProfiles;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.user.UserProfileDetailResponse;
import com.example.backend.model.user.UserProfileUpdateRequest;
import com.example.backend.model.user.UserAccountCancelApplyRequest;
import com.example.backend.model.user.UserAccountCancelStatusResponse;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.entity.AccountCancelRecords;
import com.example.backend.service.AccountCancelRecordsService;
import com.example.backend.service.ImagesService;
import com.example.backend.service.SystemConfigsService;
import com.example.backend.service.UserAccountsService;
import com.example.backend.service.UserProfilesService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import com.example.backend.utils.oss.OssUtil;
import com.example.backend.utils.upload.UploadLimitUtil;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping("/user/profile")
public class UserProfileController {

    private final UserAccountsService userAccountsService;
    private final UserProfilesService userProfilesService;
    private final ImagesService imagesService;
    private final OssUtil ossUtil;
    private final AccountCancelRecordsService accountCancelRecordsService;
    private final SystemConfigsService systemConfigsService;

    private static final int STATUS_NORMAL = 1;
    private static final int STATUS_FROZEN = 2;
    private static final int STATUS_CANCEL_APPLY = 3;
    private static final int STATUS_CANCELED = 4;
    private static final long DEFAULT_CANCEL_GRACE_DAYS = 7L;
    private static final int DEFAULT_CANCEL_DATA_RETENTION_DAYS = 30;

    public UserProfileController(
        UserAccountsService userAccountsService,
        UserProfilesService userProfilesService,
        ImagesService imagesService,
        OssUtil ossUtil,
        AccountCancelRecordsService accountCancelRecordsService,
        SystemConfigsService systemConfigsService
    ) {
        this.userAccountsService = userAccountsService;
        this.userProfilesService = userProfilesService;
        this.imagesService = imagesService;
        this.ossUtil = ossUtil;
        this.accountCancelRecordsService = accountCancelRecordsService;
        this.systemConfigsService = systemConfigsService;
    }

    @GetMapping("/me")
    public Result<UserProfileDetailResponse> getProfile() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.USER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问用户信息");
        }
        String accountId = user.getAccountId();
        UserAccounts account = userAccountsService.getById(accountId);
        if (account == null || account.getIsDelete() != null && account.getIsDelete() != 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户账号不存在");
        }
        UserProfiles profile = userProfilesService.getOne(
            new LambdaQueryWrapper<UserProfiles>()
                .eq(UserProfiles::getAccountId, accountId)
                .eq(UserProfiles::getIsDelete, 0),
            false
        );
        String avatarUrl = null;
        Images avatarImage = imagesService.getOne(
            new LambdaQueryWrapper<Images>()
                .eq(Images::getBusinessType, "AVATAR")
                .eq(Images::getBusinessId, accountId)
                .eq(Images::getIsDelete, 0)
                .orderByDesc(Images::getCreatedTime)
                .last("limit 1"),
            false
        );
        if (avatarImage != null && StringUtils.hasText(avatarImage.getFileUrl())) {
            avatarUrl = avatarImage.getFileUrl();
        }
        UserProfileDetailResponse resp = new UserProfileDetailResponse();
        resp.setId(account.getId());
        resp.setUsername(account.getUsername());
        resp.setPhone(account.getPhone());
        resp.setEmail(account.getEmail());
        resp.setStatus(account.getStatus());
        resp.setWechatBound(StringUtils.hasText(account.getWxOpenid()));
        resp.setPasswordSet(StringUtils.hasText(account.getPassword()) && StringUtils.hasText(account.getSalt()));
        if (profile != null) {
            resp.setRealName(profile.getRealName());
            resp.setGender(profile.getGender());
            resp.setProfession(profile.getProfession());
            resp.setEmergencyContact(profile.getEmergencyContact());
            resp.setEmergencyPhone(profile.getEmergencyPhone());
            if (profile.getBirthday() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                resp.setBirthday(sdf.format(new Date(profile.getBirthday())));
            }
        }
        resp.setAvatarUrl(avatarUrl);
        return Result.success(resp);
    }

    @GetMapping("/cancel/status")
    public Result<UserAccountCancelStatusResponse> getCancelStatus() {
        LoginUserInfo user = requireCurrentUser();
        UserAccounts account = requireAccount(user.getAccountId());

        long now = System.currentTimeMillis();
        AccountCancelRecords record = findActiveCancelRecord(account.getId());
        if (record != null) {
            Long deadline = record.getCancelTime();
            // Defensive: if deadline missing, treat as expired and finalize.
            if (deadline == null || deadline <= 0L || now >= deadline) {
                finalizeCancellation(account, now);
                throw new BusinessException(ErrorCode.FORBIDDEN, "账号已注销");
            }
        }

        return Result.success(buildCancelStatusResponse(account, record, now));
    }

    @PostMapping("/cancel/apply")
    public Result<UserAccountCancelStatusResponse> applyCancel(@RequestBody(required = false) UserAccountCancelApplyRequest request) {
        LoginUserInfo user = requireCurrentUser();
        UserAccounts account = requireAccount(user.getAccountId());

        int status = safeInt(account.getStatus());
        if (status == STATUS_FROZEN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已冻结，无法注销");
        }
        if (status == STATUS_CANCELED) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已注销");
        }

        long now = System.currentTimeMillis();
        AccountCancelRecords existing = findActiveCancelRecord(account.getId());
        if (existing != null) {
            Long deadline = existing.getCancelTime();
            if (deadline == null || deadline <= 0L || now >= deadline) {
                finalizeCancellation(account, now);
                throw new BusinessException(ErrorCode.FORBIDDEN, "账号已注销");
            }
            // Idempotent: already applied
            return Result.success(buildCancelStatusResponse(account, existing, now));
        }

        String reason = trimToNull(request == null ? null : request.getReason());
        if (!StringUtils.hasText(reason)) {
            reason = "用户主动注销";
        }
        long deadline = now + getCancelGraceMillis();

        account.setStatus(STATUS_CANCEL_APPLY);
        account.setCancelApplyTime(now);
        account.setCancelTime(deadline);
        account.setUpdatedTime(now);
        userAccountsService.updateById(account);

        AccountCancelRecords record = new AccountCancelRecords();
        record.setId(SnowflakeIdUtil.nextAccountCancelRecordId());
        record.setAccountId(account.getId());
        record.setCancelReason(reason);
        record.setCancelType(1);
        record.setOperatorId(account.getId());
        // cancel_time stores planned deadline for grace period
        record.setCancelTime(deadline);
        record.setDataRetentionDays(getCancelDataRetentionDays());
        record.setCreatedTime(now);
        record.setIsDelete(0);
        accountCancelRecordsService.save(record);

        return Result.success(buildCancelStatusResponse(account, record, now));
    }

    @PostMapping("/cancel/revoke")
    public Result<UserAccountCancelStatusResponse> revokeCancel() {
        LoginUserInfo user = requireCurrentUser();
        UserAccounts account = requireAccount(user.getAccountId());

        long now = System.currentTimeMillis();
        AccountCancelRecords record = findActiveCancelRecord(account.getId());
        if (record == null) {
            // Defensive cleanup: if status says canceling but record missing, reset to normal.
            if (safeInt(account.getStatus()) == STATUS_CANCEL_APPLY) {
                account.setStatus(STATUS_NORMAL);
                account.setCancelApplyTime(null);
                account.setCancelTime(null);
                account.setUpdatedTime(now);
                userAccountsService.updateById(account);
            }
            return Result.success(buildCancelStatusResponse(account, null, now));
        }
        Long deadline = record.getCancelTime();
        if (deadline == null || deadline <= 0L || now >= deadline) {
            finalizeCancellation(account, now);
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已注销");
        }

        account.setStatus(STATUS_NORMAL);
        account.setCancelApplyTime(null);
        account.setCancelTime(null);
        account.setUpdatedTime(now);
        userAccountsService.updateById(account);

        // Mark cancel record as deleted (avoid empty SET clause by using wrapper.set).
        accountCancelRecordsService.update(
            null,
            new UpdateWrapper<AccountCancelRecords>()
                .set("is_delete", 1)
                .eq("account_id", account.getId())
                .eq("is_delete", 0)
                .eq("cancel_type", 1)
        );

        return Result.success(buildCancelStatusResponse(account, null, now));
    }

    @PostMapping("/update")
    public Result<Void> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.USER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问用户信息");
        }
        String accountId = user.getAccountId();
        UserAccounts account = userAccountsService.getById(accountId);
        if (account == null || account.getIsDelete() != null && account.getIsDelete() != 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户账号不存在");
        }
        long now = System.currentTimeMillis();
        if (request.getUsername() != null) {
            account.setUsername(request.getUsername());
        }
        if (request.getPhone() != null) {
            account.setPhone(request.getPhone());
        }
        account.setUpdatedTime(now);
        userAccountsService.updateById(account);
        UserProfiles profile = userProfilesService.getOne(
            new LambdaQueryWrapper<UserProfiles>()
                .eq(UserProfiles::getAccountId, accountId)
                .eq(UserProfiles::getIsDelete, 0),
            false
        );
        boolean isNew = profile == null;
        if (isNew) {
            profile = new UserProfiles();
            profile.setId(SnowflakeIdUtil.nextUserProfileId());
            profile.setAccountId(accountId);
            profile.setCreatedTime(now);
            profile.setIsDelete(0);
        }
        if (request.getRealName() != null) {
            profile.setRealName(request.getRealName());
        }
        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }
        if (request.getProfession() != null) {
            profile.setProfession(request.getProfession());
        }
        if (request.getEmergencyContact() != null) {
            profile.setEmergencyContact(request.getEmergencyContact());
        }
        if (request.getEmergencyPhone() != null) {
            profile.setEmergencyPhone(request.getEmergencyPhone());
        }
        if (request.getBirthday() != null && !request.getBirthday().isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            try {
                Date date = sdf.parse(request.getBirthday());
                profile.setBirthday(date.getTime());
            } catch (ParseException e) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "生日格式不正确");
            }
        }
        profile.setUpdatedTime(now);
        if (isNew) {
            userProfilesService.save(profile);
        } else {
            userProfilesService.updateById(profile);
        }
        return Result.success();
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadAvatar(@RequestPart("file") MultipartFile file) {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.USER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问用户信息");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }
        UploadLimitUtil.validateImageSize(file);
        String accountId = user.getAccountId();
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null) {
            int idx = originalFilename.lastIndexOf('.');
            if (idx >= 0 && idx < originalFilename.length() - 1) {
                ext = originalFilename.substring(idx);
            }
        }
        String objectName = "avatars/" + accountId + "/" + UUID.randomUUID() + ext;
        String url;
        try (InputStream in = file.getInputStream()) {
            url = ossUtil.upload(objectName, in);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取上传文件失败");
        }
        Images image = new Images();
        image.setId("IM" + System.currentTimeMillis());
        image.setOriginalName(originalFilename);
        image.setFileName(objectName);
        image.setFilePath(objectName);
        image.setFileUrl(url);
        image.setFileSize(file.getSize());
        image.setMimeType(file.getContentType());
        image.setUploaderId(accountId);
        image.setUploaderType(1);
        image.setBusinessType("AVATAR");
        image.setBusinessId(accountId);
        image.setCreatedTime(System.currentTimeMillis());
        image.setIsDelete(0);
        imagesService.save(image);
        return Result.success(url);
    }

    private LoginUserInfo requireCurrentUser() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.USER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问用户信息");
        }
        return user;
    }

    private UserAccounts requireAccount(String accountId) {
        if (!StringUtils.hasText(accountId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        UserAccounts account = userAccountsService.getById(accountId);
        if (account == null || safeInt(account.getIsDelete()) != 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户账号不存在");
        }
        return account;
    }

    private UserAccountCancelStatusResponse buildCancelStatusResponse(UserAccounts account, AccountCancelRecords record, long now) {
        UserAccountCancelStatusResponse resp = new UserAccountCancelStatusResponse();

        int computedStatus = safeInt(account == null ? null : account.getStatus());
        if (account != null && safeInt(account.getIsDelete()) != 0) {
            computedStatus = STATUS_CANCELED;
        } else if (safeInt(account == null ? null : account.getStatus()) == STATUS_FROZEN) {
            computedStatus = STATUS_FROZEN;
        } else if (record != null) {
            Long deadline = record.getCancelTime();
            if (deadline != null && deadline > 0L && now < deadline) {
                computedStatus = STATUS_CANCEL_APPLY;
            } else if (deadline == null || deadline <= 0L || now >= deadline) {
                computedStatus = STATUS_CANCELED;
            }
        } else {
            computedStatus = STATUS_NORMAL;
        }

        int status = computedStatus;
        resp.setStatus(status);
        resp.setCancelApplyTime(record == null ? null : record.getCreatedTime());
        resp.setCancelDeadlineTime(record == null ? null : record.getCancelTime());
        resp.setCanApply(status == STATUS_NORMAL);
        resp.setCanRevoke(status == STATUS_CANCEL_APPLY);

        long cancelGraceDays = getCancelGraceDays();
        if (status == STATUS_CANCEL_APPLY) {
            resp.setTip("账号已提交注销申请，" + cancelGraceDays + "天内再次登录或撤销将取消注销，到期后将自动注销。");
        } else if (status == STATUS_CANCELED) {
            resp.setTip("账号已注销。");
        } else if (status == STATUS_FROZEN) {
            resp.setTip("账号已冻结。");
        } else {
            resp.setTip("可申请注销账号，注销后将进入" + cancelGraceDays + "天反悔期。");
        }
        return resp;
    }

    private void finalizeCancellation(UserAccounts account, long now) {
        if (account == null) {
            return;
        }
        account.setStatus(STATUS_CANCELED);
        account.setCancelTime(now);
        account.setUpdatedTime(now);
        account.setIsDelete(1);
        userAccountsService.updateById(account);

        // Mark cancel record as system canceled (keep it as main data).
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

    private AccountCancelRecords findActiveCancelRecord(String accountId) {
        if (!StringUtils.hasText(accountId)) {
            return null;
        }
        return accountCancelRecordsService.getOne(
            new LambdaQueryWrapper<AccountCancelRecords>()
                .eq(AccountCancelRecords::getAccountId, accountId)
                .eq(AccountCancelRecords::getCancelType, 1)
                .eq(AccountCancelRecords::getIsDelete, 0)
                .orderByDesc(AccountCancelRecords::getCreatedTime)
                .last("limit 1"),
            false
        );
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private long getCancelGraceDays() {
        Long value = systemConfigsService.getLongConfig("account.cancel_grace_days", DEFAULT_CANCEL_GRACE_DAYS);
        return value == null || value <= 0L ? DEFAULT_CANCEL_GRACE_DAYS : value;
    }

    private long getCancelGraceMillis() {
        return getCancelGraceDays() * 24L * 60L * 60L * 1000L;
    }

    private int getCancelDataRetentionDays() {
        Integer value = systemConfigsService.getIntegerConfig(
            "account.cancel_data_retention_days",
            DEFAULT_CANCEL_DATA_RETENTION_DAYS
        );
        return value == null || value <= 0 ? DEFAULT_CANCEL_DATA_RETENTION_DAYS : value;
    }
}
