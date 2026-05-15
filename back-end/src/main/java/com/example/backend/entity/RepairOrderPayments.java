package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("repair_order_payments")
public class RepairOrderPayments {

    @TableId
    private String id;

    private String repairOrderId;

    private BigDecimal doorFee;

    private BigDecimal distanceFee;

    private BigDecimal serviceDistanceKm;

    private BigDecimal baseRadiusKmSnapshot;

    private BigDecimal distanceOverKm;

    private BigDecimal minVisitFeeSnapshot;

    private BigDecimal extraFeePerKmSnapshot;

    private Integer distanceCalcTypeSnapshot;

    private Integer roundingRuleSnapshot;

    private Long pricingLockedTime;

    private String feeRuleSnapshot;

    private BigDecimal serviceFee;

    private BigDecimal materialFee;

    private BigDecimal overtimeFee;

    private BigDecimal totalAmount;

    private BigDecimal actualAmount;

    private BigDecimal discountAmount;

    private String couponId;

    private Integer paymentMethod;

    private Long paymentTime;

    private Long createdTime;

    private Long updatedTime;

    @Version
    private Integer version;

    @TableLogic
    private Integer isDelete;
}
