package com.example.backend.job;

import com.example.backend.service.RepairOrderFundService;
import com.example.backend.service.SchedulerLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 维修人员自动结算：扫描售后保护期已结束的冻结收入并转为可提现。 */
@Slf4j
@Component
public class TechnicianSettlementJob {

    private static final long LOCK_AT_MOST_FOR_MILLIS = 60L * 60L * 1000L;

    private final RepairOrderFundService repairOrderFundService;
    private final SchedulerLockService schedulerLockService;

    public TechnicianSettlementJob(
            RepairOrderFundService repairOrderFundService,
            SchedulerLockService schedulerLockService) {
        this.repairOrderFundService = repairOrderFundService;
        this.schedulerLockService = schedulerLockService;
    }

    @Scheduled(cron = "0 0 4 * * ?")
    public void settleTechnicianFunds() {
        schedulerLockService.runLocked(
                "job.technicianSettlement", LOCK_AT_MOST_FOR_MILLIS, this::doRun);
    }

    private void doRun() {
        long now = System.currentTimeMillis();
        int released = repairOrderFundService.releaseAllEligibleTechnicianFunds(now);
        if (released > 0) {
            log.info("维修人员自动结算完成: releasedFlows={}", released);
        }
    }
}
