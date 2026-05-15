package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("repair_orders")
public class RepairOrders {

    @TableId
    private String id;

    private String orderNo;

    private String accountId;

    private String technicianAccountId;

    private String serviceTypeId;

    private String applianceBrand;

    private String applianceModel;

    private Long purchaseDate;

    private String serviceAddressId;

    private Long appointmentTime;

    private Integer status;

    private Integer paymentStatus;

    private Long startTime;

    private Long endTime;

    private Long completionTime;

    private String cancelReason;

    private Long cancelTime;

    private String refundReason;

    private BigDecimal refundAmount;

    private Long refundTime;

    private String remark;

    private Long createdTime;

    private Long updatedTime;

    @Version
    private Integer version;

    @TableLogic
    private Integer isDelete;
}
