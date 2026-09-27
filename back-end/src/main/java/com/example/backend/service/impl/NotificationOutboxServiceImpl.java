package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.NotificationOutbox;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.NotificationOutboxMapper;
import com.example.backend.model.notification.NotificationOutboxCommand;
import com.example.backend.service.BusinessMetrics;
import com.example.backend.service.NotificationOutboxService;
import com.example.backend.service.notification.NotificationChannels;
import com.example.backend.service.notification.NotificationDeliveryException;
import com.example.backend.service.notification.NotificationOutboxStatus;
import com.example.backend.service.notification.NotificationSender;
import com.example.backend.utils.id.SnowflakeIdUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 可靠通知 outbox 实现。入队只在业务事务内写一行；投递由 {@code NotificationOutboxJob} 异步触发，
 * 失败按指数退避重试，超过最大次数进入死信，管理端可人工重试。
 */
@Service
public class NotificationOutboxServiceImpl
        extends ServiceImpl<NotificationOutboxMapper, NotificationOutbox>
        implements NotificationOutboxService {

    private static final Logger log = LoggerFactory.getLogger(NotificationOutboxServiceImpl.class);

    /** 退避基数 30s。 */
    static final long BACKOFF_BASE_MILLIS = 30L * 1000L;

    /** 退避上限 30min。 */
    static final long BACKOFF_MAX_MILLIS = 30L * 60L * 1000L;

    private final Map<String, NotificationSender> senderByChannel = new HashMap<>();
    private final BusinessMetrics businessMetrics;

    @Value("${notification.outbox.max-retry:5}")
    private int defaultMaxRetry = 5;

    public NotificationOutboxServiceImpl(
            List<NotificationSender> senders, BusinessMetrics businessMetrics) {
        this.businessMetrics = businessMetrics;
        if (senders != null) {
            for (NotificationSender sender : senders) {
                this.senderByChannel.put(sender.channel(), sender);
            }
        }
    }

    @Override
    public NotificationOutbox enqueue(NotificationOutboxCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "通知入队命令不能为空");
        }
        requireText(command.eventType(), "事件类型不能为空");
        requireText(command.channel(), "发送渠道不能为空");
        requireText(command.dedupKey(), "去重键不能为空");

        if (!senderByChannel.containsKey(command.channel())) {
            log.debug(
                    "跳过未注册渠道的通知入队: channel={}, eventType={}",
                    command.channel(),
                    command.eventType());
            return null;
        }

        NotificationOutbox existing = findByDedupKey(command.dedupKey());
        if (existing != null) {
            return existing;
        }

        long now = System.currentTimeMillis();
        NotificationOutbox row = new NotificationOutbox();
        row.setId(SnowflakeIdUtil.nextNotificationOutboxId());
        row.setEventType(command.eventType());
        row.setChannel(command.channel());
        row.setReceiverId(command.receiverId());
        row.setReceiverType(command.receiverType());
        row.setTitle(command.title());
        row.setContent(command.content());
        row.setTemplateCode(command.templateCode());
        row.setTemplateVersion(command.templateVersion());
        row.setPayload(command.payload());
        row.setBizType(command.bizType());
        row.setBizId(command.bizId());
        row.setDedupKey(command.dedupKey());
        row.setStatus(NotificationOutboxStatus.PENDING);
        row.setRetryCount(0);
        row.setMaxRetry(Math.max(defaultMaxRetry, 1));
        row.setNextRetryTime(now);
        row.setCreatedTime(now);
        row.setUpdatedTime(now);
        row.setVersion(0);
        row.setIsDelete(0);
        try {
            if (!save(row)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "入队通知失败");
            }
        } catch (DuplicateKeyException ex) {
            // 并发下唯一键冲突：视为重复事件，返回已有记录
            return findByDedupKey(command.dedupKey());
        }
        businessMetrics.notificationOutboxEnqueued(command.channel());
        return row;
    }

    @Override
    public NotificationOutbox enqueueInApp(
            String eventType,
            String receiverId,
            Integer receiverType,
            String title,
            String content,
            String bizType,
            String bizId,
            String dedupKey) {
        return enqueue(
                new NotificationOutboxCommand(
                        eventType,
                        NotificationChannels.IN_APP,
                        receiverId,
                        receiverType,
                        title,
                        content,
                        null,
                        null,
                        null,
                        bizType,
                        bizId,
                        dedupKey));
    }

    @Override
    public int dispatchDue(long now, int batchSize) {
        int size = Math.min(Math.max(batchSize, 1), 500);
        List<NotificationOutbox> due =
                list(
                        new LambdaQueryWrapper<NotificationOutbox>()
                                .in(
                                        NotificationOutbox::getStatus,
                                        NotificationOutboxStatus.PENDING,
                                        NotificationOutboxStatus.RETRY)
                                .le(NotificationOutbox::getNextRetryTime, now)
                                .orderByAsc(NotificationOutbox::getCreatedTime)
                                .last("limit " + size));
        int processed = 0;
        for (NotificationOutbox row : due) {
            dispatch(row, now);
            processed++;
        }
        return processed;
    }

    @Override
    public NotificationOutbox manualRetry(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "通知ID不能为空");
        }
        NotificationOutbox row = getById(id);
        if (row == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "通知不存在");
        }
        Integer status = row.getStatus();
        if (status == null
                || (status != NotificationOutboxStatus.RETRY
                        && status != NotificationOutboxStatus.DEAD_LETTER)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "仅失败或死信的通知可重试");
        }
        long now = System.currentTimeMillis();
        row.setStatus(NotificationOutboxStatus.PENDING);
        row.setRetryCount(0);
        row.setNextRetryTime(now);
        row.setLastError(null);
        row.setUpdatedTime(now);
        if (!updateById(row)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "重置通知失败");
        }
        return row;
    }

    @Override
    public List<NotificationOutbox> listForAdmin(Integer status, int limit) {
        LambdaQueryWrapper<NotificationOutbox> wrapper =
                new LambdaQueryWrapper<NotificationOutbox>()
                        .orderByDesc(NotificationOutbox::getCreatedTime)
                        .last("limit " + Math.min(Math.max(limit, 1), 200));
        if (status != null) {
            wrapper.eq(NotificationOutbox::getStatus, status);
        }
        return list(wrapper);
    }

    private void dispatch(NotificationOutbox row, long now) {
        String channel = row.getChannel();
        NotificationSender sender = senderByChannel.get(channel);
        try {
            if (sender == null) {
                throw new NotificationDeliveryException("未注册发送渠道: " + channel);
            }
            sender.send(row);
            markSent(row, now);
            businessMetrics.notificationOutboxSent(channel);
        } catch (Exception ex) {
            boolean dead = markFailed(row, ex, now);
            if (dead) {
                businessMetrics.notificationOutboxDeadLetter(channel);
                log.warn(
                        "通知进入死信: id={}, channel={}, error={}",
                        row.getId(),
                        channel,
                        ex.getMessage());
            } else {
                businessMetrics.notificationOutboxFailed(channel);
                log.info(
                        "通知投递失败，稍后重试: id={}, channel={}, error={}",
                        row.getId(),
                        channel,
                        ex.getMessage());
            }
        }
    }

    private void markSent(NotificationOutbox row, long now) {
        row.setStatus(NotificationOutboxStatus.SENT);
        row.setSentTime(now);
        row.setLastError(null);
        row.setUpdatedTime(now);
        updateById(row);
    }

    /** 标记失败并计算退避；返回是否进入死信。 */
    private boolean markFailed(NotificationOutbox row, Exception ex, long now) {
        int retryCount = row.getRetryCount() == null ? 0 : row.getRetryCount();
        retryCount++;
        int maxRetry = row.getMaxRetry() == null ? Math.max(defaultMaxRetry, 1) : row.getMaxRetry();
        row.setRetryCount(retryCount);
        row.setLastError(truncate(ex.getMessage()));
        row.setUpdatedTime(now);
        boolean dead = retryCount >= maxRetry;
        if (dead) {
            row.setStatus(NotificationOutboxStatus.DEAD_LETTER);
            row.setNextRetryTime(now);
        } else {
            row.setStatus(NotificationOutboxStatus.RETRY);
            row.setNextRetryTime(now + backoffMillis(retryCount));
        }
        updateById(row);
        return dead;
    }

    /** 指数退避：base * 2^(retryCount-1)，封顶 {@link #BACKOFF_MAX_MILLIS}。 */
    static long backoffMillis(int retryCount) {
        int shift = Math.min(Math.max(retryCount - 1, 0), 16);
        long delay = BACKOFF_BASE_MILLIS << shift;
        return Math.min(delay, BACKOFF_MAX_MILLIS);
    }

    private NotificationOutbox findByDedupKey(String dedupKey) {
        return getOne(
                new LambdaQueryWrapper<NotificationOutbox>()
                        .eq(NotificationOutbox::getDedupKey, dedupKey)
                        .last("limit 1"),
                false);
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
