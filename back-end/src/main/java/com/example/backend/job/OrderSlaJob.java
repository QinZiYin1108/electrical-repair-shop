package com.example.backend.job;

import com.example.backend.service.OrderSlaService;
import com.example.backend.service.SchedulerLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 订单 SLA 超时引擎定时任务：扫描超时订单，记录事件、提醒/升级并（未接单）自动取消。 */
@Slf4j
@Component
public class OrderSlaJob {

    private static final long LOCK_AT_MOST_FOR_MILLIS = 10L * 60L * 1000L;

    private final OrderSlaService orderSlaService;
    private final SchedulerLockService schedulerLockService;

    public OrderSlaJob(OrderSlaService orderSlaService, SchedulerLockService schedulerLockService) {
        this.orderSlaService = orderSlaService;
        this.schedulerLockService = schedulerLockService;
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void scan() {
        schedulerLockService.runLocked("job.orderSla", LOCK_AT_MOST_FOR_MILLIS, this::doRun);
    }

    private void doRun() {
        int handled = orderSlaService.scanAndHandle(System.currentTimeMillis());
        if (handled > 0) {
            log.info("订单 SLA 扫描完成: handled={}", handled);
        }
    }
}
