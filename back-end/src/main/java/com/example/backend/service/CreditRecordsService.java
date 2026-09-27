package com.example.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.entity.CreditRecords;

/**
 * @description 针对表【credit_records(信用积分记录表)】的数据库操作Service
 */
public interface CreditRecordsService extends IService<CreditRecords> {

    /**
     * 获取账号当前信用积分
     *
     * @param accountId 账号ID
     * @param accountType 账号类型：1-用户，2-师傅，3-门店管理员
     * @return 当前积分
     */
    int getCreditScore(String accountId, Integer accountType);

    /**
     * 扣减信用积分并记录
     *
     * @param accountId 账号ID
     * @param accountType 账号类型
     * @param scoreChange 变动值（正数为恢复，负数为扣分）
     * @param changeType 变动类型：1-违规扣分，2-自动恢复，3-举报奖励，4-完单恢复，5-申诉恢复
     * @param reason 变动原因
     * @param relatedRecordId 关联记录ID
     * @return 变动后的积分
     */
    int changeScore(
            String accountId,
            Integer accountType,
            int scoreChange,
            Integer changeType,
            String reason,
            String relatedRecordId);

    /**
     * 信用红线校验：检查账号是否可以执行指定操作
     *
     * @param accountId 账号ID
     * @param accountType 账号类型
     * @param action 操作标识（用于日志和提示）
     * @throws com.example.backend.exception.BusinessException 信用不足时抛出
     */
    void checkCreditLimit(String accountId, Integer accountType, String action);

    /**
     * 查询积分变动历史
     *
     * @param accountId 账号ID
     * @param accountType 账号类型
     * @param page 页码
     * @param size 每页条数
     * @return 分页结果
     */
    Page<CreditRecords> getCreditHistory(String accountId, Integer accountType, int page, int size);

    /**
     * 扫描并恢复长期无违规账号的积分（定时任务用）
     *
     * @param noViolationDays 无违规天数阈值（默认30）
     * @param recoveryScore 恢复积分值（默认5）
     */
    void recoverNoViolationScore(int noViolationDays, int recoveryScore);

    /**
     * 获取近期最后一条违规扣分记录
     *
     * @param accountId 账号ID
     * @param accountType 账号类型
     * @return 最近违规扣分记录的创建时间，若无则返回null
     */
    Long getLastViolationTime(String accountId, Integer accountType);
}
