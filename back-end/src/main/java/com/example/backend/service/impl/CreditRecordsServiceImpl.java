package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.*;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.CreditRecordsMapper;
import com.example.backend.service.AdminAccountsService;
import com.example.backend.service.CreditRecordsService;
import com.example.backend.service.SystemConfigsService;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.service.UserAccountsService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @description 针对表【credit_records(信用积分记录表)】的数据库操作Service实现
 */
@Service
public class CreditRecordsServiceImpl extends ServiceImpl<CreditRecordsMapper, CreditRecords>
        implements CreditRecordsService {

    /** 信用积分初始值（默认值，优先从 system_configs 读取 credit.score_initial） */
    private static final int CREDIT_INITIAL = 100;

    /** 红线值：低于此值触发封禁（默认值，优先从 system_configs 读取 credit.score_redline） */
    private static final int CREDIT_RED_LINE = 40;

    /** 警告阈值：低于此值限制部分功能（默认值，优先从 system_configs 读取 credit.score_warning） */
    private static final int CREDIT_WARNING = 60;

    private final UserAccountsService userAccountsService;
    private final TechnicianAccountsService technicianAccountsService;
    private final AdminAccountsService adminAccountsService;
    private final SystemConfigsService systemConfigsService;

    public CreditRecordsServiceImpl(
            UserAccountsService userAccountsService,
            TechnicianAccountsService technicianAccountsService,
            AdminAccountsService adminAccountsService,
            SystemConfigsService systemConfigsService) {
        this.userAccountsService = userAccountsService;
        this.technicianAccountsService = technicianAccountsService;
        this.adminAccountsService = adminAccountsService;
        this.systemConfigsService = systemConfigsService;
    }

    @Override
    public int getCreditScore(String accountId, Integer accountType) {
        if (accountType == null || accountType < 1 || accountType > 3) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的账号类型");
        }
        if (accountType == 1) {
            UserAccounts account = userAccountsService.getById(accountId);
            if (account == null) throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
            int defaultScore =
                    systemConfigsService.getIntegerConfig("credit.score_initial", CREDIT_INITIAL);
            return account.getCreditScore() != null ? account.getCreditScore() : defaultScore;
        } else if (accountType == 2) {
            TechnicianAccounts account = technicianAccountsService.getById(accountId);
            if (account == null) throw new BusinessException(ErrorCode.NOT_FOUND, "师傅不存在");
            int defaultScore =
                    systemConfigsService.getIntegerConfig("credit.score_initial", CREDIT_INITIAL);
            return account.getCreditScore() != null ? account.getCreditScore() : defaultScore;
        } else {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "门店管理员暂不支持信用积分查询");
        }
    }

    @Override
    public int changeScore(
            String accountId,
            Integer accountType,
            int scoreChange,
            Integer changeType,
            String reason,
            String relatedRecordId) {
        if (accountType == null || accountType < 1 || accountType > 3) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的账号类型");
        }

        int scoreBefore;
        int scoreAfter;

        int defaultScore =
                systemConfigsService.getIntegerConfig("credit.score_initial", CREDIT_INITIAL);
        if (accountType == 1) {
            UserAccounts account = userAccountsService.getById(accountId);
            if (account == null) throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
            scoreBefore =
                    account.getCreditScore() != null ? account.getCreditScore() : defaultScore;
            scoreAfter = Math.max(0, scoreBefore + scoreChange);
            account.setCreditScore(scoreAfter);
            userAccountsService.updateById(account);
        } else if (accountType == 2) {
            TechnicianAccounts account = technicianAccountsService.getById(accountId);
            if (account == null) throw new BusinessException(ErrorCode.NOT_FOUND, "师傅不存在");
            scoreBefore =
                    account.getCreditScore() != null ? account.getCreditScore() : defaultScore;
            scoreAfter = Math.max(0, scoreBefore + scoreChange);
            account.setCreditScore(scoreAfter);
            technicianAccountsService.updateById(account);
        } else {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "门店管理员暂不支持积分变动");
        }

        // 写信用记录
        long now = System.currentTimeMillis();
        CreditRecords record = new CreditRecords();
        record.setId(SnowflakeIdUtil.nextCreditRecordId());
        record.setAccountId(accountId);
        record.setAccountType(accountType);
        record.setChangeType(changeType);
        record.setScoreChange(scoreChange);
        record.setScoreBefore(scoreBefore);
        record.setScoreAfter(scoreAfter);
        record.setReason(reason);
        record.setRelatedRecordId(relatedRecordId);
        record.setCreatedTime(now);
        record.setIsDelete(0);
        save(record);

        return scoreAfter;
    }

    @Override
    public void checkCreditLimit(String accountId, Integer accountType, String action) {
        int score = getCreditScore(accountId, accountType);
        int redLine =
                systemConfigsService.getIntegerConfig("credit.score_redline", CREDIT_RED_LINE);
        int warning = systemConfigsService.getIntegerConfig("credit.score_warning", CREDIT_WARNING);

        if (score < redLine) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN, "您的信用积分过低（" + score + "分），账号已被限制，无法" + action);
        }
        if (score < warning) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN, "您的信用积分不足（" + score + "分），当前处于风险观察期，无法" + action);
        }
    }

    @Override
    public Page<CreditRecords> getCreditHistory(
            String accountId, Integer accountType, int page, int size) {
        LambdaQueryWrapper<CreditRecords> wrapper =
                new LambdaQueryWrapper<CreditRecords>()
                        .eq(CreditRecords::getAccountId, accountId)
                        .eq(CreditRecords::getAccountType, accountType)
                        .orderByDesc(CreditRecords::getCreatedTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public void recoverNoViolationScore(int noViolationDays, int recoveryScore) {
        if (noViolationDays <= 0 || recoveryScore <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "恢复周期和恢复分数必须大于0");
        }

        long threshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(noViolationDays);
        int maximumScore =
                systemConfigsService.getIntegerConfig("credit.score_initial", CREDIT_INITIAL);
        recoverAccounts(1, threshold, noViolationDays, recoveryScore, maximumScore);
        recoverAccounts(2, threshold, noViolationDays, recoveryScore, maximumScore);
    }

    private void recoverAccounts(
            int accountType,
            long threshold,
            int noViolationDays,
            int recoveryScore,
            int maximumScore) {
        List<CreditRecords> violations =
                list(
                        new LambdaQueryWrapper<CreditRecords>()
                                .eq(CreditRecords::getChangeType, 1)
                                .eq(CreditRecords::getAccountType, accountType)
                                .orderByDesc(CreditRecords::getCreatedTime));

        Set<String> processedAccounts = new HashSet<>();
        for (CreditRecords violation : violations) {
            if (!processedAccounts.add(violation.getAccountId())) {
                continue;
            }
            if (violation.getCreatedTime() == null || violation.getCreatedTime() >= threshold) {
                continue;
            }
            if (hasRecoveryForViolation(violation.getAccountId(), accountType, violation.getId())) {
                continue;
            }

            int currentScore = getCreditScore(violation.getAccountId(), accountType);
            int actualRecovery = Math.min(recoveryScore, maximumScore - currentScore);
            if (actualRecovery <= 0) {
                continue;
            }
            changeScore(
                    violation.getAccountId(),
                    accountType,
                    actualRecovery,
                    2,
                    noViolationDays + "天无新增违规，自动恢复" + actualRecovery + "分",
                    violation.getId());
        }
    }

    boolean hasRecoveryForViolation(String accountId, int accountType, String violationId) {
        return count(
                        new LambdaQueryWrapper<CreditRecords>()
                                .eq(CreditRecords::getAccountId, accountId)
                                .eq(CreditRecords::getAccountType, accountType)
                                .eq(CreditRecords::getChangeType, 2)
                                .eq(CreditRecords::getRelatedRecordId, violationId))
                > 0;
    }

    @Override
    public Long getLastViolationTime(String accountId, Integer accountType) {
        LambdaQueryWrapper<CreditRecords> wrapper =
                new LambdaQueryWrapper<CreditRecords>()
                        .eq(CreditRecords::getAccountId, accountId)
                        .eq(CreditRecords::getAccountType, accountType)
                        .eq(CreditRecords::getChangeType, 1) // 违规扣分
                        .orderByDesc(CreditRecords::getCreatedTime)
                        .last("LIMIT 1");
        CreditRecords record = getOne(wrapper);
        return record != null ? record.getCreatedTime() : null;
    }
}
