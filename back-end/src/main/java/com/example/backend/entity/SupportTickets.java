package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import lombok.Data;

/** 客服工单。关联订单/支付/退款/售后，支持分派、优先级、时限、备注与阈值审批。 @TableName support_tickets */
@TableName("support_tickets")
@Data
public class SupportTickets {
    /** 主键，TK+雪花ID */
    @TableId private String id;

    /** 工单号 */
    private String ticketNo;

    /** 工单分类 */
    private String category;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 关联业务类型 */
    private String bizType;

    /** 关联业务ID */
    private String bizId;

    /** 关联用户账号ID */
    private String relatedUserAccountId;

    /** 关联师傅账号ID */
    private String relatedTechnicianAccountId;

    /** 优先级：1-高，2-中，3-低 */
    private Integer priority;

    /** 状态：1-待处理，2-处理中，3-待审批，4-已解决，5-已关闭 */
    private Integer status;

    /** 来源：1-后台创建，2-用户提交，3-系统 */
    private Integer source;

    /** 当前处理人账号ID */
    private String assigneeAdminId;

    /** 创建人账号ID */
    private String creatorAdminId;

    /** 关联金额（用于阈值审批） */
    private BigDecimal amount;

    /** 是否需二次确认/双人审批：0-否，1-是 */
    private Integer requiresApproval;

    /** 审批人账号ID */
    private String approverAdminId;

    /** 审批时间戳 */
    private Long approveTime;

    /** 处理时限时间戳 */
    private Long dueTime;

    /** 解决时间戳 */
    private Long resolvedTime;

    /** 关闭时间戳 */
    private Long closedTime;

    /** 创建时间戳 */
    private Long createdTime;

    /** 更新时间戳 */
    private Long updatedTime;

    /** 乐观锁版本号 */
    @Version private Integer version;

    /** 逻辑删除：0-未删除，1-已删除 */
    @TableLogic private Integer isDelete;
}
