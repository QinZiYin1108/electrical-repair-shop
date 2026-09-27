package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import lombok.Data;

/** 维修人员提现单。 @TableName technician_withdrawals */
@TableName("technician_withdrawals")
@Data
public class TechnicianWithdrawals {
    /** 主键，TW+雪花ID */
    @TableId private String id;

    /** 提现单号 */
    private String withdrawalNo;

    /** 维修人员账号ID */
    private String technicianAccountId;

    /** 提现金额 */
    private BigDecimal amount;

    /** 状态：1-待审核，2-已通过待打款，3-打款成功，4-打款失败已退回，5-已驳回 */
    private Integer status;

    /** 收款账户说明（脱敏快照） */
    private String payoutAccount;

    /** 客户端幂等键 */
    private String idempotencyKey;

    /** 审核管理员账号ID */
    private String reviewAdminId;

    /** 审核时间戳 */
    private Long reviewTime;

    /** 审核备注 */
    private String reviewRemark;

    /** 渠道/转账单号 */
    private String payoutProviderNo;

    /** 打款时间戳 */
    private Long payoutTime;

    /** 失败原因 */
    private String failReason;

    /** 需高金额复核：0-否，1-是 */
    private Integer requiresReview;

    /** 高金额复核已确认：0-否，1-是 */
    private Integer reviewConfirmed;

    /** 风险说明 */
    private String riskReason;

    /** 人工拦截：0-否，1-是 */
    private Integer intercepted;

    /** 拦截原因 */
    private String interceptReason;

    /** 创建时间戳 */
    private Long createdTime;

    /** 更新时间戳 */
    private Long updatedTime;

    /** 乐观锁版本号 */
    @Version private Integer version;

    /** 逻辑删除：0-未删除，1-已删除 */
    @TableLogic private Integer isDelete;
}
