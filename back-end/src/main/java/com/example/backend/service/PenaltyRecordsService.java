package com.example.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.entity.PenaltyRecords;

/**
 * @description 针对表【penalty_records(处罚记录表)】的数据库操作Service
 */
public interface PenaltyRecordsService extends IService<PenaltyRecords> {

    /**
     * 执行处罚
     *
     * @param accountId 被处罚账号ID
     * @param accountType 账号类型：1-用户，2-师傅，3-门店管理员
     * @param reportId 关联举报ID（可为空）
     * @param violationLevel 违规等级：1-4
     * @param penaltyType 处罚类型：1-警告，2-功能限制，3-封禁
     * @param banDurationHours 封禁时长（小时），仅 penaltyType=3 时有效
     * @param restrictedFunctions 限制功能列表JSON，仅 penaltyType=2 时有效
     * @param operatorId 操作人ID
     * @param remark 备注
     * @return 处罚记录
     */
    PenaltyRecords executePenalty(
            String accountId,
            Integer accountType,
            String reportId,
            Integer violationLevel,
            Integer penaltyType,
            Integer banDurationHours,
            String restrictedFunctions,
            String operatorId,
            String remark);

    /** 管理端分页查询处罚列表 */
    Page<PenaltyRecords> listPenalties(
            int page,
            int size,
            Integer status,
            Integer violationLevel,
            Integer appealStatus,
            Long startTime,
            Long endTime);

    /** 获取处罚详情 */
    PenaltyRecords getPenaltyDetail(String id);

    /** 提交申诉 */
    PenaltyRecords submitAppeal(String penaltyId, String appealReason);

    /** 处理申诉 */
    PenaltyRecords processAppeal(
            String penaltyId, boolean approved, String result, String operatorId);

    /** 解除处罚 */
    PenaltyRecords liftPenalty(String penaltyId, String operatorId);

    /** 将到期封禁逐条失效，并同步恢复符合条件的账号状态。 */
    int expireBans(long now);
}
