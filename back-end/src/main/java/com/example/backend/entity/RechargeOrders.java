package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import lombok.Data;

/** 钱包充值订单。外部支付完成前不直接形成资金流水。 */
@TableName("recharge_orders")
@Data
public class RechargeOrders {
    @TableId private String id;
    private String rechargeNo;
    private String accountId;
    private BigDecimal amount;
    private String currency;
    private Integer provider;
    private Integer status;
    private Long expiresAt;
    private Long postedTime;
    private Long createdTime;
    private Long updatedTime;
    @Version private Integer version;
    @TableLogic private Integer isDelete;
}
