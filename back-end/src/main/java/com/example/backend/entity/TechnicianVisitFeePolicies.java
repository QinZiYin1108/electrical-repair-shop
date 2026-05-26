package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("technician_visit_fee_policies")
public class TechnicianVisitFeePolicies {

    @TableId
    private String id;

    private String technicianAccountId;

    private Integer serviceKind;

    private BigDecimal minVisitFee;

    private BigDecimal baseRadiusKm;

    private BigDecimal extraFeePerKm;

    private Integer distanceCalcType;

    private Integer roundingRule;

    private BigDecimal maxVisitFee;

    private Integer isActive;

    private Long effectiveTime;

    private Long createdTime;

    private Long updatedTime;

    @Version
    private Integer version;

    @TableLogic
    private Integer isDelete;
}
