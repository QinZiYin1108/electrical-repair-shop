package com.example.backend.model.notification;

/**
 * 通知 outbox 入队命令。
 *
 * @param eventType 事件类型
 * @param channel 发送渠道（见 {@code NotificationChannels}）
 * @param receiverId 接收人账号ID
 * @param receiverType 接收人类型：1-用户，2-师傅，3-管理员
 * @param title 通知标题
 * @param content 通知内容
 * @param templateCode 模板编码
 * @param templateVersion 模板版本
 * @param payload 模板变量（JSON）
 * @param bizType 业务对象类型
 * @param bizId 业务对象ID
 * @param dedupKey 去重唯一键（必填，防止重复发送）
 */
public record NotificationOutboxCommand(
        String eventType,
        String channel,
        String receiverId,
        Integer receiverType,
        String title,
        String content,
        String templateCode,
        Integer templateVersion,
        String payload,
        String bizType,
        String bizId,
        String dedupKey) {}
