package com.example.backend.job;

import com.example.backend.service.PenaltyRecordsService;
import com.example.backend.service.SchedulerLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 封禁到期自动解封任务。 每小时执行一次，检查 ban_end_time 已过期的封禁记录，自动将状态改为已过期。 */
@Component
public class BanExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(BanExpiryJob.class);
    private static final long LOCK_AT_MOST_FOR_MILLIS = 30L * 60L * 1000L;

    private final PenaltyRecordsService penaltyRecordsService;
    private final SchedulerLockService schedulerLockService;

    public BanExpiryJob(
            PenaltyRecordsService penaltyRecordsService,
            SchedulerLockService schedulerLockService) {
        this.penaltyRecordsService = penaltyRecordsService;
        this.schedulerLockService = schedulerLockService;
    }

    /** 每小时执行一次 */
    @Scheduled(cron = "0 0 * * * ?")
    public void run() {
        schedulerLockService.runLocked("job.banExpiry", LOCK_AT_MOST_FOR_MILLIS, this::doRun);
    }

    private void doRun() {
        long now = System.currentTimeMillis();

        int expiredCount = penaltyRecordsService.expireBans(now);
        if (expiredCount > 0) {
            log.info("封禁到期自动解封任务完成，共处理 {} 条", expiredCount);
        }
    }
}
