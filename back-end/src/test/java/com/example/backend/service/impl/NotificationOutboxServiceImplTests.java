package com.example.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.entity.NotificationOutbox;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.NotificationOutboxMapper;
import com.example.backend.model.notification.NotificationOutboxCommand;
import com.example.backend.service.BusinessMetrics;
import com.example.backend.service.notification.NotificationChannels;
import com.example.backend.service.notification.NotificationDeliveryException;
import com.example.backend.service.notification.NotificationOutboxStatus;
import com.example.backend.service.notification.NotificationSender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class NotificationOutboxServiceImplTests {

    private NotificationOutboxMapper mapper;
    private SimpleMeterRegistry registry;
    private BusinessMetrics metrics;

    @BeforeEach
    void setUp() {
        mapper = mock(NotificationOutboxMapper.class);
        registry = new SimpleMeterRegistry();
        metrics = new BusinessMetrics(registry);
    }

    private NotificationOutboxServiceImpl service(NotificationSender... senders) {
        NotificationOutboxServiceImpl service =
                new NotificationOutboxServiceImpl(List.of(senders), metrics);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "defaultMaxRetry", 3);
        return service;
    }

    private NotificationSender sender(
            String channel, java.util.function.Consumer<NotificationOutbox> onSend) {
        return new NotificationSender() {
            @Override
            public String channel() {
                return channel;
            }

            @Override
            public void send(NotificationOutbox message) {
                onSend.accept(message);
            }
        };
    }

    private NotificationOutbox pendingRow(String id, String channel, int retryCount, int maxRetry) {
        NotificationOutbox row = new NotificationOutbox();
        row.setId(id);
        row.setChannel(channel);
        row.setEventType("EVT");
        row.setStatus(NotificationOutboxStatus.PENDING);
        row.setRetryCount(retryCount);
        row.setMaxRetry(maxRetry);
        row.setNextRetryTime(0L);
        row.setCreatedTime(0L);
        row.setVersion(0);
        row.setIsDelete(0);
        return row;
    }

    private NotificationOutboxCommand inAppCommand(String dedupKey) {
        return new NotificationOutboxCommand(
                "EVT",
                NotificationChannels.IN_APP,
                "U1",
                1,
                "标题",
                "内容",
                null,
                null,
                null,
                "BIZ",
                "B1",
                dedupKey);
    }

    @Test
    void enqueuePersistsPendingRowAndCountsMetric() {
        when(mapper.selectOne(any(), anyBoolean())).thenReturn(null);
        when(mapper.insert(any(NotificationOutbox.class))).thenReturn(1);

        NotificationOutbox row =
                service(sender(NotificationChannels.IN_APP, m -> {})).enqueue(inAppCommand("D1"));

        assertNotNull(row);
        assertTrue(row.getId().startsWith("NO"));
        assertEquals(NotificationOutboxStatus.PENDING, row.getStatus());
        assertEquals(0, row.getRetryCount());
        assertEquals(row.getCreatedTime(), row.getNextRetryTime());
        assertEquals("D1", row.getDedupKey());
        verify(mapper).insert(any(NotificationOutbox.class));
        assertEquals(
                1.0,
                registry.get("business.notification.outbox.enqueued")
                        .tag("channel", "IN_APP")
                        .counter()
                        .count());
    }

    @Test
    void enqueueSkipsUnregisteredChannel() {
        NotificationOutbox row =
                service(sender(NotificationChannels.IN_APP, m -> {}))
                        .enqueue(
                                new NotificationOutboxCommand(
                                        "EVT",
                                        NotificationChannels.WECHAT,
                                        "U1",
                                        1,
                                        "t",
                                        "c",
                                        null,
                                        null,
                                        null,
                                        "BIZ",
                                        "B1",
                                        "D2"));

        assertNull(row);
        verify(mapper, never()).insert(any(NotificationOutbox.class));
    }

    @Test
    void enqueueReturnsExistingWhenDedupKeyPresent() {
        NotificationOutbox existing = new NotificationOutbox();
        existing.setId("NO1");
        existing.setDedupKey("D1");
        when(mapper.selectOne(any(), anyBoolean())).thenReturn(existing);

        NotificationOutbox row =
                service(sender(NotificationChannels.IN_APP, m -> {})).enqueue(inAppCommand("D1"));

        assertSame(existing, row);
        verify(mapper, never()).insert(any(NotificationOutbox.class));
    }

    @Test
    void dispatchDueMarksSentOnSuccess() {
        NotificationOutbox row = pendingRow("NO1", NotificationChannels.IN_APP, 0, 3);
        when(mapper.selectList(any())).thenReturn(List.of(row));
        when(mapper.updateById(any(NotificationOutbox.class))).thenReturn(1);

        int processed =
                service(sender(NotificationChannels.IN_APP, m -> {})).dispatchDue(1000L, 10);

        assertEquals(1, processed);
        assertEquals(NotificationOutboxStatus.SENT, row.getStatus());
        assertNotNull(row.getSentTime());
        assertNull(row.getLastError());
        assertEquals(
                1.0,
                registry.get("business.notification.outbox.sent")
                        .tag("channel", "IN_APP")
                        .counter()
                        .count());
    }

    @Test
    void dispatchDueSchedulesRetryOnFailure() {
        NotificationOutbox row = pendingRow("NO2", NotificationChannels.IN_APP, 0, 5);
        when(mapper.selectList(any())).thenReturn(List.of(row));
        when(mapper.updateById(any(NotificationOutbox.class))).thenReturn(1);

        int processed =
                service(
                                sender(
                                        NotificationChannels.IN_APP,
                                        m -> {
                                            throw new NotificationDeliveryException("boom");
                                        }))
                        .dispatchDue(1000L, 10);

        assertEquals(1, processed);
        assertEquals(NotificationOutboxStatus.RETRY, row.getStatus());
        assertEquals(1, row.getRetryCount());
        assertTrue(row.getNextRetryTime() > 1000L);
        assertTrue(row.getLastError().contains("boom"));
        assertEquals(
                1.0,
                registry.get("business.notification.outbox.failed")
                        .tag("channel", "IN_APP")
                        .counter()
                        .count());
    }

    @Test
    void dispatchDueEntersDeadLetterAtMaxRetry() {
        NotificationOutbox row = pendingRow("NO3", NotificationChannels.IN_APP, 0, 1);
        when(mapper.selectList(any())).thenReturn(List.of(row));
        when(mapper.updateById(any(NotificationOutbox.class))).thenReturn(1);

        service(
                        sender(
                                NotificationChannels.IN_APP,
                                m -> {
                                    throw new NotificationDeliveryException("boom");
                                }))
                .dispatchDue(1000L, 10);

        assertEquals(NotificationOutboxStatus.DEAD_LETTER, row.getStatus());
        assertEquals(1, row.getRetryCount());
        assertEquals(
                1.0,
                registry.get("business.notification.outbox.deadletter")
                        .tag("channel", "IN_APP")
                        .counter()
                        .count());
    }

    @Test
    void manualRetryResetsFailedRowToPending() {
        NotificationOutbox row = new NotificationOutbox();
        row.setId("NO4");
        row.setStatus(NotificationOutboxStatus.RETRY);
        row.setRetryCount(3);
        when(mapper.selectById("NO4")).thenReturn(row);
        when(mapper.updateById(any(NotificationOutbox.class))).thenReturn(1);

        NotificationOutbox result =
                service(sender(NotificationChannels.IN_APP, m -> {})).manualRetry("NO4");

        assertEquals(NotificationOutboxStatus.PENDING, result.getStatus());
        assertEquals(0, result.getRetryCount());
        assertNull(result.getLastError());
    }

    @Test
    void manualRetryRejectsSentRow() {
        NotificationOutbox row = new NotificationOutbox();
        row.setId("NO5");
        row.setStatus(NotificationOutboxStatus.SENT);
        when(mapper.selectById("NO5")).thenReturn(row);

        assertThrows(
                BusinessException.class,
                () -> service(sender(NotificationChannels.IN_APP, m -> {})).manualRetry("NO5"));
    }

    @Test
    void backoffGrowsExponentiallyAndIsCapped() {
        assertEquals(30_000L, NotificationOutboxServiceImpl.backoffMillis(1));
        assertEquals(60_000L, NotificationOutboxServiceImpl.backoffMillis(2));
        assertTrue(
                NotificationOutboxServiceImpl.backoffMillis(3)
                        > NotificationOutboxServiceImpl.backoffMillis(2));
        assertEquals(
                NotificationOutboxServiceImpl.BACKOFF_MAX_MILLIS,
                NotificationOutboxServiceImpl.backoffMillis(20));
    }
}
