package com.example.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.backend.domain.finance.InvoiceStatus;
import com.example.backend.entity.Invoices;
import com.example.backend.entity.RepairOrderPayments;
import com.example.backend.entity.RepairOrders;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.InvoicesMapper;
import com.example.backend.model.finance.InvoiceApplyRequest;
import com.example.backend.service.BusinessMetrics;
import com.example.backend.service.ProductOrdersService;
import com.example.backend.service.RepairOrderPaymentsService;
import com.example.backend.service.RepairOrdersService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class InvoiceServiceImplTests {

    private RepairOrdersService repairOrdersService;
    private RepairOrderPaymentsService paymentsService;
    private ProductOrdersService productOrdersService;
    private InvoicesMapper mapper;
    private SimpleMeterRegistry registry;
    private InvoiceServiceImpl service;

    @BeforeEach
    void setUp() {
        repairOrdersService = mock(RepairOrdersService.class);
        paymentsService = mock(RepairOrderPaymentsService.class);
        productOrdersService = mock(ProductOrdersService.class);
        mapper = mock(InvoicesMapper.class);
        registry = new SimpleMeterRegistry();
        service =
                new InvoiceServiceImpl(
                        repairOrdersService,
                        paymentsService,
                        productOrdersService,
                        new BusinessMetrics(registry));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "taxRate", BigDecimal.ZERO);
        when(mapper.insert(any(Invoices.class))).thenReturn(1);
        when(mapper.updateById(any(Invoices.class))).thenReturn(1);
    }

    private RepairOrders repairOrder(String id, String accountId, String refundAmount) {
        RepairOrders order = new RepairOrders();
        order.setId(id);
        order.setAccountId(accountId);
        order.setRefundAmount(refundAmount == null ? null : new BigDecimal(refundAmount));
        return order;
    }

    private RepairOrderPayments payment(String actualAmount) {
        RepairOrderPayments payment = new RepairOrderPayments();
        payment.setRepairOrderId("RO1");
        payment.setActualAmount(new BigDecimal(actualAmount));
        return payment;
    }

    private InvoiceApplyRequest request(String orderId, String amount) {
        InvoiceApplyRequest request = new InvoiceApplyRequest();
        request.setOrderType(1);
        request.setOrderId(orderId);
        request.setAmount(new BigDecimal(amount));
        request.setTitle("个人");
        return request;
    }

    @Test
    void applyCreatesPendingInvoice() {
        when(repairOrdersService.getById("RO1")).thenReturn(repairOrder("RO1", "U1", "0"));
        when(paymentsService.getOne(any(), anyBoolean())).thenReturn(payment("100.00"));
        when(mapper.selectList(any())).thenReturn(List.of());

        Invoices invoice = service.apply("U1", request("RO1", "80.00"));

        assertEquals(InvoiceStatus.PENDING.getCode(), invoice.getStatus());
        assertEquals(new BigDecimal("80.00"), invoice.getAmount());
        assertTrue(invoice.getInvoiceNo().startsWith("IV"));
        assertEquals(1.0, registry.get("business.invoice.applied").counter().count());
    }

    @Test
    void applyRejectsExceedingAvailable() {
        when(repairOrdersService.getById("RO1")).thenReturn(repairOrder("RO1", "U1", "0"));
        when(paymentsService.getOne(any(), anyBoolean())).thenReturn(payment("100.00"));
        when(mapper.selectList(any())).thenReturn(List.of());

        assertThrows(BusinessException.class, () -> service.apply("U1", request("RO1", "150.00")));
    }

    @Test
    void applyRejectsForeignOrder() {
        when(repairOrdersService.getById("RO1")).thenReturn(repairOrder("RO1", "OTHER", "0"));

        assertThrows(BusinessException.class, () -> service.apply("U1", request("RO1", "10.00")));
    }

    @Test
    void issueRequiresPending() {
        Invoices invoice = new Invoices();
        invoice.setId("IV1");
        invoice.setStatus(InvoiceStatus.ISSUED.getCode());
        when(mapper.selectById("IV1")).thenReturn(invoice);

        assertThrows(BusinessException.class, () -> service.issue("IV1", "AA1", "http://pdf"));
    }

    @Test
    void issueMarksIssued() {
        Invoices invoice = new Invoices();
        invoice.setId("IV1");
        invoice.setStatus(InvoiceStatus.PENDING.getCode());
        invoice.setVersion(0);
        when(mapper.selectById("IV1")).thenReturn(invoice);

        Invoices result = service.issue("IV1", "AA1", "http://pdf");

        assertEquals(InvoiceStatus.ISSUED.getCode(), result.getStatus());
        assertEquals("http://pdf", result.getInvoiceUrl());
        assertEquals(1.0, registry.get("business.invoice.issued").counter().count());
    }

    @Test
    void rejectRequiresReason() {
        Invoices invoice = new Invoices();
        invoice.setId("IV1");
        invoice.setStatus(InvoiceStatus.PENDING.getCode());
        when(mapper.selectById("IV1")).thenReturn(invoice);

        assertThrows(BusinessException.class, () -> service.reject("IV1", "AA1", null));
    }

    @Test
    void redRequiresIssued() {
        Invoices invoice = new Invoices();
        invoice.setId("IV1");
        invoice.setStatus(InvoiceStatus.PENDING.getCode());
        when(mapper.selectById("IV1")).thenReturn(invoice);

        assertThrows(BusinessException.class, () -> service.red("IV1", "AA1", "开票有误"));
    }
}
