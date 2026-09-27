package com.example.backend.model.worker;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "师傅上门费用Policy列表项")
@Data
public class WorkerVisitFeePolicyItem {

    @Schema(description = "ID")
    private String id;

    /** 上门服务类型：1-上门维修，2-上门安装 */
    @Schema(description = "服务Kind")
    private Integer serviceKind;

    /** 最低上门费（元） */
    @Schema(description = "min上门费用")
    private BigDecimal minVisitFee;

    /** 基础服务半径（公里） */
    @Schema(description = "baseRadiusKm")
    private BigDecimal baseRadiusKm;

    /** 超区每公里费用（元） */
    @Schema(description = "extra费用PerKm")
    private BigDecimal extraFeePerKm;

    /** 距离计算方式：1-驾车，2-骑行 */
    @Schema(description = "距离Calc类型")
    private Integer distanceCalcType;

    /** 公里取整规则：1-向上取整，2-四舍五入 */
    @Schema(description = "rounding规则")
    private Integer roundingRule;

    /** 封顶公里数（可空） */
    @Schema(description = "max上门费用")
    private BigDecimal maxVisitFee;

    /** 是否启用：0-禁用，1-启用 */
    @Schema(description = "is是否启用")
    private Integer isActive;
}
