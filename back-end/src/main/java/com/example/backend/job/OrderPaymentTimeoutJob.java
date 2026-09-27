package com.example.backend.job;

import com.example.backend.service.SchedulerLockService;
import com.example.backend.service.UserMallOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 关闭超时未支付的商品订单，并释放其预占的库存与优惠券。 */
@Slf4j
@Component
public class OrderPaymentTimeoutJob {

    private static final long LOCK_AT_MOST_FOR_MILLIS = 15L * 60L * 1000L;

    private final UserMallOrderService userMallOrderService;
    private final SchedulerLockService schedulerLockService;

    @Value("${order.payment.timeout-minutes:30}")
    private int timeoutMinutes;

    public OrderPaymentTimeoutJob(
            UserMallOrderService userMallOrderService, SchedulerLockService schedulerLockService) {
        this.userMallOrderService = userMallOrderService;
        this.schedulerLockService = schedulerLockService;
    }

    @Scheduled(cron = "0 */10 * * * ?")
    public void closeTimedOutOrders() {
        schedulerLockService.runLocked(
                "job.orderPaymentTimeout", LOCK_AT_MOST_FOR_MILLIS, this::doRun);
    }

    private void doRun() {
        long now = System.currentTimeMillis();
        long timeoutMillis = Math.max(timeoutMinutes, 1) * 60L * 1000L;
        int closed = userMallOrderService.closeTimedOutUnpaidOrders(now, timeoutMillis);
        if (closed > 0) {
            log.info("自动关闭超时未支付商品订单并释放预占: count={}", closed);
        }
    }
}
