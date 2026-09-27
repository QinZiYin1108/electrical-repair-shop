package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import lombok.Data;

/** 支付退款记录。一笔支付可对应多次退款申请，以幂等键防止重复退款。 @TableName payment_refunds */
@TableName("payment_refunds")
@Data
public class PaymentRefunds {
    /** 主键，RF+雪花ID */
    @TableId private String id;

    /** 平台退款单号 */
    private String refundNo;

    /** 原支付记录ID */
    private String paymentId;

    /** 原支付单号 */
    private String paymentNo;

    /** 用户账号ID */
    private String accountId;

    /** 退款金额 */
    private BigDecimal refundAmount;

    /** 币种 */
    private String currency;

    /** 退款原因 */
    private String refundReason;

    /** 退款状态：1-退款中，2-退款成功，3-退款失败 */
    private Integer refundStatus;

    /** 支付渠道：1-微信，2-支付宝 */
    private Integer provider;

    /** 渠道退款单号 */
    private String providerRefundNo;

    /** 幂等键，防止重复退款 */
    private String idempotencyKey;

    /** 脱敏后的退款回调数据 */
    private String callbackData;

    /** 退款发起时间戳 */
    private Long initiatedTime;

    /** 退款完成时间戳 */
    private Long completedTime;

    /** 创建时间戳 */
    private Long createdTime;

    /** 更新时间戳 */
    private Long updatedTime;

    /** 乐观锁版本号 */
    @Version private Integer version;

    /** 逻辑删除：0-未删除，1-已删除 */
    @TableLogic private Integer isDelete;
}
