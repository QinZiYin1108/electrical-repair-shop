package com.example.backend.job;

import com.example.backend.service.NotificationOutboxService;
import com.example.backend.service.SchedulerLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 通知 outbox 后台发送器：投递到期通知，失败按退避重试，超过次数进入死信。 */
@Slf4j
@Component
public class NotificationOutboxJob {

    private static final long LOCK_AT_MOST_FOR_MILLIS = 5L * 60L * 1000L;

    private final NotificationOutboxService notificationOutboxService;
    private final SchedulerLockService schedulerLockService;

    @Value("${notification.outbox.batch-size:100}")
    private int batchSize;

    public NotificationOutboxJob(
            NotificationOutboxService notificationOutboxService,
            SchedulerLockService schedulerLockService) {
        this.notificationOutboxService = notificationOutboxService;
        this.schedulerLockService = schedulerLockService;
    }

    @Scheduled(cron = "0 * * * * ?")
    public void dispatch() {
        schedulerLockService.runLocked(
                "job.notificationOutbox", LOCK_AT_MOST_FOR_MILLIS, this::doRun);
    }

    private void doRun() {
        long now = System.currentTimeMillis();
        int processed = notificationOutboxService.dispatchDue(now, Math.max(batchSize, 1));
        if (processed > 0) {
            log.info("通知 outbox 投递完成: processed={}", processed);
        }
    }
}
