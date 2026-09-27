package com.example.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.backend.entity.FundFlows;
import com.example.backend.entity.ReconciliationBatches;
import com.example.backend.entity.ReconciliationIssues;
import com.example.backend.service.AccountBalancesService;
import com.example.backend.service.BusinessMetrics;
import com.example.backend.service.FundFlowsService;
import com.example.backend.service.PaymentRecordsService;
import com.example.backend.service.ReconciliationBatchesService;
import com.example.backend.service.ReconciliationIssuesService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FundReconciliationServiceImplTests {
    private AccountBalancesService balances;
    private FundFlowsService flows;
    private PaymentRecordsService payments;
    private ReconciliationBatchesService batches;
    private ReconciliationIssuesService issues;
    private FundReconciliationServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        balances = mock(AccountBalancesService.class);
        flows = mock(FundFlowsService.class);
        payments = mock(PaymentRecordsService.class);
        batches = mock(ReconciliationBatchesService.class);
        issues = mock(ReconciliationIssuesService.class);
        service =
                new FundReconciliationServiceImpl(
                        balances, flows, payments, batches, issues, mock(BusinessMetrics.class));
        when(balances.list(any(Wrapper.class))).thenReturn(List.of());
        when(payments.list(any(Wrapper.class))).thenReturn(List.of());
    }

    @Test
    @SuppressWarnings("unchecked")
    void batchPersistsIssueForMissingIdempotencyKey() {
        FundFlows flow = new FundFlows();
        flow.setId("FF1");
        flow.setIdempotencyKey(null);
        when(flows.list(any(Wrapper.class))).thenReturn(List.of(flow));
        when(flows.count(any(Wrapper.class))).thenReturn(1L);
        when(batches.save(any(ReconciliationBatches.class))).thenReturn(true);
        when(issues.saveBatch(any())).thenReturn(true);

        ReconciliationBatches batch = service.runBatch(1, 0L, 1000L, "test");

        assertEquals(2, batch.getStatus());
        assertEquals(1, batch.getIssueCount());
        assertNotNull(batch.getBatchNo());
        ArgumentCaptor<List<ReconciliationIssues>> captor = ArgumentCaptor.forClass(List.class);
        verify(issues).saveBatch(captor.capture());
        assertEquals("FLOW_MISSING_IDEMPOTENCY_KEY", captor.getValue().get(0).getCategory());
        assertEquals("FF1", captor.getValue().get(0).getBizId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void healthyBatchHasNoIssues() {
        when(flows.list(any(Wrapper.class))).thenReturn(List.of());
        when(flows.count(any(Wrapper.class))).thenReturn(0L);
        when(batches.save(any(ReconciliationBatches.class))).thenReturn(true);

        ReconciliationBatches batch = service.runBatch(2, 0L, 1000L, "test");

        assertEquals(1, batch.getStatus());
        assertEquals(0, batch.getIssueCount());
        verify(issues, never()).saveBatch(any());
    }
}
