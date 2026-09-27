package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import lombok.Data;

/** 发票。用户申请、管理员开票/驳回/红冲。 @TableName invoices */
@TableName("invoices")
@Data
public class Invoices {
    /** 主键，IV+雪花ID */
    @TableId private String id;

    /** 发票申请号 */
    private String invoiceNo;

    /** 申请用户账号ID */
    private String userAccountId;

    /** 订单类型：1-维修订单，2-商品订单 */
    private Integer orderType;

    /** 订单ID */
    private String orderId;

    /** 开票金额 */
    private BigDecimal amount;

    /** 税率 */
    private BigDecimal taxRate;

    /** 税额 */
    private BigDecimal taxAmount;

    /** 抬头类型：1-个人，2-企业 */
    private Integer titleType;

    /** 发票抬头 */
    private String title;

    /** 纳税人识别号 */
    private String taxpayerNo;

    /** 开票内容 */
    private String contentType;

    /** 状态：1-待开票，2-已开票，3-已红冲，4-已驳回 */
    private Integer status;

    /** 申请备注 */
    private String applyRemark;

    /** 开票结果/PDF地址 */
    private String invoiceUrl;

    /** 开票时间戳 */
    private Long issueTime;

    /** 驳回原因 */
    private String rejectReason;

    /** 红冲时间戳 */
    private Long redTime;

    /** 红冲原因 */
    private String redReason;

    /** 操作管理员ID */
    private String operatorAdminId;

    /** 创建时间戳 */
    private Long createdTime;

    /** 更新时间戳 */
    private Long updatedTime;

    /** 乐观锁版本号 */
    @Version private Integer version;

    /** 逻辑删除：0-未删除，1-已删除 */
    @TableLogic private Integer isDelete;
}
