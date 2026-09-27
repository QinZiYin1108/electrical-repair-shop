package com.example.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.entity.NotificationOutbox;
import com.example.backend.model.notification.NotificationOutboxCommand;
import java.util.List;

/** 可靠通知事务 outbox：业务事务只入队，后台发送器异步投递并处理重试/死信。 */
public interface NotificationOutboxService extends IService<NotificationOutbox> {

    /** 入队一条待发送通知。渠道未注册时忽略并返回 null；去重键已存在时返回已有记录。 */
    NotificationOutbox enqueue(NotificationOutboxCommand command);

    /** 入队一条站内通知（IN_APP），业务侧常用便捷方法。 */
    NotificationOutbox enqueueInApp(
            String eventType,
            String receiverId,
            Integer receiverType,
            String title,
            String content,
            String bizType,
            String bizId,
            String dedupKey);

    /** 投递所有到期的待发送/待重试记录，返回处理条数。 */
    int dispatchDue(long now, int batchSize);

    /** 管理端人工重试：将失败/死信记录重置为待发送。 */
    NotificationOutbox manualRetry(String id);

    /** 管理端按状态查询（status 为 null 时全部）。 */
    List<NotificationOutbox> listForAdmin(Integer status, int limit);
}
