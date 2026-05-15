package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 维修订单故障记录表
 * @TableName repair_order_faults
 */
@TableName(value ="repair_order_faults")
@Data
public class RepairOrderFaults {
    /**
     * 主键，ROF+雪花ID
     */
    @TableId
    private String id;

    /**
     * 维修订单ID
     */
    private String repairOrderId;

    /**
     * 故障现象ID
     */
    private String faultPhenomenonId;

    /**
     * 故障描述
     */
    private String faultDescription;

    /**
     * 创建时间戳
     */
    private Long createdTime;

    /**
     * 更新时间戳
     */
    private Long updatedTime;

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
        RepairOrderFaults other = (RepairOrderFaults) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getRepairOrderId() == null ? other.getRepairOrderId() == null : this.getRepairOrderId().equals(other.getRepairOrderId()))
            && (this.getFaultPhenomenonId() == null ? other.getFaultPhenomenonId() == null : this.getFaultPhenomenonId().equals(other.getFaultPhenomenonId()))
            && (this.getFaultDescription() == null ? other.getFaultDescription() == null : this.getFaultDescription().equals(other.getFaultDescription()))
            && (this.getCreatedTime() == null ? other.getCreatedTime() == null : this.getCreatedTime().equals(other.getCreatedTime()))
            && (this.getUpdatedTime() == null ? other.getUpdatedTime() == null : this.getUpdatedTime().equals(other.getUpdatedTime()))
            && (this.getVersion() == null ? other.getVersion() == null : this.getVersion().equals(other.getVersion()))
            && (this.getIsDelete() == null ? other.getIsDelete() == null : this.getIsDelete().equals(other.getIsDelete()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getRepairOrderId() == null) ? 0 : getRepairOrderId().hashCode());
        result = prime * result + ((getFaultPhenomenonId() == null) ? 0 : getFaultPhenomenonId().hashCode());
        result = prime * result + ((getFaultDescription() == null) ? 0 : getFaultDescription().hashCode());
        result = prime * result + ((getCreatedTime() == null) ? 0 : getCreatedTime().hashCode());
        result = prime * result + ((getUpdatedTime() == null) ? 0 : getUpdatedTime().hashCode());
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
        sb.append(", repairOrderId=").append(repairOrderId);
        sb.append(", faultPhenomenonId=").append(faultPhenomenonId);
        sb.append(", faultDescription=").append(faultDescription);
        sb.append(", createdTime=").append(createdTime);
        sb.append(", updatedTime=").append(updatedTime);
        sb.append(", version=").append(version);
        sb.append(", isDelete=").append(isDelete);
        sb.append("]");
        return sb.toString();
    }
}