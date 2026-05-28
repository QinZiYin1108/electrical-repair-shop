package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 商品表
 * @TableName products
 */
@TableName(value ="products")
@Data
public class Products {
    /**
     * 主键，PD+雪花ID
     */
    @TableId
    private String id;

    /**
     * 商品编号
     */
    private String productNo;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品类型：1-普通商品，2-二手商品
     */
    private Integer productType;

    /**
     * 分类ID
     */
    private String categoryId;

    /**
     * 归属门店ID
     */
    private String storeId;

    /**
     * 品牌
     */
    private String brand;

    /**
     * 型号
     */
    private String model;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 规格参数JSON
     */
    private String specifications;

    /**
     * 主图URL
     */
    private String mainImageUrl;

    /**
     * 商品图片JSON数组
     */
    private String imageUrls;

    /**
     * 商品视频JSON数组
     */
    private String videoUrls;

    /**
     * 原价
     */
    private BigDecimal originalPrice;

    /**
     * 售价
     */
    private BigDecimal sellingPrice;

    /**
     * 成本价
     */
    private BigDecimal costPrice;

    /**
     * 库存数量
     */
    private Integer stockQuantity;

    /**
     * 预警库存
     */
    private Integer warningStock;

    /**
     * 销量
     */
    private Integer salesCount;

    /**
     * 浏览量
     */
    private Integer viewCount;

    /**
     * 收藏量
     */
    private Integer favoriteCount;

    /**
     * 重量（kg）
     */
    private BigDecimal weight;

    /**
     * 尺寸（长x宽x高）
     */
    private String dimensions;

    /**
     * 保修期（月）
     */
    private Integer warrantyPeriod;

    /**
     * 运费
     */
    private BigDecimal shippingFee;

    /**
     * 是否包邮：0-否，1-是
     */
    private Integer isFreeShipping;

    /**
     * 商品状态：1-上架，2-下架，3-缺货
     */
    private Integer status;

    /**
     * 是否热销：0-否，1-是
     */
    private Integer isHot;

    /**
     * 是否新品：0-否，1-是
     */
    private Integer isNew;

    /**
     * 是否推荐：0-否，1-是
     */
    private Integer isRecommended;

    /**
     * 履约方式：1-自取，2-送货上门
     */
    private Integer fulfillmentType;

    /**
     * 配送范围（公里）
     */
    private BigDecimal deliveryRangeKm;

    /**
     * 配送费
     */
    private BigDecimal deliveryFee;

    /**
     * 送货上门是否需要预约：0-否，1-是
     */
    private Integer needAppointment;

    /**
     * 审核状态：1-待审核，2-审核通过，3-审核拒绝
     */
    private Integer auditStatus;

    /**
     * 是否冻结：0-正常，1-已冻结
     */
    private Integer isFrozen;

    /**
     * 冻结时间戳
     */
    private Long frozenTime;

    /**
     * 冻结操作人ID
     */
    private String frozenBy;

    /**
     * 排序
     */
    private Integer sortOrder;

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
        Products other = (Products) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getProductNo() == null ? other.getProductNo() == null : this.getProductNo().equals(other.getProductNo()))
            && (this.getName() == null ? other.getName() == null : this.getName().equals(other.getName()))
            && (this.getCategoryId() == null ? other.getCategoryId() == null : this.getCategoryId().equals(other.getCategoryId()))
            && (this.getBrand() == null ? other.getBrand() == null : this.getBrand().equals(other.getBrand()))
            && (this.getModel() == null ? other.getModel() == null : this.getModel().equals(other.getModel()))
            && (this.getDescription() == null ? other.getDescription() == null : this.getDescription().equals(other.getDescription()))
            && (this.getSpecifications() == null ? other.getSpecifications() == null : this.getSpecifications().equals(other.getSpecifications()))
            && (this.getMainImageUrl() == null ? other.getMainImageUrl() == null : this.getMainImageUrl().equals(other.getMainImageUrl()))
            && (this.getImageUrls() == null ? other.getImageUrls() == null : this.getImageUrls().equals(other.getImageUrls()))
            && (this.getVideoUrls() == null ? other.getVideoUrls() == null : this.getVideoUrls().equals(other.getVideoUrls()))
            && (this.getOriginalPrice() == null ? other.getOriginalPrice() == null : this.getOriginalPrice().equals(other.getOriginalPrice()))
            && (this.getSellingPrice() == null ? other.getSellingPrice() == null : this.getSellingPrice().equals(other.getSellingPrice()))
            && (this.getCostPrice() == null ? other.getCostPrice() == null : this.getCostPrice().equals(other.getCostPrice()))
            && (this.getStockQuantity() == null ? other.getStockQuantity() == null : this.getStockQuantity().equals(other.getStockQuantity()))
            && (this.getWarningStock() == null ? other.getWarningStock() == null : this.getWarningStock().equals(other.getWarningStock()))
            && (this.getSalesCount() == null ? other.getSalesCount() == null : this.getSalesCount().equals(other.getSalesCount()))
            && (this.getViewCount() == null ? other.getViewCount() == null : this.getViewCount().equals(other.getViewCount()))
            && (this.getFavoriteCount() == null ? other.getFavoriteCount() == null : this.getFavoriteCount().equals(other.getFavoriteCount()))
            && (this.getWeight() == null ? other.getWeight() == null : this.getWeight().equals(other.getWeight()))
            && (this.getDimensions() == null ? other.getDimensions() == null : this.getDimensions().equals(other.getDimensions()))
            && (this.getWarrantyPeriod() == null ? other.getWarrantyPeriod() == null : this.getWarrantyPeriod().equals(other.getWarrantyPeriod()))
            && (this.getShippingFee() == null ? other.getShippingFee() == null : this.getShippingFee().equals(other.getShippingFee()))
            && (this.getIsFreeShipping() == null ? other.getIsFreeShipping() == null : this.getIsFreeShipping().equals(other.getIsFreeShipping()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getIsHot() == null ? other.getIsHot() == null : this.getIsHot().equals(other.getIsHot()))
            && (this.getIsNew() == null ? other.getIsNew() == null : this.getIsNew().equals(other.getIsNew()))
            && (this.getIsRecommended() == null ? other.getIsRecommended() == null : this.getIsRecommended().equals(other.getIsRecommended()))
            && (this.getSortOrder() == null ? other.getSortOrder() == null : this.getSortOrder().equals(other.getSortOrder()))
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
        result = prime * result + ((getProductNo() == null) ? 0 : getProductNo().hashCode());
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((getCategoryId() == null) ? 0 : getCategoryId().hashCode());
        result = prime * result + ((getBrand() == null) ? 0 : getBrand().hashCode());
        result = prime * result + ((getModel() == null) ? 0 : getModel().hashCode());
        result = prime * result + ((getDescription() == null) ? 0 : getDescription().hashCode());
        result = prime * result + ((getSpecifications() == null) ? 0 : getSpecifications().hashCode());
        result = prime * result + ((getMainImageUrl() == null) ? 0 : getMainImageUrl().hashCode());
        result = prime * result + ((getImageUrls() == null) ? 0 : getImageUrls().hashCode());
        result = prime * result + ((getVideoUrls() == null) ? 0 : getVideoUrls().hashCode());
        result = prime * result + ((getOriginalPrice() == null) ? 0 : getOriginalPrice().hashCode());
        result = prime * result + ((getSellingPrice() == null) ? 0 : getSellingPrice().hashCode());
        result = prime * result + ((getCostPrice() == null) ? 0 : getCostPrice().hashCode());
        result = prime * result + ((getStockQuantity() == null) ? 0 : getStockQuantity().hashCode());
        result = prime * result + ((getWarningStock() == null) ? 0 : getWarningStock().hashCode());
        result = prime * result + ((getSalesCount() == null) ? 0 : getSalesCount().hashCode());
        result = prime * result + ((getViewCount() == null) ? 0 : getViewCount().hashCode());
        result = prime * result + ((getFavoriteCount() == null) ? 0 : getFavoriteCount().hashCode());
        result = prime * result + ((getWeight() == null) ? 0 : getWeight().hashCode());
        result = prime * result + ((getDimensions() == null) ? 0 : getDimensions().hashCode());
        result = prime * result + ((getWarrantyPeriod() == null) ? 0 : getWarrantyPeriod().hashCode());
        result = prime * result + ((getShippingFee() == null) ? 0 : getShippingFee().hashCode());
        result = prime * result + ((getIsFreeShipping() == null) ? 0 : getIsFreeShipping().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getIsHot() == null) ? 0 : getIsHot().hashCode());
        result = prime * result + ((getIsNew() == null) ? 0 : getIsNew().hashCode());
        result = prime * result + ((getIsRecommended() == null) ? 0 : getIsRecommended().hashCode());
        result = prime * result + ((getSortOrder() == null) ? 0 : getSortOrder().hashCode());
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
        sb.append(", productNo=").append(productNo);
        sb.append(", name=").append(name);
        sb.append(", categoryId=").append(categoryId);
        sb.append(", brand=").append(brand);
        sb.append(", model=").append(model);
        sb.append(", description=").append(description);
        sb.append(", specifications=").append(specifications);
        sb.append(", mainImageUrl=").append(mainImageUrl);
        sb.append(", imageUrls=").append(imageUrls);
        sb.append(", videoUrls=").append(videoUrls);
        sb.append(", originalPrice=").append(originalPrice);
        sb.append(", sellingPrice=").append(sellingPrice);
        sb.append(", costPrice=").append(costPrice);
        sb.append(", stockQuantity=").append(stockQuantity);
        sb.append(", warningStock=").append(warningStock);
        sb.append(", salesCount=").append(salesCount);
        sb.append(", viewCount=").append(viewCount);
        sb.append(", favoriteCount=").append(favoriteCount);
        sb.append(", weight=").append(weight);
        sb.append(", dimensions=").append(dimensions);
        sb.append(", warrantyPeriod=").append(warrantyPeriod);
        sb.append(", shippingFee=").append(shippingFee);
        sb.append(", isFreeShipping=").append(isFreeShipping);
        sb.append(", status=").append(status);
        sb.append(", isHot=").append(isHot);
        sb.append(", isNew=").append(isNew);
        sb.append(", isRecommended=").append(isRecommended);
        sb.append(", sortOrder=").append(sortOrder);
        sb.append(", createdTime=").append(createdTime);
        sb.append(", updatedTime=").append(updatedTime);
        sb.append(", version=").append(version);
        sb.append(", isDelete=").append(isDelete);
        sb.append("]");
        return sb.toString();
    }
}
