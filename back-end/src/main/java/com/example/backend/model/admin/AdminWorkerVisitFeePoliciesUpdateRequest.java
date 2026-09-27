package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理员师傅上门费用PoliciesUpdate请求")
public class AdminWorkerVisitFeePoliciesUpdateRequest {

    @NotEmpty(message = "计费策略不能为空")
    @Valid
    @Schema(description = "policies")
    private List<PolicyItem> policies;

    public List<PolicyItem> getPolicies() {
        return policies;
    }

    public void setPolicies(List<PolicyItem> policies) {
        this.policies = policies;
    }

    public static class PolicyItem {

        @Schema(description = "ID")
        private String id;

        @NotNull(message = "服务类型不能为空")
        @Schema(description = "服务Kind")
        private Integer serviceKind;

        @NotNull(message = "最低上门费不能为空")
        @Schema(description = "min上门费用")
        private BigDecimal minVisitFee;

        @NotNull(message = "基础服务半径不能为空")
        @Schema(description = "baseRadiusKm")
        private BigDecimal baseRadiusKm;

        @NotNull(message = "超区每公里费用不能为空")
        @Schema(description = "extra费用PerKm")
        private BigDecimal extraFeePerKm;

        @NotNull(message = "距离计算方式不能为空")
        @Schema(description = "距离Calc类型")
        private Integer distanceCalcType;

        @NotNull(message = "取整规则不能为空")
        @Schema(description = "rounding规则")
        private Integer roundingRule;

        @Schema(description = "max上门费用")
        private BigDecimal maxVisitFee;

        @Schema(description = "is是否启用")
        private Integer isActive;

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
    }
}
