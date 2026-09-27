package com.example.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.backend.domain.support.SupportTicketAction;
import com.example.backend.domain.support.SupportTicketStatus;
import com.example.backend.entity.SupportTicketLogs;
import com.example.backend.entity.SupportTickets;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.SupportTicketsMapper;
import com.example.backend.model.support.SupportTicketCreateRequest;
import com.example.backend.model.support.SupportTicketDetailResponse;
import com.example.backend.service.AuditEventsService;
import com.example.backend.service.BusinessMetrics;
import com.example.backend.service.SupportTicketLogsService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class SupportTicketServiceImplTests {

    private SupportTicketLogsService logsService;
    private AuditEventsService auditEventsService;
    private SupportTicketsMapper mapper;
    private SimpleMeterRegistry registry;
    private SupportTicketServiceImpl service;

    @BeforeEach
    void setUp() {
        logsService = mock(SupportTicketLogsService.class);
        auditEventsService = mock(AuditEventsService.class);
        mapper = mock(SupportTicketsMapper.class);
        registry = new SimpleMeterRegistry();
        service =
                new SupportTicketServiceImpl(
                        logsService, auditEventsService, new BusinessMetrics(registry));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "approvalAmountThreshold", new BigDecimal("1000"));
        ReflectionTestUtils.setField(service, "defaultDueHours", 24);
        when(mapper.insert(any(SupportTickets.class))).thenReturn(1);
        when(mapper.updateById(any(SupportTickets.class))).thenReturn(1);
    }

    private SupportTicketCreateRequest request(String title, BigDecimal amount) {
        SupportTicketCreateRequest request = new SupportTicketCreateRequest();
        request.setTitle(title);
        request.setContent("详情");
        request.setAmount(amount);
        return request;
    }

    private SupportTickets ticket(String id, int status, String creator, String approver) {
        SupportTickets ticket = new SupportTickets();
        ticket.setId(id);
        ticket.setTicketNo("TK-" + id);
        ticket.setStatus(status);
        ticket.setCreatorAdminId(creator);
        ticket.setApproverAdminId(approver);
        ticket.setVersion(0);
        ticket.setIsDelete(0);
        return ticket;
    }

    @Test
    void createWithoutAmountStartsPendingAndLogs() {
        SupportTickets created = service.create(request("咨询", null), "AA1");

        assertEquals(SupportTicketStatus.PENDING.getCode(), created.getStatus());
        assertEquals(0, created.getRequiresApproval());
        assertTrue(created.getTicketNo().startsWith("TK"));
        assertNotNull(created.getDueTime());
        ArgumentCaptor<SupportTicketLogs> logCaptor =
                ArgumentCaptor.forClass(SupportTicketLogs.class);
        verify(logsService).save(logCaptor.capture());
        assertEquals(SupportTicketAction.CREATE, logCaptor.getValue().getAction());
        verify(auditEventsService).record(any());
        assertEquals(1.0, registry.get("business.support.ticket.created").counter().count());
        assertEquals(
                0, registry.find("business.support.ticket.approval.required").counters().size());
    }

    @Test
    void createWithAmountOverThresholdRequiresApproval() {
        SupportTickets created = service.create(request("大额退款", new BigDecimal("5000")), "AA1");

        assertEquals(SupportTicketStatus.PENDING_APPROVAL.getCode(), created.getStatus());
        assertEquals(1, created.getRequiresApproval());
        assertEquals(
                1.0, registry.get("business.support.ticket.approval.required").counter().count());
    }

    @Test
    void changeStatusRejectsResolvedWhenApprovalRequired() {
        SupportTickets t = ticket("TK1", 2, "AA1", null);
        t.setRequiresApproval(1);
        when(mapper.selectById("TK1")).thenReturn(t);

        assertThrows(
                BusinessException.class,
                () ->
                        service.changeStatus(
                                "TK1", SupportTicketStatus.RESOLVED.getCode(), "AA2", "解决"));
    }

    @Test
    void approveRejectsSelfApproval() {
        when(mapper.selectById("TK1"))
                .thenReturn(
                        ticket("TK1", SupportTicketStatus.PENDING_APPROVAL.getCode(), "AA1", null));

        assertThrows(BusinessException.class, () -> service.approve("TK1", "AA1", "同意"));
    }

    @Test
    void approveMovesToProcessing() {
        when(mapper.selectById("TK1"))
                .thenReturn(
                        ticket("TK1", SupportTicketStatus.PENDING_APPROVAL.getCode(), "AA1", null));

        SupportTickets result = service.approve("TK1", "AA2", "同意");

        assertEquals(SupportTicketStatus.PROCESSING.getCode(), result.getStatus());
        assertEquals("AA2", result.getApproverAdminId());
        assertNotNull(result.getApproveTime());
        assertEquals(1.0, registry.get("business.support.ticket.approved").counter().count());
    }

    @Test
    void approveRejectsNonPendingApprovalTicket() {
        when(mapper.selectById("TK1")).thenReturn(ticket("TK1", 2, "AA1", null));

        assertThrows(BusinessException.class, () -> service.approve("TK1", "AA2", "同意"));
    }

    @Test
    void assignMovesPendingTicketToProcessing() {
        when(mapper.selectById("TK1")).thenReturn(ticket("TK1", 1, "AA1", null));

        SupportTickets result = service.assign("TK1", "AA2", "AA1", "取单");

        assertEquals("AA2", result.getAssigneeAdminId());
        assertEquals(SupportTicketStatus.PROCESSING.getCode(), result.getStatus());
    }

    @Test
    void detailReturnsTicketAndLogs() {
        when(mapper.selectById("TK1")).thenReturn(ticket("TK1", 1, "AA1", null));
        SupportTicketLogs log = new SupportTicketLogs();
        log.setAction(SupportTicketAction.CREATE);
        when(logsService.list(any(Wrapper.class))).thenReturn(List.of(log));

        SupportTicketDetailResponse response = service.detail("TK1");

        assertEquals("TK1", response.ticket().getId());
        assertEquals(1, response.logs().size());
    }
}
