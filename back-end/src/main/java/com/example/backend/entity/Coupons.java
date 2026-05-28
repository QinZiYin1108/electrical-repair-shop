package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 优惠券表
 * @TableName coupons
 */
@TableName(value ="coupons")
@Data
public class Coupons {
    /**
     * 主键，Q+雪花ID
     */
    @TableId
    private String id;

    /**
     * 优惠券名称
     */
    private String name;

    /**
     * 类型：1-满减券，2-折扣券，3-免费券
     */
    private Integer type;

    /**
     * 优惠类型：1-固定金额，2-百分比折扣
     */
    private Integer discountType;

    /**
     * 优惠值（金额或折扣百分比）
     */
    private BigDecimal discountValue;

    /**
     * 最低消费金额
     */
    private BigDecimal minAmount;

    /**
     * 最大优惠金额
     */
    private BigDecimal maxDiscount;

    /**
     * 发放总数量
     */
    private Integer totalCount;

    /**
     * 已使用数量
     */
    private Integer usedCount;

    /**
     * 每人限领数量
     */
    private Integer perUserLimit;

    /**
     * 适用范围：1-全部，2-维修服务，3-商品购买
     */
    private Integer applicableType;

    /**
     * 适用商品/服务ID列表JSON
     */
    private String applicableIds;

    /**
     * 状态：1-有效，2-已停用
     */
    private Integer status;

    /**
     * 开始时间戳
     */
    private Long startTime;

    /**
     * 结束时间戳
     */
    private Long endTime;

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

    /**
     * 归属门店ID（平台优惠券为NULL）
     */
    private String storeId;

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
        Coupons other = (Coupons) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getName() == null ? other.getName() == null : this.getName().equals(other.getName()))
            && (this.getType() == null ? other.getType() == null : this.getType().equals(other.getType()))
            && (this.getDiscountType() == null ? other.getDiscountType() == null : this.getDiscountType().equals(other.getDiscountType()))
            && (this.getDiscountValue() == null ? other.getDiscountValue() == null : this.getDiscountValue().equals(other.getDiscountValue()))
            && (this.getMinAmount() == null ? other.getMinAmount() == null : this.getMinAmount().equals(other.getMinAmount()))
            && (this.getMaxDiscount() == null ? other.getMaxDiscount() == null : this.getMaxDiscount().equals(other.getMaxDiscount()))
            && (this.getTotalCount() == null ? other.getTotalCount() == null : this.getTotalCount().equals(other.getTotalCount()))
            && (this.getUsedCount() == null ? other.getUsedCount() == null : this.getUsedCount().equals(other.getUsedCount()))
            && (this.getPerUserLimit() == null ? other.getPerUserLimit() == null : this.getPerUserLimit().equals(other.getPerUserLimit()))
            && (this.getApplicableType() == null ? other.getApplicableType() == null : this.getApplicableType().equals(other.getApplicableType()))
            && (this.getApplicableIds() == null ? other.getApplicableIds() == null : this.getApplicableIds().equals(other.getApplicableIds()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getStartTime() == null ? other.getStartTime() == null : this.getStartTime().equals(other.getStartTime()))
            && (this.getEndTime() == null ? other.getEndTime() == null : this.getEndTime().equals(other.getEndTime()))
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
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
        result = prime * result + ((getDiscountType() == null) ? 0 : getDiscountType().hashCode());
        result = prime * result + ((getDiscountValue() == null) ? 0 : getDiscountValue().hashCode());
        result = prime * result + ((getMinAmount() == null) ? 0 : getMinAmount().hashCode());
        result = prime * result + ((getMaxDiscount() == null) ? 0 : getMaxDiscount().hashCode());
        result = prime * result + ((getTotalCount() == null) ? 0 : getTotalCount().hashCode());
        result = prime * result + ((getUsedCount() == null) ? 0 : getUsedCount().hashCode());
        result = prime * result + ((getPerUserLimit() == null) ? 0 : getPerUserLimit().hashCode());
        result = prime * result + ((getApplicableType() == null) ? 0 : getApplicableType().hashCode());
        result = prime * result + ((getApplicableIds() == null) ? 0 : getApplicableIds().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getStartTime() == null) ? 0 : getStartTime().hashCode());
        result = prime * result + ((getEndTime() == null) ? 0 : getEndTime().hashCode());
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
        sb.append(", name=").append(name);
        sb.append(", type=").append(type);
        sb.append(", discountType=").append(discountType);
        sb.append(", discountValue=").append(discountValue);
        sb.append(", minAmount=").append(minAmount);
        sb.append(", maxDiscount=").append(maxDiscount);
        sb.append(", totalCount=").append(totalCount);
        sb.append(", usedCount=").append(usedCount);
        sb.append(", perUserLimit=").append(perUserLimit);
        sb.append(", applicableType=").append(applicableType);
        sb.append(", applicableIds=").append(applicableIds);
        sb.append(", status=").append(status);
        sb.append(", startTime=").append(startTime);
        sb.append(", endTime=").append(endTime);
        sb.append(", createdTime=").append(createdTime);
        sb.append(", updatedTime=").append(updatedTime);
        sb.append(", version=").append(version);
        sb.append(", isDelete=").append(isDelete);
        sb.append("]");
        return sb.toString();
    }
}