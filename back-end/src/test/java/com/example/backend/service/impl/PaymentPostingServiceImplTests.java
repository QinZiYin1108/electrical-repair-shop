package com.example.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.backend.entity.AccountBalances;
import com.example.backend.entity.FundFlows;
import com.example.backend.entity.PaymentRecords;
import com.example.backend.entity.RechargeOrders;
import com.example.backend.exception.BusinessException;
import com.example.backend.payment.OrderPaymentFinalizer;
import com.example.backend.payment.VerifiedPaymentCallback;
import com.example.backend.service.AccountBalancesService;
import com.example.backend.service.BusinessMetrics;
import com.example.backend.service.FundFlowsService;
import com.example.backend.service.PaymentRecordsService;
import com.example.backend.service.RechargeOrdersService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PaymentPostingServiceImplTests {
    private PaymentRecordsService payments;
    private RechargeOrdersService recharges;
    private AccountBalancesService balances;
    private FundFlowsService flows;
    private PaymentPostingServiceImpl service;

    @BeforeEach
    void setUp() {
        payments = org.mockito.Mockito.mock(PaymentRecordsService.class);
        recharges = org.mockito.Mockito.mock(RechargeOrdersService.class);
        balances = org.mockito.Mockito.mock(AccountBalancesService.class);
        flows = org.mockito.Mockito.mock(FundFlowsService.class);
        service =
                new PaymentPostingServiceImpl(
                        payments,
                        recharges,
                        balances,
                        flows,
                        org.mockito.Mockito.mock(BusinessMetrics.class),
                        List.of());
    }

    @Test
    @SuppressWarnings("unchecked")
    void verifiedRechargePostsBalanceAndFundFlowOnce() {
        PaymentRecords payment = payment(2);
        RechargeOrders recharge = recharge();
        AccountBalances balance = balance();
        when(payments.getOne(any(Wrapper.class), eq(false))).thenReturn(payment);
        when(recharges.getOne(any(Wrapper.class), eq(false))).thenReturn(recharge);
        when(balances.getOne(any(Wrapper.class), eq(false))).thenReturn(balance);
        when(balances.updateById(any(AccountBalances.class))).thenReturn(true);
        when(flows.save(any(FundFlows.class))).thenReturn(true);
        when(payments.updateById(any(PaymentRecords.class))).thenReturn(true);
        when(recharges.updateById(any(RechargeOrders.class))).thenReturn(true);

        service.postVerifiedPayment(1, callback(new BigDecimal("20.00"), "WX-TX-1"));

        assertEquals(new BigDecimal("30.00"), balance.getBalance());
        assertEquals(3, payment.getPaymentStatus());
        assertEquals("WX-TX-1", payment.getThirdPartyNo());
        ArgumentCaptor<FundFlows> flow = ArgumentCaptor.forClass(FundFlows.class);
        verify(flows).save(flow.capture());
        assertEquals("PAYMENT_POSTING:PAY1", flow.getValue().getIdempotencyKey());
        assertEquals(new BigDecimal("10.00"), flow.getValue().getBalanceBefore());
        assertEquals(new BigDecimal("30.00"), flow.getValue().getBalanceAfter());
    }

    @Test
    @SuppressWarnings("unchecked")
    void amountMismatchDoesNotTouchBalance() {
        when(payments.getOne(any(Wrapper.class), eq(false))).thenReturn(payment(2));

        assertThrows(
                BusinessException.class,
                () -> service.postVerifiedPayment(1, callback(new BigDecimal("19.99"), "WX-TX-2")));

        verify(balances, never()).updateById(any());
        verify(flows, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void duplicateSuccessfulCallbackIsIdempotent() {
        PaymentRecords payment = payment(3);
        payment.setThirdPartyNo("WX-TX-1");
        when(payments.getOne(any(Wrapper.class), eq(false))).thenReturn(payment);

        service.postVerifiedPayment(1, callback(new BigDecimal("20.00"), "WX-TX-1"));

        verify(recharges, never()).getOne(any(Wrapper.class), eq(false));
        verify(balances, never()).updateById(any());
        verify(flows, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void orderPaymentDispatchesToOrderFinalizer() {
        PaymentRecords payment = new PaymentRecords();
        payment.setPaymentNo("PAY1");
        payment.setOrderId("RO1");
        payment.setOrderType(1);
        payment.setAccountId("U1");
        payment.setPaymentMethod(1);
        payment.setPaymentAmount(new BigDecimal("20.00"));
        payment.setCurrency("CNY");
        payment.setPaymentStatus(2);
        when(payments.getOne(any(Wrapper.class), eq(false))).thenReturn(payment);
        when(payments.updateById(any(PaymentRecords.class))).thenReturn(true);
        OrderPaymentFinalizer finalizer = org.mockito.Mockito.mock(OrderPaymentFinalizer.class);
        when(finalizer.orderType()).thenReturn(1);
        PaymentPostingServiceImpl svc =
                new PaymentPostingServiceImpl(
                        payments,
                        recharges,
                        balances,
                        flows,
                        org.mockito.Mockito.mock(BusinessMetrics.class),
                        List.of(finalizer));

        svc.postVerifiedPayment(1, callback(new BigDecimal("20.00"), "WX-TX-9"));

        assertEquals(3, payment.getPaymentStatus());
        assertEquals("WX-TX-9", payment.getThirdPartyNo());
        verify(finalizer).onPaymentSuccess(eq(payment), anyLong());
        verify(recharges, never()).getOne(any(Wrapper.class), eq(false));
        verify(balances, never()).updateById(any());
    }

    private PaymentRecords payment(int status) {
        PaymentRecords payment = new PaymentRecords();
        payment.setPaymentNo("PAY1");
        payment.setOrderId("RC1");
        payment.setOrderType(3);
        payment.setAccountId("U1");
        payment.setPaymentMethod(1);
        payment.setPaymentAmount(new BigDecimal("20.00"));
        payment.setCurrency("CNY");
        payment.setPaymentStatus(status);
        return payment;
    }

    private RechargeOrders recharge() {
        RechargeOrders recharge = new RechargeOrders();
        recharge.setId("RC1");
        recharge.setAccountId("U1");
        recharge.setAmount(new BigDecimal("20.00"));
        return recharge;
    }

    private AccountBalances balance() {
        AccountBalances balance = new AccountBalances();
        balance.setId("AB1");
        balance.setBalance(new BigDecimal("10.00"));
        balance.setTotalIncome(BigDecimal.ZERO.setScale(2));
        return balance;
    }

    private VerifiedPaymentCallback callback(BigDecimal amount, String transactionNo) {
        return new VerifiedPaymentCallback("PAY1", transactionNo, amount, "CNY", true, "sha256:x");
    }
}
