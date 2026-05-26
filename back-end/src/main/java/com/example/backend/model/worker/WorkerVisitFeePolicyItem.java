package com.example.backend.model.worker;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WorkerVisitFeePolicyItem {

    private String id;

    /**
     * 上门服务类型：1-上门维修，2-上门安装
     */
    private Integer serviceKind;

    /**
     * 最低上门费（元）
     */
    private BigDecimal minVisitFee;

    /**
     * 基础服务半径（公里）
     */
    private BigDecimal baseRadiusKm;

    /**
     * 超区每公里费用（元）
     */
    private BigDecimal extraFeePerKm;

    /**
     * 距离计算方式：1-驾车，2-骑行
     */
    private Integer distanceCalcType;

    /**
     * 公里取整规则：1-向上取整，2-四舍五入
     */
    private Integer roundingRule;

    /**
     * 封顶公里数（可空）
     */
    private BigDecimal maxVisitFee;

    /**
     * 是否启用：0-禁用，1-启用
     */
    private Integer isActive;
}
