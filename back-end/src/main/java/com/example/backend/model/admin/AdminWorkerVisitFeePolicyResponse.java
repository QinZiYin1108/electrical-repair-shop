package com.example.backend.model.admin;

import java.math.BigDecimal;

public class AdminWorkerVisitFeePolicyResponse {

    private String id;
    private Integer serviceKind;
    private BigDecimal minVisitFee;
    private BigDecimal baseRadiusKm;
    private BigDecimal extraFeePerKm;
    private Integer distanceCalcType;
    private Integer roundingRule;
    private BigDecimal maxVisitFee;
    private Integer isActive;
    private Long effectiveTime;
    private Long updatedTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getServiceKind() {
        return serviceKind;
    }

    public void setServiceKind(Integer serviceKind) {
        this.serviceKind = serviceKind;
    }

    public BigDecimal getMinVisitFee() {
        return minVisitFee;
    }

    public void setMinVisitFee(BigDecimal minVisitFee) {
        this.minVisitFee = minVisitFee;
    }

    public BigDecimal getBaseRadiusKm() {
        return baseRadiusKm;
    }

    public void setBaseRadiusKm(BigDecimal baseRadiusKm) {
        this.baseRadiusKm = baseRadiusKm;
    }

    public BigDecimal getExtraFeePerKm() {
        return extraFeePerKm;
    }

    public void setExtraFeePerKm(BigDecimal extraFeePerKm) {
        this.extraFeePerKm = extraFeePerKm;
    }

    public Integer getDistanceCalcType() {
        return distanceCalcType;
    }

    public void setDistanceCalcType(Integer distanceCalcType) {
        this.distanceCalcType = distanceCalcType;
    }

    public Integer getRoundingRule() {
        return roundingRule;
    }

    public void setRoundingRule(Integer roundingRule) {
        this.roundingRule = roundingRule;
    }

    public BigDecimal getMaxVisitFee() {
        return maxVisitFee;
    }

    public void setMaxVisitFee(BigDecimal maxVisitFee) {
        this.maxVisitFee = maxVisitFee;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public Long getEffectiveTime() {
        return effectiveTime;
    }

    public void setEffectiveTime(Long effectiveTime) {
        this.effectiveTime = effectiveTime;
    }

    public Long getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(Long updatedTime) {
        this.updatedTime = updatedTime;
    }
}
