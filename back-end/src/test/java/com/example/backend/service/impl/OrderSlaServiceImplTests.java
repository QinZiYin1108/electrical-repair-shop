package com.example.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.domain.order.OrderSlaType;
import com.example.backend.domain.order.RepairOrderStateMachine;
import com.example.backend.entity.OrderProgress;
import com.example.backend.entity.OrderSlaEvents;
import com.example.backend.entity.RepairOrders;
import com.example.backend.mapper.OrderSlaEventsMapper;
import com.example.backend.service.AppointmentCapacityService;
import com.example.backend.service.BusinessMetrics;
import com.example.backend.service.NotificationOutboxService;
import com.example.backend.service.OrderProgressService;
import com.example.backend.service.RepairOrderCommandService;
import com.example.backend.service.RepairOrdersService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class OrderSlaServiceImplTests {

    private static final long MIN = 60_000L;

    private RepairOrdersService repairOrdersService;
    private OrderProgressService orderProgressService;
    private NotificationOutboxService notificationOutboxService;
    private OrderSlaEventsMapper mapper;
    private SimpleMeterRegistry registry;
    private OrderSlaServiceImpl service;

    @BeforeEach
    void setUp() {
        repairOrdersService = mock(RepairOrdersService.class);
        orderProgressService = mock(OrderProgressService.class);
        notificationOutboxService = mock(NotificationOutboxService.class);
        mapper = mock(OrderSlaEventsMapper.class);
        registry = new SimpleMeterRegistry();
        RepairOrderStateMachine stateMachine = new RepairOrderStateMachine();
        RepairOrderCommandService commandService =
                new RepairOrderCommandServiceImpl(repairOrdersService, stateMachine);
        service =
                new OrderSlaServiceImpl(
                        repairOrdersService,
                        stateMachine,
                        commandService,
                        orderProgressService,
                        notificationOutboxService,
                        mock(AppointmentCapacityService.class),
                        new BusinessMetrics(registry));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "adminAccountId", "AA1");
        when(mapper.insert(any(OrderSlaEvents.class))).thenReturn(1);
        when(mapper.updateById(any(OrderSlaEvents.class))).thenReturn(1);
        when(repairOrdersService.updateById(any(RepairOrders.class))).thenReturn(true);
    }

    private RepairOrders order(String id, int status, int paymentStatus, long updatedTime) {
        RepairOrders order = new RepairOrders();
        order.setId(id);
        order.setOrderNo("NO-" + id);
        order.setAccountId("U1");
        order.setStatus(status);
        order.setPaymentStatus(paymentStatus);
        order.setCreatedTime(updatedTime);
        order.setUpdatedTime(updatedTime);
        return order;
    }

    @Test
    void acceptOverdueCreatesEventAndNotifiesAdmin() {
        when(mapper.selectOne(any(), anyBoolean())).thenReturn(null);
        long now = 1_000_000_000L;
        RepairOrders order = order("RO1", 1, 1, now - 40 * MIN);

        boolean handled = service.processOverdueOrder(order, OrderSlaType.ACCEPT, now);

        assertTrue(handled);
        ArgumentCaptor<OrderSlaEvents> captor = ArgumentCaptor.forClass(OrderSlaEvents.class);
        verify(mapper).insert(captor.capture());
        OrderSlaEvents event = captor.getValue();
        assertEquals("RO1", event.getOrderId());
        assertEquals(OrderSlaType.ACCEPT.name(), event.getSlaType());
        assertEquals(OrderSlaServiceImpl.EVENT_STATUS_ACTIVE, event.getStatus());
        assertEquals(OrderSlaServiceImpl.ESCALATION_REMIND, event.getEscalationLevel());
        assertEquals(OrderSlaServiceImpl.ACTION_REMIND, event.getActionTaken());
        assertEquals(1, event.getNotifyCount());
        assertEquals("PLATFORM", event.getResponsibleParty());
        verify(notificationOutboxService)
                .enqueueInApp(
                        anyString(),
                        eq("AA1"),
                        eq(3),
                        anyString(),
                        anyString(),
                        anyString(),
                        eq("RO1"),
                        anyString());
        assertEquals(1.0, slaCount("business.order.sla.exceeded", "ACCEPT"));
    }

    @Test
    void acceptOverdueBeyondCancelAutoCancelsViaStateMachine() {
        long now = 1_000_000_000L;
        RepairOrders order = order("RO1", 1, 1, now - 130 * MIN);
        OrderSlaEvents existing = activeEvent("SL1", "RO1", OrderSlaType.ACCEPT, 1, 1);
        when(mapper.selectOne(any(), anyBoolean())).thenReturn(existing);

        boolean handled = service.processOverdueOrder(order, OrderSlaType.ACCEPT, now);

        assertTrue(handled);
        assertEquals(7, order.getStatus());
        verify(repairOrdersService).updateById(order);
        verify(orderProgressService).save(any(OrderProgress.class));
        assertEquals(OrderSlaServiceImpl.EVENT_STATUS_AUTO_HANDLED, existing.getStatus());
        assertEquals(OrderSlaServiceImpl.ACTION_AUTO_CANCEL, existing.getActionTaken());
        assertEquals(1.0, slaCount("business.order.sla.auto.canceled", "ACCEPT"));
    }

    @Test
    void paymentOverdueNotifiesUser() {
        when(mapper.selectOne(any(), anyBoolean())).thenReturn(null);
        long now = 1_000_000_000L;
        RepairOrders order = order("RO2", 4, 1, now - 1500 * MIN);

        boolean handled = service.processOverdueOrder(order, OrderSlaType.PAYMENT, now);

        assertTrue(handled);
        verify(notificationOutboxService)
                .enqueueInApp(
                        anyString(),
                        eq("U1"),
                        eq(1),
                        anyString(),
                        anyString(),
                        anyString(),
                        eq("RO2"),
                        anyString());
        assertEquals(1.0, slaCount("business.order.sla.exceeded", "PAYMENT"));
    }

    @Test
    void escalatedWhenOverdueExceedsReminderMultiple() {
        long now = 1_000_000_000L;
        RepairOrders order = order("RO1", 1, 1, now - 70 * MIN);
        OrderSlaEvents existing = activeEvent("SL1", "RO1", OrderSlaType.ACCEPT, 1, 1);
        when(mapper.selectOne(any(), anyBoolean())).thenReturn(existing);

        service.processOverdueOrder(order, OrderSlaType.ACCEPT, now);

        assertEquals(OrderSlaServiceImpl.ESCALATION_ESCALATED, existing.getEscalationLevel());
        assertEquals(OrderSlaServiceImpl.ACTION_ESCALATE, existing.getActionTaken());
        assertEquals(2, existing.getNotifyCount());
        assertEquals(1.0, slaCount("business.order.sla.escalated", "ACCEPT"));
    }

    @Test
    void resolveStaleEventWhenOrderLeftStatus() {
        OrderSlaEvents active = activeEvent("SL2", "RO1", OrderSlaType.VISIT, 1, 1);
        when(mapper.selectList(any())).thenReturn(List.of(active));
        RepairOrders progressed = order("RO1", 6, 2, 0L);
        when(repairOrdersService.getById("RO1")).thenReturn(progressed);

        int handled = service.resolveStaleEvents(1_000_000_000L);

        assertEquals(1, handled);
        assertEquals(OrderSlaServiceImpl.EVENT_STATUS_RECOVERED, active.getStatus());
        assertEquals(1.0, slaCount("business.order.sla.recovered", "VISIT"));
    }

    @Test
    void notOverdueProducesNothing() {
        long now = 1_000_000_000L;
        RepairOrders order = order("RO1", 1, 1, now - 5 * MIN);

        boolean handled = service.processOverdueOrder(order, OrderSlaType.ACCEPT, now);

        assertFalse(handled);
        verify(mapper, never()).insert(any(OrderSlaEvents.class));
        verify(notificationOutboxService, never())
                .enqueueInApp(
                        anyString(),
                        anyString(),
                        anyInt(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString());
    }

    private OrderSlaEvents activeEvent(
            String id, String orderId, OrderSlaType type, int escalation, int notifyCount) {
        OrderSlaEvents event = new OrderSlaEvents();
        event.setId(id);
        event.setOrderId(orderId);
        event.setSlaType(type.name());
        event.setStatus(OrderSlaServiceImpl.EVENT_STATUS_ACTIVE);
        event.setEscalationLevel(escalation);
        event.setNotifyCount(notifyCount);
        event.setVersion(0);
        event.setIsDelete(0);
        return event;
    }

    private double slaCount(String name, String slaType) {
        return registry.get(name).tag("slaType", slaType).counter().count();
    }
}
