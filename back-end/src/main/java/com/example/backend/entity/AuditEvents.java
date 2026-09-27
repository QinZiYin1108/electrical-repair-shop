package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import lombok.Data;

/** 统一业务审计事件。记录关键业务状态与金额变更，便于事后追溯。 @TableName audit_events */
@TableName("audit_events")
@Data
public class AuditEvents {
    /** 主键，AE+雪花ID */
    @TableId private String id;

    /** 事件类型：ORDER_REFUND / BALANCE_ADJUST / BAN / UNBAN / INVENTORY_ADJUST 等 */
    private String eventType;

    /** 业务对象类型：REPAIR_ORDER / PRODUCT_ORDER / ACCOUNT / PRODUCT 等 */
    private String bizType;

    /** 业务对象ID */
    private String bizId;

    /** 变更前状态 */
    private String beforeState;

    /** 变更后状态 */
    private String afterState;

    /** 金额变更前 */
    private BigDecimal amountBefore;

    /** 金额变更后 */
    private BigDecimal amountAfter;

    /** 操作原因 */
    private String reason;

    /** 关联单号（支付/退款/售后/处罚单等） */
    private String relatedId;

    /** 操作者账号ID */
    private String operatorId;

    /** 操作者角色 */
    private String operatorRole;

    /** 操作者名称 */
    private String operatorName;

    /** 来源IP */
    private String sourceIp;

    /** 请求ID */
    private String requestId;

    /** 创建时间戳 */
    private Long createdTime;

    /** 乐观锁版本号 */
    @Version private Integer version;

    /** 逻辑删除：0-未删除，1-已删除 */
    @TableLogic private Integer isDelete;
}
