package com.example.backend.job;

import com.example.backend.entity.ReconciliationBatches;
import com.example.backend.service.FundReconciliationService;
import com.example.backend.service.SchedulerLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FundReconciliationJob {

    private static final long LOOKBACK_MILLIS = 24L * 60L * 60L * 1000L;
    private static final long INCREMENTAL_LOCK_AT_MOST_FOR_MILLIS = 30L * 60L * 1000L;
    private static final long FULL_LOCK_AT_MOST_FOR_MILLIS = 120L * 60L * 1000L;

    private final FundReconciliationService reconciliationService;
    private final SchedulerLockService schedulerLockService;

    public FundReconciliationJob(
            FundReconciliationService reconciliationService,
            SchedulerLockService schedulerLockService) {
        this.reconciliationService = reconciliationService;
        this.schedulerLockService = schedulerLockService;
    }

    /** 每日增量对账：从上次批次窗口结束时间继续扫描。 */
    @Scheduled(cron = "0 30 3 * * ?")
    public void reconcileRecentFunds() {
        schedulerLockService.runLocked(
                "job.fundReconciliation.incremental",
                INCREMENTAL_LOCK_AT_MOST_FOR_MILLIS,
                this::runIncremental);
    }

    private void runIncremental() {
        long now = System.currentTimeMillis();
        Long last = reconciliationService.lastBatchWindowEnd();
        long since = last != null ? last : now - LOOKBACK_MILLIS;
        ReconciliationBatches batch =
                reconciliationService.runBatch(
                        FundReconciliationService.BATCH_TYPE_INCREMENTAL, since, now, "scheduler");
        if (batch.getIssueCount() != null && batch.getIssueCount() > 0) {
            log.warn(
                    "资金核对发现异常: batchNo={}, issueCount={}, summary={}",
                    batch.getBatchNo(),
                    batch.getIssueCount(),
                    batch.getErrorSummary());
        }
    }

    /** 每周一次全量历史对账。 */
    @Scheduled(cron = "0 0 4 ? * MON")
    public void reconcileFullFunds() {
        schedulerLockService.runLocked(
                "job.fundReconciliation.full", FULL_LOCK_AT_MOST_FOR_MILLIS, this::runFull);
    }

    private void runFull() {
        long now = System.currentTimeMillis();
        ReconciliationBatches batch =
                reconciliationService.runBatch(
                        FundReconciliationService.BATCH_TYPE_FULL, 0L, now, "scheduler");
        log.info("资金全量核对完成: batchNo={}, issueCount={}", batch.getBatchNo(), batch.getIssueCount());
    }
}
