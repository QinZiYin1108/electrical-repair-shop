package com.example.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.entity.FinanceOrderSnapshots;
import com.example.backend.entity.ProductOrders;
import com.example.backend.entity.RepairOrderPayments;
import com.example.backend.entity.RepairOrders;
import com.example.backend.mapper.FinanceOrderSnapshotsMapper;
import com.example.backend.service.BusinessMetrics;
import com.example.backend.service.ProductOrdersService;
import com.example.backend.service.RepairOrderPaymentsService;
import com.example.backend.service.RepairOrdersService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class OrderFinanceServiceImplTests {

    private RepairOrdersService repairOrdersService;
    private RepairOrderPaymentsService paymentsService;
    private ProductOrdersService productOrdersService;
    private FinanceOrderSnapshotsMapper mapper;
    private OrderFinanceServiceImpl service;

    @BeforeEach
    void setUp() {
        repairOrdersService = mock(RepairOrdersService.class);
        paymentsService = mock(RepairOrderPaymentsService.class);
        productOrdersService = mock(ProductOrdersService.class);
        mapper = mock(FinanceOrderSnapshotsMapper.class);
        service =
                new OrderFinanceServiceImpl(
                        repairOrdersService,
                        paymentsService,
                        productOrdersService,
                        new BusinessMetrics(new SimpleMeterRegistry()));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "commissionRate", BigDecimal.ZERO);
        ReflectionTestUtils.setField(service, "taxRate", BigDecimal.ZERO);
        when(mapper.insert(any(FinanceOrderSnapshots.class))).thenReturn(1);
        when(mapper.updateById(any(FinanceOrderSnapshots.class))).thenReturn(1);
    }

    private RepairOrders repairOrder() {
        RepairOrders order = new RepairOrders();
        order.setId("RO1");
        order.setOrderNo("NO1");
        order.setAccountId("U1");
        order.setRefundAmount(BigDecimal.ZERO);
        order.setCompletionTime(5000L);
        return order;
    }

    private RepairOrderPayments repairPayment() {
        RepairOrderPayments payment = new RepairOrderPayments();
        payment.setServiceFee(new BigDecimal("100.00"));
        payment.setMaterialFee(new BigDecimal("20.00"));
        payment.setDoorFee(new BigDecimal("30.00"));
        payment.setDistanceFee(new BigDecimal("10.00"));
        payment.setOvertimeFee(BigDecimal.ZERO);
        payment.setActualAmount(new BigDecimal("160.00"));
        payment.setDiscountAmount(BigDecimal.ZERO);
        payment.setPaymentTime(6000L);
        return payment;
    }

    @Test
    void rebuildRepairComputesIncomeBreakdown() {
        when(repairOrdersService.getById("RO1")).thenReturn(repairOrder());
        when(paymentsService.getOne(any(), anyBoolean())).thenReturn(repairPayment());
        when(mapper.selectOne(any(), anyBoolean())).thenReturn(null);

        FinanceOrderSnapshots snapshot = service.rebuild(1, "RO1");

        assertEquals(new BigDecimal("100.00"), snapshot.getIncomeService());
        assertEquals(new BigDecimal("20.00"), snapshot.getIncomeMaterial());
        assertEquals(new BigDecimal("40.00"), snapshot.getIncomeDoorFee());
        assertEquals(new BigDecimal("160.00"), snapshot.getNetAmount());
        assertEquals(new BigDecimal("160.00"), snapshot.getTechnicianIncome());
        assertEquals(BigDecimal.ZERO.setScale(2), snapshot.getPlatformCommission());
        verify(mapper).insert(any(FinanceOrderSnapshots.class));
    }

    @Test
    void rebuildProductComputesIncomeBreakdown() {
        ProductOrders order = new ProductOrders();
        order.setId("PO1");
        order.setOrderNo("PN1");
        order.setAccountId("U1");
        order.setProductAmount(new BigDecimal("200.00"));
        order.setShippingFee(new BigDecimal("15.00"));
        order.setDiscountAmount(new BigDecimal("5.00"));
        order.setActualAmount(new BigDecimal("210.00"));
        order.setRefundAmount(BigDecimal.ZERO);
        order.setPaymentTime(7000L);
        order.setCompletionTime(8000L);
        when(productOrdersService.getById("PO1")).thenReturn(order);
        when(mapper.selectOne(any(), anyBoolean())).thenReturn(null);

        FinanceOrderSnapshots snapshot = service.rebuild(2, "PO1");

        assertEquals(new BigDecimal("200.00"), snapshot.getIncomeProduct());
        assertEquals(new BigDecimal("15.00"), snapshot.getIncomeShipping());
        assertEquals(new BigDecimal("210.00"), snapshot.getNetAmount());
        assertEquals(BigDecimal.ZERO.setScale(2), snapshot.getTechnicianIncome());
        assertEquals("PLATFORM", snapshot.getDiscountBearer());
    }

    @Test
    void rebuildAppliesCommissionAndTax() {
        ReflectionTestUtils.setField(service, "commissionRate", new BigDecimal("0.1"));
        ReflectionTestUtils.setField(service, "taxRate", new BigDecimal("0.06"));
        when(repairOrdersService.getById("RO1")).thenReturn(repairOrder());
        when(paymentsService.getOne(any(), anyBoolean())).thenReturn(repairPayment());
        when(mapper.selectOne(any(), anyBoolean())).thenReturn(null);

        FinanceOrderSnapshots snapshot = service.rebuild(1, "RO1");

        assertEquals(new BigDecimal("16.00"), snapshot.getPlatformCommission());
        assertEquals(new BigDecimal("9.60"), snapshot.getTaxAmount());
        assertEquals(new BigDecimal("144.00"), snapshot.getTechnicianIncome());
    }

    @Test
    void rebuildUpsertsExisting() {
        FinanceOrderSnapshots existing = new FinanceOrderSnapshots();
        existing.setId("FO9");
        existing.setCreatedTime(123L);
        existing.setVersion(0);
        when(repairOrdersService.getById("RO1")).thenReturn(repairOrder());
        when(paymentsService.getOne(any(), anyBoolean())).thenReturn(repairPayment());
        when(mapper.selectOne(any(), anyBoolean())).thenReturn(existing);

        FinanceOrderSnapshots snapshot = service.rebuild(1, "RO1");

        assertEquals("FO9", snapshot.getId());
        verify(mapper).updateById(any(FinanceOrderSnapshots.class));
    }
}
