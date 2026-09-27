package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.PenaltyRecords;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.PenaltyRecordsMapper;
import com.example.backend.service.CreditRecordsService;
import com.example.backend.service.PenaltyAccountEnforcementService;
import com.example.backend.service.PenaltyRecordsService;
import com.example.backend.service.SystemConfigsService;
import com.example.backend.service.SystemMessagesService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @description 针对表【penalty_records(处罚记录表)】的数据库操作Service实现
 */
@Service
public class PenaltyRecordsServiceImpl extends ServiceImpl<PenaltyRecordsMapper, PenaltyRecords>
        implements PenaltyRecordsService {

    /** 四级违规对应扣分默认值：索引 0-3 对应 1-4 级（优先从 system_configs 读取 credit.deduct_level1~4） */
    private static final int[] SCORE_MAP_DEFAULT = {5, 10, 20, 40};

    /** 信用红线默认值（优先从 system_configs 读取 credit.score_redline） */
    private static final int RED_LINE_DEFAULT = 40;

    private final CreditRecordsService creditRecordsService;
    private final SystemMessagesService systemMessagesService;
    private final SystemConfigsService systemConfigsService;
    private final PenaltyAccountEnforcementService penaltyAccountEnforcementService;

    public PenaltyRecordsServiceImpl(
            CreditRecordsService creditRecordsService,
            SystemMessagesService systemMessagesService,
            SystemConfigsService systemConfigsService,
            PenaltyAccountEnforcementService penaltyAccountEnforcementService) {
        this.creditRecordsService = creditRecordsService;
        this.systemMessagesService = systemMessagesService;
        this.systemConfigsService = systemConfigsService;
        this.penaltyAccountEnforcementService = penaltyAccountEnforcementService;
    }

    @Override
    @Transactional
    public PenaltyRecords executePenalty(
            String accountId,
            Integer accountType,
            String reportId,
            Integer violationLevel,
            Integer penaltyType,
            Integer banDurationHours,
            String restrictedFunctions,
            String operatorId,
            String remark) {
        // 1. 参数校验
        if (accountId == null
                || accountType == null
                || violationLevel == null
                || penaltyType == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "处罚参数不完整");
        }
        if (violationLevel < 1 || violationLevel > 4) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "违规等级必须在1-4之间");
        }
        if (accountType < 1 || accountType > 3 || penaltyType < 1 || penaltyType > 3) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号类型或处罚类型无效");
        }
        if (penaltyType == 3 && banDurationHours != null && banDurationHours <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "封禁时长必须大于0，永久封禁请留空");
        }

        int scoreDeducted =
                systemConfigsService.getIntegerConfig(
                        "credit.deduct_level" + violationLevel,
                        SCORE_MAP_DEFAULT[violationLevel - 1]);

        // 2. 写处罚记录
        long now = System.currentTimeMillis();
        PenaltyRecords penalty = new PenaltyRecords();
        penalty.setId(SnowflakeIdUtil.nextPenaltyRecordId());
        penalty.setAccountId(accountId);
        penalty.setAccountType(accountType);
        penalty.setReportId(reportId);
        penalty.setViolationLevel(violationLevel);
        penalty.setScoreDeducted(scoreDeducted);
        penalty.setPenaltyType(penaltyType);
        penalty.setRestrictedFunctions(restrictedFunctions);
        if (penaltyType == 3) {
            penalty.setPreviousAccountStatus(
                    penaltyAccountEnforcementService.freezeAccount(accountId, accountType));
        }
        penalty.setOperatorId(operatorId);
        penalty.setRemark(remark);
        penalty.setAppealStatus(0); // 未申诉
        penalty.setStatus(1); // 执行中

        if (penaltyType == 3) {
            // 封禁
            penalty.setBanDurationHours(banDurationHours);
            penalty.setBanStartTime(now);
            if (banDurationHours != null && banDurationHours > 0) {
                penalty.setBanEndTime(now + banDurationHours * 3600_000L);
            }
        }

        penalty.setCreatedTime(now);
        penalty.setUpdatedTime(now);
        penalty.setVersion(1);
        penalty.setIsDelete(0);
        save(penalty);

        // 3. 扣减信用积分
        int scoreAfter =
                creditRecordsService.changeScore(
                        accountId,
                        accountType,
                        -scoreDeducted,
                        1,
                        "违规处罚：等级" + violationLevel + "，扣" + scoreDeducted + "分",
                        penalty.getId());

        // 4. 发系统消息通知
        String levelName = getLevelName(violationLevel);
        String typeName = getPenaltyTypeName(penaltyType);
        systemMessagesService.createSystemMessage(
                accountId,
                accountType,
                "处罚通知",
                "您因"
                        + levelName
                        + "违规被处以"
                        + typeName
                        + "，扣除"
                        + scoreDeducted
                        + "分，当前积分："
                        + scoreAfter
                        + "分",
                3, // 系统通知
                "penalty",
                penalty.getId(),
                2); // 高优先级

        return penalty;
    }

    @Override
    public Page<PenaltyRecords> listPenalties(
            int page,
            int size,
            Integer status,
            Integer violationLevel,
            Integer appealStatus,
            Long startTime,
            Long endTime) {
        LambdaQueryWrapper<PenaltyRecords> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(PenaltyRecords::getStatus, status);
        }
        if (violationLevel != null) {
            wrapper.eq(PenaltyRecords::getViolationLevel, violationLevel);
        }
        if (appealStatus != null) {
            wrapper.eq(PenaltyRecords::getAppealStatus, appealStatus);
        }
        if (startTime != null) {
            wrapper.ge(PenaltyRecords::getCreatedTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(PenaltyRecords::getCreatedTime, endTime);
        }
        wrapper.orderByDesc(PenaltyRecords::getCreatedTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public PenaltyRecords getPenaltyDetail(String id) {
        PenaltyRecords penalty = getById(id);
        if (penalty == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "处罚记录不存在");
        }
        return penalty;
    }

    @Override
    public PenaltyRecords submitAppeal(String penaltyId, String appealReason) {
        PenaltyRecords penalty = getById(penaltyId);
        if (penalty == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "处罚记录不存在");
        }
        if (penalty.getAppealStatus() != null && penalty.getAppealStatus() != 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "已申诉过的处罚记录不可再次申诉");
        }
        if (penalty.getStatus() != null && penalty.getStatus() != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "处罚已结束，无法申诉");
        }

        long now = System.currentTimeMillis();
        penalty.setAppealStatus(1); // 申诉中
        penalty.setAppealReason(appealReason);
        penalty.setAppealTime(now);
        penalty.setUpdatedTime(now);
        updateById(penalty);
        return penalty;
    }

    @Override
    @Transactional
    public PenaltyRecords processAppeal(
            String penaltyId, boolean approved, String result, String operatorId) {
        PenaltyRecords penalty = getById(penaltyId);
        if (penalty == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "处罚记录不存在");
        }
        if (penalty.getAppealStatus() == null || penalty.getAppealStatus() != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该处罚当前无可处理的申诉");
        }

        long now = System.currentTimeMillis();
        penalty.setAppealResult(result);
        penalty.setUpdatedTime(now);

        if (approved) {
            penalty.setAppealStatus(2); // 申诉通过
            // 恢复积分
            int scoreAfter =
                    creditRecordsService.changeScore(
                            penalty.getAccountId(),
                            penalty.getAccountType(),
                            penalty.getScoreDeducted(),
                            5, // 申诉恢复
                            "申诉通过，恢复" + penalty.getScoreDeducted() + "分",
                            penalty.getId());

            // 仅当恢复后积分 >= 红线值时才解除处罚；否则积分仍低，保持功能限制
            int redLine =
                    systemConfigsService.getIntegerConfig("credit.score_redline", RED_LINE_DEFAULT);
            if (scoreAfter >= redLine) {
                penalty.setStatus(2); // 已解除
                if (penalty.getPenaltyType() != null && penalty.getPenaltyType() == 3) {
                    penalty.setBanEndTime(now); // 提前结束封禁
                }
                restoreAccountIfNoActiveBan(penalty);
            }

            String notifyContent = "您的申诉已通过，" + penalty.getScoreDeducted() + "分已恢复";
            if (scoreAfter < redLine) {
                notifyContent += "，但当前积分（" + scoreAfter + "分）仍低于红线，功能限制继续执行";
            }
            // 通知
            systemMessagesService.createSystemMessage(
                    penalty.getAccountId(),
                    penalty.getAccountType(),
                    "申诉结果",
                    notifyContent,
                    3,
                    "appeal",
                    penalty.getId(),
                    1);
        } else {
            penalty.setAppealStatus(3); // 申诉驳回
            systemMessagesService.createSystemMessage(
                    penalty.getAccountId(),
                    penalty.getAccountType(),
                    "申诉结果",
                    "您的申诉已被驳回：" + (result != null ? result : ""),
                    3,
                    "appeal",
                    penalty.getId(),
                    1);
        }

        updateById(penalty);
        return penalty;
    }

    @Override
    @Transactional
    public PenaltyRecords liftPenalty(String penaltyId, String operatorId) {
        PenaltyRecords penalty = getById(penaltyId);
        if (penalty == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "处罚记录不存在");
        }
        if (penalty.getStatus() != null && penalty.getStatus() != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该处罚已结束，无需重复解除");
        }

        long now = System.currentTimeMillis();
        penalty.setStatus(2); // 已解除
        if (penalty.getPenaltyType() != null && penalty.getPenaltyType() == 3) {
            penalty.setBanEndTime(now);
        }
        penalty.setUpdatedTime(now);
        updateById(penalty);
        restoreAccountIfNoActiveBan(penalty);
        return penalty;
    }

    @Override
    @Transactional
    public int expireBans(long now) {
        java.util.List<PenaltyRecords> expiredBans =
                list(
                        new LambdaQueryWrapper<PenaltyRecords>()
                                .eq(PenaltyRecords::getPenaltyType, 3)
                                .eq(PenaltyRecords::getStatus, 1)
                                .lt(PenaltyRecords::getBanEndTime, now)
                                .isNotNull(PenaltyRecords::getBanEndTime));
        for (PenaltyRecords penalty : expiredBans) {
            penalty.setStatus(3);
            penalty.setUpdatedTime(now);
            updateById(penalty);
            restoreAccountIfNoActiveBan(penalty);
        }
        return expiredBans.size();
    }

    private void restoreAccountIfNoActiveBan(PenaltyRecords releasedPenalty) {
        if (releasedPenalty.getPenaltyType() == null || releasedPenalty.getPenaltyType() != 3) {
            return;
        }
        long activeBanCount =
                count(
                        new LambdaQueryWrapper<PenaltyRecords>()
                                .eq(PenaltyRecords::getAccountId, releasedPenalty.getAccountId())
                                .eq(
                                        PenaltyRecords::getAccountType,
                                        releasedPenalty.getAccountType())
                                .eq(PenaltyRecords::getPenaltyType, 3)
                                .eq(PenaltyRecords::getStatus, 1)
                                .ne(PenaltyRecords::getId, releasedPenalty.getId()));
        if (activeBanCount > 0) {
            return;
        }

        Integer previousStatus = releasedPenalty.getPreviousAccountStatus();
        if (previousStatus == null) {
            PenaltyRecords origin =
                    getOne(
                            new LambdaQueryWrapper<PenaltyRecords>()
                                    .eq(
                                            PenaltyRecords::getAccountId,
                                            releasedPenalty.getAccountId())
                                    .eq(
                                            PenaltyRecords::getAccountType,
                                            releasedPenalty.getAccountType())
                                    .eq(PenaltyRecords::getPenaltyType, 3)
                                    .isNotNull(PenaltyRecords::getPreviousAccountStatus)
                                    .orderByDesc(PenaltyRecords::getBanStartTime)
                                    .last("LIMIT 1"));
            previousStatus = origin == null ? null : origin.getPreviousAccountStatus();
        }
        penaltyAccountEnforcementService.restoreAccount(
                releasedPenalty.getAccountId(), releasedPenalty.getAccountType(), previousStatus);
    }

    private String getLevelName(int level) {
        switch (level) {
            case 1:
                return "一级（轻微）";
            case 2:
                return "二级（一般）";
            case 3:
                return "三级（严重）";
            case 4:
                return "四级（重大）";
            default:
                return "未知";
        }
    }

    private String getPenaltyTypeName(int type) {
        switch (type) {
            case 1:
                return "警告";
            case 2:
                return "功能限制";
            case 3:
                return "封禁";
            default:
                return "未知";
        }
    }
}
