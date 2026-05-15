package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 账号注销记录表
 * @TableName account_cancel_records
 */
@TableName(value ="account_cancel_records")
@Data
public class AccountCancelRecords {
    /**
     * 主键，CR+雪花ID
     */
    @TableId
    private String id;

    /**
     * 账号ID
     */
    private String accountId;

    /**
     * 注销原因
     */
    private String cancelReason;

    /**
     * 注销类型：1-用户主动，2-系统注销
     */
    private Integer cancelType;

    /**
     * 操作人ID
     */
    private String operatorId;

    /**
     * 注销时间戳
     */
    private Long cancelTime;

    /**
     * 数据保留天数
     */
    private Integer dataRetentionDays;

    /**
     * 创建时间戳
     */
    private Long createdTime;

    /**
     * 乐观锁版本号
     */
    @Version
    private Integer version;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer isDelete;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        AccountCancelRecords other = (AccountCancelRecords) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getAccountId() == null ? other.getAccountId() == null : this.getAccountId().equals(other.getAccountId()))
            && (this.getCancelReason() == null ? other.getCancelReason() == null : this.getCancelReason().equals(other.getCancelReason()))
            && (this.getCancelType() == null ? other.getCancelType() == null : this.getCancelType().equals(other.getCancelType()))
            && (this.getOperatorId() == null ? other.getOperatorId() == null : this.getOperatorId().equals(other.getOperatorId()))
            && (this.getCancelTime() == null ? other.getCancelTime() == null : this.getCancelTime().equals(other.getCancelTime()))
            && (this.getDataRetentionDays() == null ? other.getDataRetentionDays() == null : this.getDataRetentionDays().equals(other.getDataRetentionDays()))
            && (this.getCreatedTime() == null ? other.getCreatedTime() == null : this.getCreatedTime().equals(other.getCreatedTime()))
            && (this.getVersion() == null ? other.getVersion() == null : this.getVersion().equals(other.getVersion()))
            && (this.getIsDelete() == null ? other.getIsDelete() == null : this.getIsDelete().equals(other.getIsDelete()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getAccountId() == null) ? 0 : getAccountId().hashCode());
        result = prime * result + ((getCancelReason() == null) ? 0 : getCancelReason().hashCode());
        result = prime * result + ((getCancelType() == null) ? 0 : getCancelType().hashCode());
        result = prime * result + ((getOperatorId() == null) ? 0 : getOperatorId().hashCode());
        result = prime * result + ((getCancelTime() == null) ? 0 : getCancelTime().hashCode());
        result = prime * result + ((getDataRetentionDays() == null) ? 0 : getDataRetentionDays().hashCode());
        result = prime * result + ((getCreatedTime() == null) ? 0 : getCreatedTime().hashCode());
        result = prime * result + ((getVersion() == null) ? 0 : getVersion().hashCode());
        result = prime * result + ((getIsDelete() == null) ? 0 : getIsDelete().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", accountId=").append(accountId);
        sb.append(", cancelReason=").append(cancelReason);
        sb.append(", cancelType=").append(cancelType);
        sb.append(", operatorId=").append(operatorId);
        sb.append(", cancelTime=").append(cancelTime);
        sb.append(", dataRetentionDays=").append(dataRetentionDays);
        sb.append(", createdTime=").append(createdTime);
        sb.append(", version=").append(version);
        sb.append(", isDelete=").append(isDelete);
        sb.append("]");
        return sb.toString();
    }
}