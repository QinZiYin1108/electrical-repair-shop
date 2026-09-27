package com.example.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.backend.entity.AppointmentClosures;
import com.example.backend.entity.AppointmentReservations;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.AppointmentReservationsMapper;
import com.example.backend.model.appointment.AppointmentClosureCreateRequest;
import com.example.backend.service.AppointmentClosuresService;
import com.example.backend.service.BusinessMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class AppointmentCapacityServiceImplTests {

    private AppointmentClosuresService closuresService;
    private AppointmentReservationsMapper mapper;
    private SimpleMeterRegistry registry;
    private AppointmentCapacityServiceImpl service;

    @BeforeEach
    void setUp() {
        closuresService = mock(AppointmentClosuresService.class);
        mapper = mock(AppointmentReservationsMapper.class);
        registry = new SimpleMeterRegistry();
        service =
                new AppointmentCapacityServiceImpl(closuresService, new BusinessMetrics(registry));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        when(mapper.insert(any(AppointmentReservations.class))).thenReturn(1);
        when(mapper.updateById(any(AppointmentReservations.class))).thenReturn(1);
        when(closuresService.count(any(Wrapper.class))).thenReturn(0L);
    }

    private AppointmentReservations reservation(
            String id, String orderId, String slot, int status) {
        AppointmentReservations r = new AppointmentReservations();
        r.setId(id);
        r.setOrderId(orderId);
        r.setActiveSlot(slot);
        r.setTechnicianAccountId("TA1");
        r.setAppointmentTime(1000L);
        r.setStatus(status);
        r.setVersion(0);
        r.setIsDelete(0);
        return r;
    }

    @Test
    void reserveInsertsActiveReservation() {
        when(mapper.selectOne(any(), anyBoolean())).thenReturn(null);

        service.reserve("TA1", 1000L, "RO1");

        ArgumentCaptor<AppointmentReservations> captor =
                ArgumentCaptor.forClass(AppointmentReservations.class);
        verify(mapper).insert(captor.capture());
        AppointmentReservations saved = captor.getValue();
        assertTrue(saved.getId().startsWith("AR"));
        assertEquals("RO1", saved.getOrderId());
        assertEquals("TA1:1000", saved.getActiveSlot());
        assertEquals(AppointmentCapacityServiceImpl.STATUS_ACTIVE, saved.getStatus());
        assertEquals(1.0, registry.get("business.appointment.reserved").counter().count());
    }

    @Test
    void reserveRejectsWhenOccupiedByOtherOrder() {
        when(mapper.selectOne(any(), anyBoolean()))
                .thenReturn(null, reservation("AR9", "RO2", "TA1:1000", 1));

        assertThrows(BusinessException.class, () -> service.reserve("TA1", 1000L, "RO1"));

        assertEquals(1.0, registry.get("business.appointment.rejected").counter().count());
    }

    @Test
    void reserveRejectsWhenClosureBlocks() {
        when(mapper.selectOne(any(), anyBoolean())).thenReturn(null);
        when(closuresService.count(any(Wrapper.class))).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.reserve("TA1", 1000L, "RO1"));

        assertEquals(1.0, registry.get("business.appointment.rejected").counter().count());
    }

    @Test
    void releaseMarksReservationReleased() {
        AppointmentReservations active = reservation("AR1", "RO1", "TA1:1000", 1);
        when(mapper.selectOne(any(), anyBoolean())).thenReturn(active);

        int released = service.release("RO1", "用户取消订单");

        assertEquals(1, released);
        assertEquals(AppointmentCapacityServiceImpl.STATUS_RELEASED, active.getStatus());
        assertNull(active.getActiveSlot());
        assertEquals(1.0, registry.get("business.appointment.released").counter().count());
    }

    @Test
    void releaseSkipsWhenAlreadyReleased() {
        when(mapper.selectOne(any(), anyBoolean())).thenReturn(reservation("AR1", "RO1", null, 2));

        assertEquals(0, service.release("RO1", "取消"));
    }

    @Test
    void rescheduleRebindsToNewSlot() {
        AppointmentReservations active = reservation("AR1", "RO1", "TA1:1000", 1);
        when(mapper.selectOne(any(), anyBoolean())).thenReturn(active, null);

        service.reschedule("RO1", "TA1", 2000L);

        assertEquals("TA1:2000", active.getActiveSlot());
        assertEquals(AppointmentCapacityServiceImpl.STATUS_ACTIVE, active.getStatus());
    }

    @Test
    void createClosureValidatesAndSaves() {
        when(closuresService.save(any(AppointmentClosures.class))).thenReturn(true);
        AppointmentClosureCreateRequest request = new AppointmentClosureCreateRequest();
        request.setOwnerType("technician");
        request.setOwnerId("TA1");
        request.setStartTime(100L);
        request.setEndTime(200L);
        request.setReason("请假");

        AppointmentClosures closure = service.createClosure(request, "AA1");

        assertTrue(closure.getId().startsWith("AC"));
        assertEquals("TECHNICIAN", closure.getOwnerType());
        verify(closuresService).save(any(AppointmentClosures.class));
    }

    @Test
    void createClosureRejectsInvalidWindow() {
        AppointmentClosureCreateRequest request = new AppointmentClosureCreateRequest();
        request.setOwnerType("GLOBAL");
        request.setStartTime(200L);
        request.setEndTime(100L);

        assertThrows(BusinessException.class, () -> service.createClosure(request, "AA1"));
    }

    @Test
    void assertBookablePassesWhenNoClosureAndFree() {
        when(mapper.selectOne(any(), anyBoolean())).thenReturn(null);

        service.assertBookable("TA1", 1000L, null);

        assertEquals(0, registry.find("business.appointment.rejected").counters().size());
    }
}
