package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import lombok.Data;

/** 订单财务核算快照。记录收入归属与分摊，支持按支付/履约/退款时间统计。 @TableName finance_order_snapshots */
@TableName("finance_order_snapshots")
@Data
public class FinanceOrderSnapshots {
    /** 主键，FO+雪花ID */
    @TableId private String id;

    /** 订单类型：1-维修订单，2-商品订单 */
    private Integer orderType;

    /** 订单ID */
    private String orderId;

    /** 订单号 */
    private String orderNo;

    /** 用户账号ID */
    private String userAccountId;

    /** 维修服务费收入 */
    private BigDecimal incomeService;

    /** 配件收入 */
    private BigDecimal incomeMaterial;

    /** 商品收入 */
    private BigDecimal incomeProduct;

    /** 上门费收入 */
    private BigDecimal incomeDoorFee;

    /** 运费收入 */
    private BigDecimal incomeShipping;

    /** 平台服务费收入 */
    private BigDecimal incomePlatformService;

    /** 实收合计 */
    private BigDecimal totalPaid;

    /** 退款合计 */
    private BigDecimal totalRefunded;

    /** 净额（实收-退款） */
    private BigDecimal netAmount;

    /** 平台佣金 */
    private BigDecimal platformCommission;

    /** 维修人员应结收入 */
    private BigDecimal technicianIncome;

    /** 税率 */
    private BigDecimal taxRate;

    /** 税额 */
    private BigDecimal taxAmount;

    /** 优惠金额 */
    private BigDecimal discountAmount;

    /** 优惠承担方：PLATFORM/MERCHANT/TECHNICIAN */
    private String discountBearer;

    /** 支付时间戳 */
    private Long payTime;

    /** 履约完成时间戳 */
    private Long performTime;

    /** 退款时间戳 */
    private Long refundTime;

    /** 状态：1-正常 */
    private Integer status;

    /** 创建时间戳 */
    private Long createdTime;

    /** 更新时间戳 */
    private Long updatedTime;

    /** 乐观锁版本号 */
    @Version private Integer version;

    /** 逻辑删除：0-未删除，1-已删除 */
    @TableLogic private Integer isDelete;
}
