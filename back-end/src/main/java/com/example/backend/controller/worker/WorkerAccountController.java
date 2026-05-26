package com.example.backend.controller.worker;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.AccountCancelRecords;
import com.example.backend.entity.Images;
import com.example.backend.entity.RepairOrders;
import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.entity.TechnicianServiceAreas;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.worker.WorkerAccountCancelStatusResponse;
import com.example.backend.model.worker.WorkerAccountInfoResponse;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.AccountCancelRecordsService;
import com.example.backend.service.ImagesService;
import com.example.backend.service.RepairOrdersService;
import com.example.backend.service.SystemConfigsService;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.service.TechnicianServiceAreasService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/worker/account")
public class WorkerAccountController {

    private static final int ACCOUNT_STATUS_FROZEN = 3;
    private static final int ACCOUNT_STATUS_LEFT = 4;

    private static final int DEFAULT_CANCEL_GRACE_DAYS = 7;
    private static final int DEFAULT_CANCEL_DATA_RETENTION_DAYS = 30;

    private final TechnicianAccountsService technicianAccountsService;
    private final TechnicianServiceAreasService technicianServiceAreasService;
    private final ImagesService imagesService;
    private final RepairOrdersService repairOrdersService;
    private final AccountCancelRecordsService accountCancelRecordsService;
    private final SystemConfigsService systemConfigsService;

    public WorkerAccountController(
        TechnicianAccountsService technicianAccountsService,
        TechnicianServiceAreasService technicianServiceAreasService,
        ImagesService imagesService,
        RepairOrdersService repairOrdersService,
        AccountCancelRecordsService accountCancelRecordsService,
        SystemConfigsService systemConfigsService
    ) {
        this.technicianAccountsService = technicianAccountsService;
        this.technicianServiceAreasService = technicianServiceAreasService;
        this.imagesService = imagesService;
        this.repairOrdersService = repairOrdersService;
        this.accountCancelRecordsService = accountCancelRecordsService;
        this.systemConfigsService = systemConfigsService;
    }

    @GetMapping("/me")
    public Result<WorkerAccountInfoResponse> getCurrentWorker() {
        LoginUserInfo user = requireWorker();
        String accountId = user.getAccountId();
        TechnicianAccounts technician = technicianAccountsService.getById(accountId);
        if (technician == null || (technician.getIsDelete() != null && technician.getIsDelete() != 0)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "师傅账号不存在");
        }

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
        if (avatarImage != null) {
            avatarUrl = avatarImage.getFileUrl();
        }

        long pendingOrderCount = repairOrdersService.count(
            new LambdaQueryWrapper<RepairOrders>()
                .eq(RepairOrders::getTechnicianAccountId, accountId)
                .eq(RepairOrders::getIsDelete, 0)
                .in(RepairOrders::getStatus, 2, 3, 4, 5)
        );

        TechnicianServiceAreas area = getDefaultArea(accountId);

        WorkerAccountInfoResponse response = new WorkerAccountInfoResponse();
        response.setId(technician.getId());
        response.setUsername(technician.getUsername());
        response.setEmail(technician.getEmail());
        response.setAccountStatus(technician.getAccountStatus());
        response.setWorkStatus(technician.getWorkStatus());
        response.setPendingOrderCount((int) pendingOrderCount);
        response.setAddress(area == null ? null : area.getCenterAddress());
        response.setLatitude(area == null ? null : area.getCenterLatitude());
        response.setLongitude(area == null ? null : area.getCenterLongitude());
        response.setAvatarUrl(avatarUrl);
        return Result.success(response);
    }

    @GetMapping("/cancel/status")
    public Result<WorkerAccountCancelStatusResponse> getCancelStatus() {
        LoginUserInfo user = requireWorker();
        String accountId = user.getAccountId();
        TechnicianAccounts account = requireActiveAccount(accountId);

        long now = System.currentTimeMillis();
        CancelState state = getCancelState(accountId, now);
        if (state.expired) {
            finalizeCancellation(account, now);
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已注销");
        }

        return Result.success(buildCancelStatusResponse(account.getAccountStatus(), state));
    }

    @PostMapping("/cancel/apply")
    public Result<WorkerAccountCancelStatusResponse> applyCancel() {
        LoginUserInfo user = requireWorker();
        String accountId = user.getAccountId();
        TechnicianAccounts account = requireActiveAccount(accountId);

        int accountStatus = safeInt(account.getAccountStatus());
        if (accountStatus == ACCOUNT_STATUS_FROZEN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已冻结，无法注销");
        }
        if (accountStatus == ACCOUNT_STATUS_LEFT) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已离职，无法注销");
        }

        long now = System.currentTimeMillis();
        CancelState state = getCancelState(accountId, now);
        if (state.expired) {
            finalizeCancellation(account, now);
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已注销");
        }
        if (state.canceling) {
            // Idempotent: already applied
            return Result.success(buildCancelStatusResponse(account.getAccountStatus(), state));
        }

        long deadline = now + getCancelGraceMillis();
        AccountCancelRecords record = new AccountCancelRecords();
        record.setId(SnowflakeIdUtil.nextAccountCancelRecordId());
        record.setAccountId(accountId);
        record.setCancelReason("师傅主动注销");
        record.setCancelType(1);
        record.setOperatorId(accountId);
        // cancel_time stores planned deadline for grace period
        record.setCancelTime(deadline);
        record.setDataRetentionDays(getCancelDataRetentionDays());
        record.setCreatedTime(now);
        record.setIsDelete(0);
        accountCancelRecordsService.save(record);

        CancelState newState = new CancelState(true, now, deadline, false);
        return Result.success(buildCancelStatusResponse(account.getAccountStatus(), newState));
    }

    @PostMapping("/cancel/revoke")
    public Result<WorkerAccountCancelStatusResponse> revokeCancel() {
        LoginUserInfo user = requireWorker();
        String accountId = user.getAccountId();
        TechnicianAccounts account = requireActiveAccount(accountId);

        long now = System.currentTimeMillis();
        CancelState state = getCancelState(accountId, now);
        if (state.expired) {
            finalizeCancellation(account, now);
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已注销");
        }
        if (!state.canceling) {
            return Result.success(buildCancelStatusResponse(account.getAccountStatus(), state));
        }

        // Mark cancel record as deleted (avoid empty SET clause).
        accountCancelRecordsService.update(
            null,
            new UpdateWrapper<AccountCancelRecords>()
                .set("is_delete", 1)
                .eq("account_id", accountId)
                .eq("cancel_type", 1)
                .eq("is_delete", 0)
        );

        CancelState revoked = new CancelState(false, null, null, false);
        return Result.success(buildCancelStatusResponse(account.getAccountStatus(), revoked));
    }

    public static class UpdateWorkStatusRequest {
        private Integer workStatus;

        public Integer getWorkStatus() {
            return workStatus;
        }

        public void setWorkStatus(Integer workStatus) {
            this.workStatus = workStatus;
        }
    }

    @PostMapping("/work-status")
    public Result<Void> updateWorkStatus(@RequestBody UpdateWorkStatusRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数不能为空");
        }
        LoginUserInfo user = requireWorker();
        Integer status = request.getWorkStatus();
        if (status == null || status < 0 || status > 3) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非法的工作状态");
        }

        String accountId = user.getAccountId();
        TechnicianAccounts technician = technicianAccountsService.getById(accountId);
        if (technician == null || (technician.getIsDelete() != null && technician.getIsDelete() != 0)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "师傅账号不存在");
        }
        if (technician.getAccountStatus() == null || technician.getAccountStatus() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, getAccountStatusBlockedMessage(technician.getAccountStatus()));
        }

        if (status == 1) {
            TechnicianServiceAreas area = getDefaultArea(accountId);
            if (!hasValidLocation(area)) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请先完成定位并填写地址后再上线");
            }
        }

        technician.setWorkStatus(status);
        technician.setUpdatedTime(System.currentTimeMillis());
        technicianAccountsService.updateById(technician);
        return Result.success();
    }

    private TechnicianServiceAreas getDefaultArea(String accountId) {
        return technicianServiceAreasService.getOne(
            new LambdaQueryWrapper<TechnicianServiceAreas>()
                .eq(TechnicianServiceAreas::getTechnicianAccountId, accountId)
                .eq(TechnicianServiceAreas::getIsDelete, 0)
                .orderByDesc(TechnicianServiceAreas::getIsDefault)
                .orderByDesc(TechnicianServiceAreas::getCreatedTime)
                .last("limit 1"),
            false
        );
    }

    private boolean hasValidLocation(TechnicianServiceAreas area) {
        if (area == null) {
            return false;
        }
        if (!StringUtils.hasText(area.getCenterAddress())) {
            return false;
        }
        BigDecimal latitude = area.getCenterLatitude();
        BigDecimal longitude = area.getCenterLongitude();
        if (latitude == null || longitude == null) {
            return false;
        }
        return latitude.compareTo(BigDecimal.valueOf(-90)) >= 0
            && latitude.compareTo(BigDecimal.valueOf(90)) <= 0
            && longitude.compareTo(BigDecimal.valueOf(-180)) >= 0
            && longitude.compareTo(BigDecimal.valueOf(180)) <= 0
            && (latitude.compareTo(BigDecimal.ZERO) != 0 || longitude.compareTo(BigDecimal.ZERO) != 0);
    }

    private LoginUserInfo requireWorker() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.WORKER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问师傅信息");
        }
        return user;
    }

    private TechnicianAccounts requireActiveAccount(String accountId) {
        if (!StringUtils.hasText(accountId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        TechnicianAccounts account = technicianAccountsService.getById(accountId);
        if (account == null || safeInt(account.getIsDelete()) != 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "师傅账号不存在");
        }
        return account;
    }

    private WorkerAccountCancelStatusResponse buildCancelStatusResponse(Integer accountStatus, CancelState state) {
        WorkerAccountCancelStatusResponse resp = new WorkerAccountCancelStatusResponse();
        boolean canceling = state != null && state.canceling;
        resp.setCanceling(canceling);
        resp.setCancelApplyTime(state == null ? null : state.applyTime);
        resp.setCancelDeadlineTime(state == null ? null : state.deadlineTime);
        boolean canApply = !canceling && safeInt(accountStatus) != ACCOUNT_STATUS_FROZEN && safeInt(accountStatus) != ACCOUNT_STATUS_LEFT;
        resp.setCanApply(canApply);
        resp.setCanRevoke(canceling);
        int cancelGraceDays = getCancelGraceDays();
        if (canceling) {
            resp.setTip("账号已提交注销申请，" + cancelGraceDays + "天内再次登录或撤销将取消注销，到期后将自动注销。");
        } else if (safeInt(accountStatus) == ACCOUNT_STATUS_FROZEN) {
            resp.setTip("账号已冻结。");
        } else if (safeInt(accountStatus) == ACCOUNT_STATUS_LEFT) {
            resp.setTip("账号已离职。");
        } else {
            resp.setTip("可申请注销账号，注销后将进入" + cancelGraceDays + "天反悔期。");
        }
        return resp;
    }

    private CancelState getCancelState(String technicianAccountId, long now) {
        AccountCancelRecords record = accountCancelRecordsService.getOne(
            new LambdaQueryWrapper<AccountCancelRecords>()
                .eq(AccountCancelRecords::getAccountId, technicianAccountId)
                .eq(AccountCancelRecords::getCancelType, 1)
                .eq(AccountCancelRecords::getIsDelete, 0)
                .orderByDesc(AccountCancelRecords::getCreatedTime)
                .last("limit 1"),
            false
        );
        if (record == null || record.getCreatedTime() == null || record.getCreatedTime() <= 0L) {
            return new CancelState(false, null, null, false);
        }
        long applyTime = record.getCreatedTime();
        Long deadlineObj = record.getCancelTime();
        long deadline = deadlineObj == null ? 0L : deadlineObj;
        // Defensive: if deadline missing, treat as expired (and let caller finalize).
        boolean expired = deadline <= 0L || now >= deadline;
        boolean canceling = !expired;
        return new CancelState(canceling, applyTime, deadline, expired);
    }

    private void finalizeCancellation(TechnicianAccounts account, long now) {
        if (account == null) {
            return;
        }
        account.setUpdatedTime(now);
        account.setIsDelete(1);
        technicianAccountsService.updateById(account);

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

    private static final class CancelState {
        private final boolean canceling;
        private final Long applyTime;
        private final Long deadlineTime;
        private final boolean expired;

        private CancelState(boolean canceling, Long applyTime, Long deadlineTime, boolean expired) {
            this.canceling = canceling;
            this.applyTime = applyTime;
            this.deadlineTime = deadlineTime;
            this.expired = expired;
        }
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private int getCancelGraceDays() {
        Integer value = systemConfigsService.getIntegerConfig("account.cancel_grace_days", DEFAULT_CANCEL_GRACE_DAYS);
        return value == null || value <= 0 ? DEFAULT_CANCEL_GRACE_DAYS : value;
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

    private String getAccountStatusBlockedMessage(Integer accountStatus) {
        if (accountStatus == null) {
            return "当前账号状态异常，无法修改工作状态";
        }
        if (accountStatus == 2) {
            return "账号未实名认证，无法修改工作状态";
        }
        if (accountStatus == 3) {
            return "账号已冻结，无法修改工作状态";
        }
        if (accountStatus == 4) {
            return "账号已离职，无法修改工作状态";
        }
        return "当前账号状态异常，无法修改工作状态";
    }
}
