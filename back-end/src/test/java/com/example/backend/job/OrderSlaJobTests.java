package com.example.backend.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.service.OrderSlaService;
import com.example.backend.service.SchedulerLockService;
import org.junit.jupiter.api.Test;

class OrderSlaJobTests {

    @Test
    void scanRunsUnderSchedulerLock() {
        OrderSlaService orderSlaService = mock(OrderSlaService.class);
        SchedulerLockService schedulerLockService = mock(SchedulerLockService.class);
        doAnswer(
                        inv -> {
                            ((Runnable) inv.getArgument(2)).run();
                            return null;
                        })
                .when(schedulerLockService)
                .runLocked(anyString(), anyLong(), any());
        when(orderSlaService.scanAndHandle(anyLong())).thenReturn(0);

        new OrderSlaJob(orderSlaService, schedulerLockService).scan();

        verify(schedulerLockService).runLocked(eq("job.orderSla"), anyLong(), any());
        verify(orderSlaService).scanAndHandle(anyLong());
    }
}
