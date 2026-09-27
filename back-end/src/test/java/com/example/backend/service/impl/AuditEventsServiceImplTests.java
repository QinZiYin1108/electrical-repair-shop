package com.example.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.entity.AuditEvents;
import com.example.backend.mapper.AuditEventsMapper;
import com.example.backend.model.audit.AuditEventCommand;
import com.example.backend.security.context.RequestContext;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class AuditEventsServiceImplTests {

    @Test
    void recordPersistsStructuredEventWithRequestContext() {
        AuditEventsMapper mapper = mock(AuditEventsMapper.class);
        when(mapper.insert(any(AuditEvents.class))).thenReturn(1);
        AuditEventsServiceImpl service = new AuditEventsServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);

        RequestContext.set("req-1", "10.0.0.1");
        try {
            service.record(
                    new AuditEventCommand(
                            "ORDER_REFUND",
                            "REPAIR_ORDER",
                            "RO1",
                            "7",
                            "8",
                            null,
                            new BigDecimal("20.00"),
                            "售后退款",
                            "AS1"));
        } finally {
            RequestContext.clear();
        }

        ArgumentCaptor<AuditEvents> captor = ArgumentCaptor.forClass(AuditEvents.class);
        verify(mapper).insert(captor.capture());
        AuditEvents event = captor.getValue();
        assertEquals("ORDER_REFUND", event.getEventType());
        assertEquals("REPAIR_ORDER", event.getBizType());
        assertEquals("RO1", event.getBizId());
        assertEquals("7", event.getBeforeState());
        assertEquals("8", event.getAfterState());
        assertEquals(new BigDecimal("20.00"), event.getAmountAfter());
        assertEquals("req-1", event.getRequestId());
        assertEquals("10.0.0.1", event.getSourceIp());
        assertEquals("AS1", event.getRelatedId());
        assertNotNull(event.getId());
    }
}
