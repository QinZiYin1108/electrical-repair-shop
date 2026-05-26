package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;
import lombok.Data;

/**
 * 保修卡表
 * @TableName warranty_cards
 */
@TableName(value ="warranty_cards")
@Data
public class WarrantyCards {
    /**
     * 主键，WC+雪花ID
     */
    @TableId
    private String id;

    /**
     * 保修卡号
     */
    private String cardNo;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 商品ID
     */
    private String productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品型号
     */
    private String productModel;

    /**
     * 购买日期
     */
    private Date purchaseDate;

    /**
     * 保修开始日期
     */
    private Date warrantyStartDate;

    /**
     * 保修结束日期
     */
    private Date warrantyEndDate;

    /**
     * 保修期（月）
     */
    private Integer warrantyPeriod;

    /**
     * 保修类型：1-厂家保修，2-店铺保修，3-延保
     */
    private Integer warrantyType;

    /**
     * 保修状态：1-有效，2-已过期，3-已使用
     */
    private Integer warrantyStatus;

    /**
     * 维修次数
     */
    private Integer repairCount;

    /**
     * 最后维修日期
     */
    private Date lastRepairDate;

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
        WarrantyCards other = (WarrantyCards) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getCardNo() == null ? other.getCardNo() == null : this.getCardNo().equals(other.getCardNo()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getProductId() == null ? other.getProductId() == null : this.getProductId().equals(other.getProductId()))
            && (this.getProductName() == null ? other.getProductName() == null : this.getProductName().equals(other.getProductName()))
            && (this.getProductModel() == null ? other.getProductModel() == null : this.getProductModel().equals(other.getProductModel()))
            && (this.getPurchaseDate() == null ? other.getPurchaseDate() == null : this.getPurchaseDate().equals(other.getPurchaseDate()))
            && (this.getWarrantyStartDate() == null ? other.getWarrantyStartDate() == null : this.getWarrantyStartDate().equals(other.getWarrantyStartDate()))
            && (this.getWarrantyEndDate() == null ? other.getWarrantyEndDate() == null : this.getWarrantyEndDate().equals(other.getWarrantyEndDate()))
            && (this.getWarrantyPeriod() == null ? other.getWarrantyPeriod() == null : this.getWarrantyPeriod().equals(other.getWarrantyPeriod()))
            && (this.getWarrantyType() == null ? other.getWarrantyType() == null : this.getWarrantyType().equals(other.getWarrantyType()))
            && (this.getWarrantyStatus() == null ? other.getWarrantyStatus() == null : this.getWarrantyStatus().equals(other.getWarrantyStatus()))
            && (this.getRepairCount() == null ? other.getRepairCount() == null : this.getRepairCount().equals(other.getRepairCount()))
            && (this.getLastRepairDate() == null ? other.getLastRepairDate() == null : this.getLastRepairDate().equals(other.getLastRepairDate()))
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
        result = prime * result + ((getCardNo() == null) ? 0 : getCardNo().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getProductId() == null) ? 0 : getProductId().hashCode());
        result = prime * result + ((getProductName() == null) ? 0 : getProductName().hashCode());
        result = prime * result + ((getProductModel() == null) ? 0 : getProductModel().hashCode());
        result = prime * result + ((getPurchaseDate() == null) ? 0 : getPurchaseDate().hashCode());
        result = prime * result + ((getWarrantyStartDate() == null) ? 0 : getWarrantyStartDate().hashCode());
        result = prime * result + ((getWarrantyEndDate() == null) ? 0 : getWarrantyEndDate().hashCode());
        result = prime * result + ((getWarrantyPeriod() == null) ? 0 : getWarrantyPeriod().hashCode());
        result = prime * result + ((getWarrantyType() == null) ? 0 : getWarrantyType().hashCode());
        result = prime * result + ((getWarrantyStatus() == null) ? 0 : getWarrantyStatus().hashCode());
        result = prime * result + ((getRepairCount() == null) ? 0 : getRepairCount().hashCode());
        result = prime * result + ((getLastRepairDate() == null) ? 0 : getLastRepairDate().hashCode());
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
        sb.append(", cardNo=").append(cardNo);
        sb.append(", userId=").append(userId);
        sb.append(", productId=").append(productId);
        sb.append(", productName=").append(productName);
        sb.append(", productModel=").append(productModel);
        sb.append(", purchaseDate=").append(purchaseDate);
        sb.append(", warrantyStartDate=").append(warrantyStartDate);
        sb.append(", warrantyEndDate=").append(warrantyEndDate);
        sb.append(", warrantyPeriod=").append(warrantyPeriod);
        sb.append(", warrantyType=").append(warrantyType);
        sb.append(", warrantyStatus=").append(warrantyStatus);
        sb.append(", repairCount=").append(repairCount);
        sb.append(", lastRepairDate=").append(lastRepairDate);
        sb.append(", createdTime=").append(createdTime);
        sb.append(", updatedTime=").append(updatedTime);
        sb.append(", version=").append(version);
        sb.append(", isDelete=").append(isDelete);
        sb.append("]");
        return sb.toString();
    }
}