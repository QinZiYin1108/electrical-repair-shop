package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.AccountBalances;
import com.example.backend.entity.FundFlows;
import com.example.backend.entity.TechnicianWithdrawals;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.TechnicianWithdrawalsMapper;
import com.example.backend.model.worker.TechnicianWithdrawalApplyRequest;
import com.example.backend.service.AccountBalancesService;
import com.example.backend.service.FundFlowsService;
import com.example.backend.service.NotificationOutboxService;
import com.example.backend.service.TechnicianWithdrawalService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TechnicianWithdrawalServiceImpl
        extends ServiceImpl<TechnicianWithdrawalsMapper, TechnicianWithdrawals>
        implements TechnicianWithdrawalService {

    static final int STATUS_APPLIED = 1;
    static final int STATUS_APPROVED = 2;
    static final int STATUS_PAID = 3;
    static final int STATUS_FAILED = 4;
    static final int STATUS_REJECTED = 5;

    private static final int ACCOUNT_TYPE_TECHNICIAN = 2;
    private static final int RECEIVER_TYPE_TECHNICIAN = 2;
    private static final int FLOW_TYPE_INCOME = 1;
    private static final int FLOW_TYPE_EXPENSE = 2;
    private static final String BT_APPLY = "TECHNICIAN_WITHDRAW";
    private static final String BT_REFUND = "TECHNICIAN_WITHDRAW_REFUND";
    private static final String BIZ_TYPE_WITHDRAWAL = "TECHNICIAN_WITHDRAWAL";

    @Value("${withdrawal.review-threshold:5000}")
    private BigDecimal reviewThreshold = new BigDecimal("5000");

    @Value("${withdrawal.daily-limit:50000}")
    private BigDecimal dailyLimit = new BigDecimal("50000");

    private final AccountBalancesService accountBalancesService;
    private final FundFlowsService fundFlowsService;
    private final NotificationOutboxService notificationOutboxService;

    public TechnicianWithdrawalServiceImpl(
            AccountBalancesService accountBalancesService,
            FundFlowsService fundFlowsService,
            NotificationOutboxService notificationOutboxService) {
        this.accountBalancesService = accountBalancesService;
        this.fundFlowsService = fundFlowsService;
        this.notificationOutboxService = notificationOutboxService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TechnicianWithdrawals apply(
            String technicianAccountId, TechnicianWithdrawalApplyRequest request) {
        if (!StringUtils.hasText(technicianAccountId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (request == null || request.getAmount() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "提现金额不能为空");
        }
        BigDecimal amount = normalizeMoney(request.getAmount());
        if (amount.compareTo(new BigDecimal("0.01")) < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "提现金额需不小于 0.01 元");
        }
        String idempotencyKey = trimToNull(request.getIdempotencyKey());
        if (idempotencyKey != null) {
            TechnicianWithdrawals existing =
                    getOne(
                            new LambdaQueryWrapper<TechnicianWithdrawals>()
                                    .eq(TechnicianWithdrawals::getIdempotencyKey, idempotencyKey)
                                    .eq(TechnicianWithdrawals::getIsDelete, 0)
                                    .last("limit 1"),
                            false);
            if (existing != null) {
                return existing;
            }
        }

        assertWithinDailyLimit(technicianAccountId, amount);
        boolean requiresReview =
                reviewThreshold != null
                        && reviewThreshold.compareTo(BigDecimal.ZERO) > 0
                        && amount.compareTo(reviewThreshold) >= 0;

        long now = System.currentTimeMillis();
        AccountBalances balance = lockBalance(technicianAccountId);
        BigDecimal availableBefore = normalizeMoney(balance.getBalance());
        if (availableBefore.compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "可提现余额不足");
        }
        BigDecimal availableAfter = availableBefore.subtract(amount);
        balance.setBalance(availableAfter);
        balance.setFrozenBalance(normalizeMoney(balance.getFrozenBalance()).add(amount));
        balance.setUpdatedTime(now);
        if (!accountBalancesService.updateById(balance)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新余额失败");
        }

        TechnicianWithdrawals withdrawal = new TechnicianWithdrawals();
        withdrawal.setId(SnowflakeIdUtil.nextWithdrawalId());
        withdrawal.setWithdrawalNo("WD" + withdrawal.getId().substring(2));
        withdrawal.setTechnicianAccountId(technicianAccountId);
        withdrawal.setAmount(amount);
        withdrawal.setStatus(STATUS_APPLIED);
        withdrawal.setRequiresReview(requiresReview ? 1 : 0);
        withdrawal.setReviewConfirmed(0);
        withdrawal.setIntercepted(0);
        if (requiresReview) {
            withdrawal.setRiskReason("单笔金额达到复核阈值");
        }
        withdrawal.setPayoutAccount(trimToNull(request.getPayoutAccount()));
        withdrawal.setIdempotencyKey(idempotencyKey);
        withdrawal.setCreatedTime(now);
        withdrawal.setUpdatedTime(now);
        withdrawal.setVersion(0);
        withdrawal.setIsDelete(0);
        try {
            if (!save(withdrawal)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建提现单失败");
            }
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.DUPLICATE_KEY, "提现申请已存在");
        }

        saveFlow(
                technicianAccountId,
                FLOW_TYPE_EXPENSE,
                amount,
                availableBefore,
                availableAfter,
                BT_APPLY,
                withdrawal.getId(),
                "TECH_WITHDRAW:" + withdrawal.getId(),
                "提现申请（冻结可提现）",
                now);
        return withdrawal;
    }

    @Override
    public List<TechnicianWithdrawals> listForTechnician(String technicianAccountId, int limit) {
        if (!StringUtils.hasText(technicianAccountId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return list(
                new LambdaQueryWrapper<TechnicianWithdrawals>()
                        .eq(TechnicianWithdrawals::getTechnicianAccountId, technicianAccountId)
                        .eq(TechnicianWithdrawals::getIsDelete, 0)
                        .orderByDesc(TechnicianWithdrawals::getCreatedTime)
                        .last("limit " + clampLimit(limit)));
    }

    @Override
    public List<TechnicianWithdrawals> listForAdmin(Integer status, int limit) {
        LambdaQueryWrapper<TechnicianWithdrawals> wrapper =
                new LambdaQueryWrapper<TechnicianWithdrawals>()
                        .eq(TechnicianWithdrawals::getIsDelete, 0)
                        .orderByDesc(TechnicianWithdrawals::getCreatedTime)
                        .last("limit " + clampLimit(limit));
        if (status != null) {
            wrapper.eq(TechnicianWithdrawals::getStatus, status);
        }
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TechnicianWithdrawals review(String id, String adminId, boolean approve, String remark) {
        TechnicianWithdrawals withdrawal = lockWithdrawal(id);
        if (!Integer.valueOf(STATUS_APPLIED).equals(withdrawal.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "仅待审核的提现单可审核");
        }
        long now = System.currentTimeMillis();
        withdrawal.setReviewAdminId(adminId);
        withdrawal.setReviewTime(now);
        withdrawal.setReviewRemark(trimToNull(remark));
        withdrawal.setUpdatedTime(now);
        if (approve) {
            withdrawal.setStatus(STATUS_APPROVED);
        } else {
            withdrawal.setStatus(STATUS_REJECTED);
            refundFrozen(withdrawal, now);
        }
        if (!updateById(withdrawal)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新提现单失败");
        }
        if (approve) {
            notifyTechnician(
                    "WITHDRAWAL_APPROVED",
                    withdrawal,
                    "提现申请已通过",
                    "您的提现申请（单号 "
                            + withdrawal.getWithdrawalNo()
                            + "，金额 "
                            + withdrawal.getAmount()
                            + " 元）已通过审核，等待打款。");
        } else {
            notifyTechnician(
                    "WITHDRAWAL_REJECTED",
                    withdrawal,
                    "提现申请已驳回",
                    "您的提现申请（单号 " + withdrawal.getWithdrawalNo() + "）已驳回，冻结金额已退回可提现余额。");
        }
        return withdrawal;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TechnicianWithdrawals markPaid(String id, String adminId, String providerNo) {
        TechnicianWithdrawals withdrawal = lockWithdrawal(id);
        if (!Integer.valueOf(STATUS_APPROVED).equals(withdrawal.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "仅已通过的提现单可打款");
        }
        if (Integer.valueOf(1).equals(withdrawal.getIntercepted())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该提现单已被人工拦截，无法打款");
        }
        if (Integer.valueOf(1).equals(withdrawal.getRequiresReview())
                && !Integer.valueOf(1).equals(withdrawal.getReviewConfirmed())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "高金额提现需复核确认后方可打款");
        }
        long now = System.currentTimeMillis();
        AccountBalances balance = lockBalance(withdrawal.getTechnicianAccountId());
        BigDecimal frozenAfter =
                normalizeMoney(balance.getFrozenBalance()).subtract(withdrawal.getAmount());
        if (frozenAfter.compareTo(BigDecimal.ZERO) < 0) {
            frozenAfter = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        balance.setFrozenBalance(frozenAfter);
        balance.setUpdatedTime(now);
        if (!accountBalancesService.updateById(balance)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新余额失败");
        }
        withdrawal.setStatus(STATUS_PAID);
        withdrawal.setPayoutProviderNo(trimToNull(providerNo));
        withdrawal.setPayoutTime(now);
        withdrawal.setUpdatedTime(now);
        if (!updateById(withdrawal)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新提现单失败");
        }
        notifyTechnician(
                "WITHDRAWAL_PAID",
                withdrawal,
                "提现已打款",
                "您的提现申请（单号 "
                        + withdrawal.getWithdrawalNo()
                        + "，金额 "
                        + withdrawal.getAmount()
                        + " 元）已打款成功。");
        return withdrawal;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TechnicianWithdrawals markFailed(String id, String adminId, String reason) {
        TechnicianWithdrawals withdrawal = lockWithdrawal(id);
        if (!Integer.valueOf(STATUS_APPROVED).equals(withdrawal.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "仅已通过的提现单可标记失败");
        }
        long now = System.currentTimeMillis();
        refundFrozen(withdrawal, now);
        withdrawal.setStatus(STATUS_FAILED);
        withdrawal.setFailReason(trimToNull(reason));
        withdrawal.setUpdatedTime(now);
        if (!updateById(withdrawal)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新提现单失败");
        }
        notifyTechnician(
                "WITHDRAWAL_FAILED",
                withdrawal,
                "提现打款失败",
                "您的提现申请（单号 " + withdrawal.getWithdrawalNo() + "）打款失败，冻结金额已退回可提现余额。");
        return withdrawal;
    }

    private void refundFrozen(TechnicianWithdrawals withdrawal, long now) {
        AccountBalances balance = lockBalance(withdrawal.getTechnicianAccountId());
        BigDecimal availableBefore = normalizeMoney(balance.getBalance());
        BigDecimal frozenBefore = normalizeMoney(balance.getFrozenBalance());
        BigDecimal amount = normalizeMoney(withdrawal.getAmount());
        BigDecimal frozenAfter = frozenBefore.subtract(amount);
        if (frozenAfter.compareTo(BigDecimal.ZERO) < 0) {
            frozenAfter = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal availableAfter = availableBefore.add(amount);
        balance.setFrozenBalance(frozenAfter);
        balance.setBalance(availableAfter);
        balance.setUpdatedTime(now);
        if (!accountBalancesService.updateById(balance)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退回提现金额失败");
        }
        saveFlow(
                withdrawal.getTechnicianAccountId(),
                FLOW_TYPE_INCOME,
                amount,
                availableBefore,
                availableAfter,
                BT_REFUND,
                withdrawal.getId(),
                "TECH_WITHDRAW_REFUND:" + withdrawal.getId(),
                "提现失败/驳回，退回可提现",
                now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TechnicianWithdrawals confirmReview(String id, String adminId) {
        TechnicianWithdrawals withdrawal = lockWithdrawal(id);
        if (!Integer.valueOf(STATUS_APPROVED).equals(withdrawal.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "仅已通过的提现单可复核");
        }
        if (!Integer.valueOf(1).equals(withdrawal.getRequiresReview())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该提现单无需复核");
        }
        withdrawal.setReviewConfirmed(1);
        withdrawal.setUpdatedTime(System.currentTimeMillis());
        if (!updateById(withdrawal)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新提现单失败");
        }
        return withdrawal;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TechnicianWithdrawals intercept(String id, String adminId, String reason) {
        TechnicianWithdrawals withdrawal = lockWithdrawal(id);
        if (Integer.valueOf(STATUS_PAID).equals(withdrawal.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "已打款的提现单不可拦截");
        }
        withdrawal.setIntercepted(1);
        withdrawal.setInterceptReason(trimToNull(reason));
        withdrawal.setUpdatedTime(System.currentTimeMillis());
        if (!updateById(withdrawal)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新提现单失败");
        }
        return withdrawal;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TechnicianWithdrawals releaseIntercept(String id, String adminId) {
        TechnicianWithdrawals withdrawal = lockWithdrawal(id);
        withdrawal.setIntercepted(0);
        withdrawal.setInterceptReason(null);
        withdrawal.setUpdatedTime(System.currentTimeMillis());
        if (!updateById(withdrawal)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新提现单失败");
        }
        return withdrawal;
    }

    /** 通过可靠通知 outbox 向师傅投递站内通知。 */
    private void notifyTechnician(
            String eventType, TechnicianWithdrawals withdrawal, String title, String content) {
        notificationOutboxService.enqueueInApp(
                eventType,
                withdrawal.getTechnicianAccountId(),
                RECEIVER_TYPE_TECHNICIAN,
                title,
                content,
                BIZ_TYPE_WITHDRAWAL,
                withdrawal.getId(),
                eventType + ":" + withdrawal.getId());
    }

    private void saveFlow(
            String accountId,
            int flowType,
            BigDecimal amount,
            BigDecimal before,
            BigDecimal after,
            String businessType,
            String businessId,
            String idempotencyKey,
            String description,
            long now) {
        FundFlows flow = new FundFlows();
        flow.setId(SnowflakeIdUtil.nextFundFlowId());
        flow.setAccountId(accountId);
        flow.setAccountType(ACCOUNT_TYPE_TECHNICIAN);
        flow.setFlowType(flowType);
        flow.setAmount(amount);
        flow.setBalanceBefore(before);
        flow.setBalanceAfter(after);
        flow.setBusinessType(businessType);
        flow.setBusinessId(businessId);
        flow.setIdempotencyKey(idempotencyKey);
        flow.setDescription(description);
        flow.setCreatedTime(now);
        flow.setVersion(0);
        flow.setIsDelete(0);
        try {
            if (!fundFlowsService.save(flow)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存资金流水失败");
            }
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.DUPLICATE_KEY, "提现流水已存在");
        }
    }

    private TechnicianWithdrawals lockWithdrawal(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "提现单ID不能为空");
        }
        TechnicianWithdrawals withdrawal =
                getOne(
                        new LambdaQueryWrapper<TechnicianWithdrawals>()
                                .eq(TechnicianWithdrawals::getId, id)
                                .eq(TechnicianWithdrawals::getIsDelete, 0)
                                .last("limit 1 for update"),
                        false);
        if (withdrawal == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "提现单不存在");
        }
        return withdrawal;
    }

    private AccountBalances lockBalance(String technicianAccountId) {
        AccountBalances balance =
                accountBalancesService.getOne(
                        new LambdaQueryWrapper<AccountBalances>()
                                .eq(AccountBalances::getAccountId, technicianAccountId)
                                .eq(AccountBalances::getAccountType, ACCOUNT_TYPE_TECHNICIAN)
                                .eq(AccountBalances::getIsDelete, 0)
                                .last("limit 1 for update"),
                        false);
        if (balance == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "可提现余额不足");
        }
        return balance;
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void assertWithinDailyLimit(String technicianAccountId, BigDecimal amount) {
        if (dailyLimit == null || dailyLimit.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        long startOfDay = startOfToday();
        List<TechnicianWithdrawals> todayList =
                list(
                        new LambdaQueryWrapper<TechnicianWithdrawals>()
                                .eq(
                                        TechnicianWithdrawals::getTechnicianAccountId,
                                        technicianAccountId)
                                .eq(TechnicianWithdrawals::getIsDelete, 0)
                                .in(
                                        TechnicianWithdrawals::getStatus,
                                        STATUS_APPLIED,
                                        STATUS_APPROVED,
                                        STATUS_PAID)
                                .ge(TechnicianWithdrawals::getCreatedTime, startOfDay));
        BigDecimal used = BigDecimal.ZERO;
        if (todayList != null) {
            for (TechnicianWithdrawals item : todayList) {
                used = used.add(normalizeMoney(item.getAmount()));
            }
        }
        if (used.add(amount).compareTo(dailyLimit) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "超过当日提现限额");
        }
    }

    private long startOfToday() {
        return java.time.LocalDate.now()
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }

    private int clampLimit(int limit) {
        return Math.min(Math.max(limit, 1), 100);
    }
}
