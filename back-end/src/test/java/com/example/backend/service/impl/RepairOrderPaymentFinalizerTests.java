package com.example.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.backend.entity.OrderProgress;
import com.example.backend.entity.PaymentRecords;
import com.example.backend.entity.RepairOrderPayments;
import com.example.backend.entity.RepairOrders;
import com.example.backend.service.OrderProgressService;
import com.example.backend.service.RepairOrderFundService;
import com.example.backend.service.RepairOrderPaymentsService;
import com.example.backend.service.RepairOrdersService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RepairOrderPaymentFinalizerTests {
    private RepairOrdersService orders;
    private RepairOrderPaymentsService orderPayments;
    private RepairOrderFundService funds;
    private OrderProgressService progress;
    private RepairOrderPaymentFinalizer finalizer;

    @BeforeEach
    void setUp() {
        orders = mock(RepairOrdersService.class);
        orderPayments = mock(RepairOrderPaymentsService.class);
        funds = mock(RepairOrderFundService.class);
        progress = mock(OrderProgressService.class);
        finalizer = new RepairOrderPaymentFinalizer(orders, orderPayments, funds, progress);
    }

    @Test
    @SuppressWarnings("unchecked")
    void prepaySuccessAdvancesOrderAndFreezesTechnician() {
        RepairOrders order = order(1);
        RepairOrderPayments orderPayment =
                orderPayment(new BigDecimal("0.00"), new BigDecimal("30.00"));
        when(orders.getOne(any(Wrapper.class), eq(false))).thenReturn(order);
        when(orders.updateById(any(RepairOrders.class))).thenReturn(true);
        when(orderPayments.getOne(any(Wrapper.class), eq(false))).thenReturn(orderPayment);
        when(orderPayments.updateById(any(RepairOrderPayments.class))).thenReturn(true);
        when(progress.save(any(OrderProgress.class))).thenReturn(true);

        finalizer.onPaymentSuccess(payment(1, new BigDecimal("30.00")), 1000L);

        assertEquals(new BigDecimal("30.00"), orderPayment.getActualAmount());
        assertEquals(1, orderPayment.getPaymentMethod());
        assertEquals(2, order.getPaymentStatus());
        verify(funds)
                .recordOrderPrepay(
                        eq("U1"),
                        eq("TA1"),
                        eq("RO1"),
                        eq("RO1"),
                        eq(1),
                        eq(new BigDecimal("30.00")),
                        anyLong());
        verify(funds, never())
                .recordOrderTailPay(any(), any(), any(), any(), any(), any(), anyLong());
        verify(progress).save(any(OrderProgress.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void tailSuccessPaysOffOrder() {
        RepairOrders order = order(2);
        RepairOrderPayments orderPayment =
                orderPayment(new BigDecimal("30.00"), new BigDecimal("50.00"));
        when(orders.getOne(any(Wrapper.class), eq(false))).thenReturn(order);
        when(orders.updateById(any(RepairOrders.class))).thenReturn(true);
        when(orderPayments.getOne(any(Wrapper.class), eq(false))).thenReturn(orderPayment);
        when(orderPayments.updateById(any(RepairOrderPayments.class))).thenReturn(true);
        when(progress.save(any(OrderProgress.class))).thenReturn(true);

        finalizer.onPaymentSuccess(payment(2, new BigDecimal("20.00")), 1000L);

        assertEquals(new BigDecimal("50.00"), orderPayment.getActualAmount());
        verify(funds)
                .recordOrderTailPay(
                        eq("U1"),
                        eq("TA1"),
                        eq("RO1"),
                        eq("RO1"),
                        eq(1),
                        eq(new BigDecimal("20.00")),
                        anyLong());
        verify(funds, never())
                .recordOrderPrepay(any(), any(), any(), any(), any(), any(), anyLong());
    }

    private PaymentRecords payment(int stage, BigDecimal amount) {
        PaymentRecords payment = new PaymentRecords();
        payment.setPaymentNo("PAY1");
        payment.setOrderId("RO1");
        payment.setOrderType(1);
        payment.setBizStage(stage);
        payment.setAccountId("U1");
        payment.setPaymentMethod(1);
        payment.setPaymentAmount(amount);
        payment.setCurrency("CNY");
        payment.setPaymentStatus(3);
        return payment;
    }

    private RepairOrders order(int paymentStatus) {
        RepairOrders order = new RepairOrders();
        order.setId("RO1");
        order.setOrderNo("RO1");
        order.setAccountId("U1");
        order.setTechnicianAccountId("TA1");
        order.setPaymentStatus(paymentStatus);
        return order;
    }

    private RepairOrderPayments orderPayment(BigDecimal actual, BigDecimal total) {
        RepairOrderPayments orderPayment = new RepairOrderPayments();
        orderPayment.setId("ROP1");
        orderPayment.setRepairOrderId("RO1");
        orderPayment.setActualAmount(actual);
        orderPayment.setTotalAmount(total);
        return orderPayment;
    }
}
