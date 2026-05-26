package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 商品订单表
 * @TableName product_orders
 */
@TableName(value ="product_orders")
@Data
public class ProductOrders {
    /**
     * 主键，PO+雪花ID
     */
    @TableId
    private String id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户账号ID
     */
    private String accountId;

    /**
     * 订单状态：1-待支付，2-待发货，3-待收货，4-待评价，5-已完成，6-已取消，7-已退款
     */
    private Integer orderStatus;

    /**
     * 支付状态：1-待支付，2-已支付，3-已退款
     */
    private Integer paymentStatus;

    /**
     * 配送状态：1-待发货，2-已发货，3-配送中，4-已送达
     */
    private Integer deliveryStatus;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 商品金额
     */
    private BigDecimal productAmount;

    /**
     * 运费
     */
    private BigDecimal shippingFee;

    /**
     * 优惠金额
     */
    private BigDecimal discountAmount;

    /**
     * 实际支付金额
     */
    private BigDecimal actualAmount;

    /**
     * 使用的优惠券ID
     */
    private String couponId;

    /**
     * 支付方式：1-微信支付，2-支付宝，3-银行卡
     */
    private Integer paymentMethod;

    /**
     * 支付时间戳
     */
    private Long paymentTime;

    /**
     * 收货地址ID
     */
    private String deliveryAddressId;

    /**
     * 收货人姓名
     */
    private String deliveryName;

    /**
     * 收货人电话
     */
    private String deliveryPhone;

    /**
     * 收货地址
     */
    private String deliveryAddress;

    /**
     * 快递公司
     */
    private String deliveryCompany;

    /**
     * 快递单号
     */
    private String deliveryNo;

    /**
     * 发货时间戳
     */
    private Long deliveryTime;

    /**
     * 收货时间戳
     */
    private Long receiveTime;

    /**
     * 完成时间戳
     */
    private Long completionTime;

    /**
     * 取消原因
     */
    private String cancelReason;

    /**
     * 取消时间戳
     */
    private Long cancelTime;

    /**
     * 退款原因
     */
    private String refundReason;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 退款时间戳
     */
    private Long refundTime;

    /**
     * 订单备注
     */
    private String remark;

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
        ProductOrders other = (ProductOrders) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getOrderNo() == null ? other.getOrderNo() == null : this.getOrderNo().equals(other.getOrderNo()))
            && (this.getAccountId() == null ? other.getAccountId() == null : this.getAccountId().equals(other.getAccountId()))
            && (this.getOrderStatus() == null ? other.getOrderStatus() == null : this.getOrderStatus().equals(other.getOrderStatus()))
            && (this.getPaymentStatus() == null ? other.getPaymentStatus() == null : this.getPaymentStatus().equals(other.getPaymentStatus()))
            && (this.getDeliveryStatus() == null ? other.getDeliveryStatus() == null : this.getDeliveryStatus().equals(other.getDeliveryStatus()))
            && (this.getTotalAmount() == null ? other.getTotalAmount() == null : this.getTotalAmount().equals(other.getTotalAmount()))
            && (this.getProductAmount() == null ? other.getProductAmount() == null : this.getProductAmount().equals(other.getProductAmount()))
            && (this.getShippingFee() == null ? other.getShippingFee() == null : this.getShippingFee().equals(other.getShippingFee()))
            && (this.getDiscountAmount() == null ? other.getDiscountAmount() == null : this.getDiscountAmount().equals(other.getDiscountAmount()))
            && (this.getActualAmount() == null ? other.getActualAmount() == null : this.getActualAmount().equals(other.getActualAmount()))
            && (this.getCouponId() == null ? other.getCouponId() == null : this.getCouponId().equals(other.getCouponId()))
            && (this.getPaymentMethod() == null ? other.getPaymentMethod() == null : this.getPaymentMethod().equals(other.getPaymentMethod()))
            && (this.getPaymentTime() == null ? other.getPaymentTime() == null : this.getPaymentTime().equals(other.getPaymentTime()))
            && (this.getDeliveryAddressId() == null ? other.getDeliveryAddressId() == null : this.getDeliveryAddressId().equals(other.getDeliveryAddressId()))
            && (this.getDeliveryName() == null ? other.getDeliveryName() == null : this.getDeliveryName().equals(other.getDeliveryName()))
            && (this.getDeliveryPhone() == null ? other.getDeliveryPhone() == null : this.getDeliveryPhone().equals(other.getDeliveryPhone()))
            && (this.getDeliveryAddress() == null ? other.getDeliveryAddress() == null : this.getDeliveryAddress().equals(other.getDeliveryAddress()))
            && (this.getDeliveryCompany() == null ? other.getDeliveryCompany() == null : this.getDeliveryCompany().equals(other.getDeliveryCompany()))
            && (this.getDeliveryNo() == null ? other.getDeliveryNo() == null : this.getDeliveryNo().equals(other.getDeliveryNo()))
            && (this.getDeliveryTime() == null ? other.getDeliveryTime() == null : this.getDeliveryTime().equals(other.getDeliveryTime()))
            && (this.getReceiveTime() == null ? other.getReceiveTime() == null : this.getReceiveTime().equals(other.getReceiveTime()))
            && (this.getCompletionTime() == null ? other.getCompletionTime() == null : this.getCompletionTime().equals(other.getCompletionTime()))
            && (this.getCancelReason() == null ? other.getCancelReason() == null : this.getCancelReason().equals(other.getCancelReason()))
            && (this.getCancelTime() == null ? other.getCancelTime() == null : this.getCancelTime().equals(other.getCancelTime()))
            && (this.getRefundReason() == null ? other.getRefundReason() == null : this.getRefundReason().equals(other.getRefundReason()))
            && (this.getRefundAmount() == null ? other.getRefundAmount() == null : this.getRefundAmount().equals(other.getRefundAmount()))
            && (this.getRefundTime() == null ? other.getRefundTime() == null : this.getRefundTime().equals(other.getRefundTime()))
            && (this.getRemark() == null ? other.getRemark() == null : this.getRemark().equals(other.getRemark()))
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
        result = prime * result + ((getOrderNo() == null) ? 0 : getOrderNo().hashCode());
        result = prime * result + ((getAccountId() == null) ? 0 : getAccountId().hashCode());
        result = prime * result + ((getOrderStatus() == null) ? 0 : getOrderStatus().hashCode());
        result = prime * result + ((getPaymentStatus() == null) ? 0 : getPaymentStatus().hashCode());
        result = prime * result + ((getDeliveryStatus() == null) ? 0 : getDeliveryStatus().hashCode());
        result = prime * result + ((getTotalAmount() == null) ? 0 : getTotalAmount().hashCode());
        result = prime * result + ((getProductAmount() == null) ? 0 : getProductAmount().hashCode());
        result = prime * result + ((getShippingFee() == null) ? 0 : getShippingFee().hashCode());
        result = prime * result + ((getDiscountAmount() == null) ? 0 : getDiscountAmount().hashCode());
        result = prime * result + ((getActualAmount() == null) ? 0 : getActualAmount().hashCode());
        result = prime * result + ((getCouponId() == null) ? 0 : getCouponId().hashCode());
        result = prime * result + ((getPaymentMethod() == null) ? 0 : getPaymentMethod().hashCode());
        result = prime * result + ((getPaymentTime() == null) ? 0 : getPaymentTime().hashCode());
        result = prime * result + ((getDeliveryAddressId() == null) ? 0 : getDeliveryAddressId().hashCode());
        result = prime * result + ((getDeliveryName() == null) ? 0 : getDeliveryName().hashCode());
        result = prime * result + ((getDeliveryPhone() == null) ? 0 : getDeliveryPhone().hashCode());
        result = prime * result + ((getDeliveryAddress() == null) ? 0 : getDeliveryAddress().hashCode());
        result = prime * result + ((getDeliveryCompany() == null) ? 0 : getDeliveryCompany().hashCode());
        result = prime * result + ((getDeliveryNo() == null) ? 0 : getDeliveryNo().hashCode());
        result = prime * result + ((getDeliveryTime() == null) ? 0 : getDeliveryTime().hashCode());
        result = prime * result + ((getReceiveTime() == null) ? 0 : getReceiveTime().hashCode());
        result = prime * result + ((getCompletionTime() == null) ? 0 : getCompletionTime().hashCode());
        result = prime * result + ((getCancelReason() == null) ? 0 : getCancelReason().hashCode());
        result = prime * result + ((getCancelTime() == null) ? 0 : getCancelTime().hashCode());
        result = prime * result + ((getRefundReason() == null) ? 0 : getRefundReason().hashCode());
        result = prime * result + ((getRefundAmount() == null) ? 0 : getRefundAmount().hashCode());
        result = prime * result + ((getRefundTime() == null) ? 0 : getRefundTime().hashCode());
        result = prime * result + ((getRemark() == null) ? 0 : getRemark().hashCode());
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
        sb.append(", orderNo=").append(orderNo);
        sb.append(", accountId=").append(accountId);
        sb.append(", orderStatus=").append(orderStatus);
        sb.append(", paymentStatus=").append(paymentStatus);
        sb.append(", deliveryStatus=").append(deliveryStatus);
        sb.append(", totalAmount=").append(totalAmount);
        sb.append(", productAmount=").append(productAmount);
        sb.append(", shippingFee=").append(shippingFee);
        sb.append(", discountAmount=").append(discountAmount);
        sb.append(", actualAmount=").append(actualAmount);
        sb.append(", couponId=").append(couponId);
        sb.append(", paymentMethod=").append(paymentMethod);
        sb.append(", paymentTime=").append(paymentTime);
        sb.append(", deliveryAddressId=").append(deliveryAddressId);
        sb.append(", deliveryName=").append(deliveryName);
        sb.append(", deliveryPhone=").append(deliveryPhone);
        sb.append(", deliveryAddress=").append(deliveryAddress);
        sb.append(", deliveryCompany=").append(deliveryCompany);
        sb.append(", deliveryNo=").append(deliveryNo);
        sb.append(", deliveryTime=").append(deliveryTime);
        sb.append(", receiveTime=").append(receiveTime);
        sb.append(", completionTime=").append(completionTime);
        sb.append(", cancelReason=").append(cancelReason);
        sb.append(", cancelTime=").append(cancelTime);
        sb.append(", refundReason=").append(refundReason);
        sb.append(", refundAmount=").append(refundAmount);
        sb.append(", refundTime=").append(refundTime);
        sb.append(", remark=").append(remark);
        sb.append(", createdTime=").append(createdTime);
        sb.append(", updatedTime=").append(updatedTime);
        sb.append(", version=").append(version);
        sb.append(", isDelete=").append(isDelete);
        sb.append("]");
        return sb.toString();
    }
}