package com.example.backend.job;

import com.example.backend.service.CreditRecordsService;
import com.example.backend.service.SchedulerLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 信用积分自动恢复任务。 每天凌晨 2 点执行，扫描连续 30 天无新增违规的账号，自动恢复 5 分。 */
@Component
public class CreditRecoveryJob {

    private static final Logger log = LoggerFactory.getLogger(CreditRecoveryJob.class);
    private static final long LOCK_AT_MOST_FOR_MILLIS = 60L * 60L * 1000L;

    /** 无违规天数阈值（默认 30 天） */
    private static final int NO_VIOLATION_DAYS = 30;

    /** 每次恢复积分 */
    private static final int RECOVERY_SCORE = 5;

    private final CreditRecordsService creditRecordsService;
    private final SchedulerLockService schedulerLockService;

    public CreditRecoveryJob(
            CreditRecordsService creditRecordsService, SchedulerLockService schedulerLockService) {
        this.creditRecordsService = creditRecordsService;
        this.schedulerLockService = schedulerLockService;
    }

    /** 每天凌晨 2:00 执行 */
    @Scheduled(cron = "0 0 2 * * ?")
    public void run() {
        schedulerLockService.runLocked("job.creditRecovery", LOCK_AT_MOST_FOR_MILLIS, this::doRun);
    }

    private void doRun() {
        log.info("信用积分自动恢复任务开始");
        try {
            creditRecordsService.recoverNoViolationScore(NO_VIOLATION_DAYS, RECOVERY_SCORE);
            log.info("信用积分自动恢复任务完成");
        } catch (Exception e) {
            log.error("信用积分自动恢复任务异常", e);
        }
    }
}
