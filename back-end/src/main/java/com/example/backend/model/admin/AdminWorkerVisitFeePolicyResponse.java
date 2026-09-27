package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "管理员师傅上门费用Policy响应")
public class AdminWorkerVisitFeePolicyResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "服务Kind")
    private Integer serviceKind;

    @Schema(description = "min上门费用")
    private BigDecimal minVisitFee;

    @Schema(description = "baseRadiusKm")
    private BigDecimal baseRadiusKm;

    @Schema(description = "extra费用PerKm")
    private BigDecimal extraFeePerKm;

    @Schema(description = "距离Calc类型")
    private Integer distanceCalcType;

    @Schema(description = "rounding规则")
    private Integer roundingRule;

    @Schema(description = "max上门费用")
    private BigDecimal maxVisitFee;

    @Schema(description = "is是否启用")
    private Integer isActive;

    @Schema(description = "effective时间")
    private Long effectiveTime;

    @Schema(description = "更新时间")
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
