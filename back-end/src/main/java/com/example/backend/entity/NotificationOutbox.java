package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

/** 可靠通知事务 outbox。业务事务只写待发送事件，后台发送器异步投递。 @TableName notification_outbox */
@TableName("notification_outbox")
@Data
public class NotificationOutbox {
    /** 主键，NO+雪花ID */
    @TableId private String id;

    /** 事件类型，如 WITHDRAWAL_APPROVED / ORDER_REFUND_SUCCEEDED */
    private String eventType;

    /** 发送渠道：IN_APP / SMS / WECHAT / EMAIL */
    private String channel;

    /** 接收人账号ID */
    private String receiverId;

    /** 接收人类型：1-用户，2-师傅，3-管理员 */
    private Integer receiverType;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 模板编码 */
    private String templateCode;

    /** 模板版本 */
    private Integer templateVersion;

    /** 模板变量（JSON） */
    private String payload;

    /** 业务对象类型 */
    private String bizType;

    /** 业务对象ID */
    private String bizId;

    /** 去重唯一键，防止重复发送 */
    private String dedupKey;

    /** 状态：1-待发送，2-发送中，3-已发送，4-待重试，5-死信 */
    private Integer status;

    /** 已重试次数 */
    private Integer retryCount;

    /** 最大重试次数 */
    private Integer maxRetry;

    /** 下次发送时间戳 */
    private Long nextRetryTime;

    /** 最近一次失败原因 */
    private String lastError;

    /** 发送成功时间戳 */
    private Long sentTime;

    /** 创建时间戳 */
    private Long createdTime;

    /** 更新时间戳 */
    private Long updatedTime;

    /** 乐观锁版本号 */
    @Version private Integer version;

    /** 逻辑删除：0-未删除，1-已删除 */
    @TableLogic private Integer isDelete;
}
