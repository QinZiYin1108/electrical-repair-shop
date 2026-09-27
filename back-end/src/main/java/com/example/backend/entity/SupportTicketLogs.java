package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

/** 客服工单操作记录（分派/优先级/状态/备注/审批等）。 @TableName support_ticket_logs */
@TableName("support_ticket_logs")
@Data
public class SupportTicketLogs {
    /** 主键，TL+雪花ID */
    @TableId private String id;

    /** 工单ID */
    private String ticketId;

    /** 动作：CREATE/ASSIGN/PRIORITY/STATUS/COMMENT/APPROVE/REJECT/ATTACH */
    private String action;

    /** 操作人账号ID */
    private String operatorAdminId;

    /** 操作人名称 */
    private String operatorName;

    /** 操作内容/备注 */
    private String content;

    /** 变更前状态 */
    private Integer fromStatus;

    /** 变更后状态 */
    private Integer toStatus;

    /** 附件（JSON 数组） */
    private String attachments;

    /** 创建时间戳 */
    private Long createdTime;

    /** 乐观锁版本号 */
    @Version private Integer version;

    /** 逻辑删除：0-未删除，1-已删除 */
    @TableLogic private Integer isDelete;
}
