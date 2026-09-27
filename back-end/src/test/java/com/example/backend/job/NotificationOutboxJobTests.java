package com.example.backend.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.service.NotificationOutboxService;
import com.example.backend.service.SchedulerLockService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class NotificationOutboxJobTests {

    @Test
    void dispatchRunsUnderSchedulerLock() {
        NotificationOutboxService notificationOutboxService = mock(NotificationOutboxService.class);
        SchedulerLockService schedulerLockService = mock(SchedulerLockService.class);
        doAnswer(
                        inv -> {
                            ((Runnable) inv.getArgument(2)).run();
                            return null;
                        })
                .when(schedulerLockService)
                .runLocked(anyString(), anyLong(), any());
        when(notificationOutboxService.dispatchDue(anyLong(), anyInt())).thenReturn(0);

        NotificationOutboxJob job =
                new NotificationOutboxJob(notificationOutboxService, schedulerLockService);
        ReflectionTestUtils.setField(job, "batchSize", 50);

        job.dispatch();

        verify(schedulerLockService).runLocked(eq("job.notificationOutbox"), anyLong(), any());
        verify(notificationOutboxService).dispatchDue(anyLong(), eq(50));
    }
}
