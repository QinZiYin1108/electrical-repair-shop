package com.example.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.backend.entity.AccountBalances;
import com.example.backend.entity.FundFlows;
import com.example.backend.entity.PaymentRecords;
import com.example.backend.entity.PaymentRefunds;
import com.example.backend.entity.RechargeOrders;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.PaymentRefundsMapper;
import com.example.backend.model.payment.RefundApplyRequest;
import com.example.backend.payment.ChannelRefundStatus;
import com.example.backend.payment.PaymentGateway;
import com.example.backend.payment.RefundResult;
import com.example.backend.payment.VerifiedRefundCallback;
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
import org.springframework.test.util.ReflectionTestUtils;

class PaymentRefundsServiceImplTests {
    private PaymentRecordsService payments;
    private RechargeOrdersService recharges;
    private AccountBalancesService balances;
    private FundFlowsService flows;
    private PaymentRefundsMapper refundsMapper;
    private PaymentRefundsServiceImpl service;

    /** 记录被插入的退款单，模拟数据库在后续查询中的可见性。 */
    private final PaymentRefunds[] persisted = new PaymentRefunds[1];

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        payments = mock(PaymentRecordsService.class);
        recharges = mock(RechargeOrdersService.class);
        balances = mock(AccountBalancesService.class);
        flows = mock(FundFlowsService.class);
        refundsMapper = mock(PaymentRefundsMapper.class);
        PaymentGateway gateway = mock(PaymentGateway.class);
        when(gateway.provider()).thenReturn(1);
        when(gateway.isAvailable()).thenReturn(true);
        when(gateway.refund(any()))
                .thenAnswer(
                        inv -> {
                            com.example.backend.payment.RefundRequest r = inv.getArgument(0);
                            return new RefundResult(
                                    r.refundNo(),
                                    "WX-RF-1",
                                    r.refundAmount(),
                                    r.currency(),
                                    ChannelRefundStatus.SUCCESS,
                                    "wx-refund:SUCCESS");
                        });

        persisted[0] = null;
        service =
                new PaymentRefundsServiceImpl(
                        payments,
                        recharges,
                        balances,
                        flows,
                        mock(BusinessMetrics.class),
                        List.of(gateway));
        ReflectionTestUtils.setField(service, "baseMapper", refundsMapper);

        when(refundsMapper.insert(any(PaymentRefunds.class)))
                .thenAnswer(
                        inv -> {
                            persisted[0] = inv.getArgument(0);
                            return 1;
                        });
        when(refundsMapper.selectList(any()))
                .thenAnswer(inv -> persisted[0] == null ? List.of() : List.of(persisted[0]));
        when(refundsMapper.selectOne(any(), anyBoolean())).thenAnswer(inv -> persisted[0]);
        when(refundsMapper.selectById(any())).thenAnswer(inv -> persisted[0]);
        when(refundsMapper.updateById(any(PaymentRefunds.class))).thenReturn(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void fullRefundDeductsBalanceAndRecordsFundFlow() {
        PaymentRecords payment = payment(new BigDecimal("20.00"), BigDecimal.ZERO);
        RechargeOrders recharge = recharge(new BigDecimal("20.00"));
        AccountBalances balance = balance(new BigDecimal("30.00"));
        when(payments.getOne(any(Wrapper.class), eq(false))).thenReturn(payment);
        when(payments.updateById(any(PaymentRecords.class))).thenReturn(true);
        when(recharges.getOne(any(Wrapper.class), eq(false))).thenReturn(recharge);
        when(recharges.updateById(any(RechargeOrders.class))).thenReturn(true);
        when(balances.getOne(any(Wrapper.class), eq(false))).thenReturn(balance);
        when(balances.updateById(any(AccountBalances.class))).thenReturn(true);
        when(flows.save(any(FundFlows.class))).thenReturn(true);

        var response = service.applyRefund("PAY1", request(new BigDecimal("20.00")), "AA1");

        assertEquals(2, response.getStatus());
        assertEquals(new BigDecimal("10.00"), balance.getBalance());
        assertEquals(new BigDecimal("20.00"), balance.getTotalExpense());
        assertEquals(new BigDecimal("20.00"), payment.getRefundAmount());
        assertEquals(5, payment.getPaymentStatus());
        assertEquals(6, recharge.getStatus());
        ArgumentCaptor<FundFlows> flow = ArgumentCaptor.forClass(FundFlows.class);
        verify(flows).save(flow.capture());
        assertEquals(
                "PAYMENT_REFUND:" + response.getRefundNo(), flow.getValue().getIdempotencyKey());
        assertEquals(2, flow.getValue().getFlowType());
        assertEquals(new BigDecimal("30.00"), flow.getValue().getBalanceBefore());
        assertEquals(new BigDecimal("10.00"), flow.getValue().getBalanceAfter());
    }

    @Test
    @SuppressWarnings("unchecked")
    void unavailableGatewayRejectsRefundWithoutPersisting() {
        PaymentGateway unavailable = mock(PaymentGateway.class);
        when(unavailable.provider()).thenReturn(1);
        when(unavailable.isAvailable()).thenReturn(false);
        service =
                new PaymentRefundsServiceImpl(
                        payments,
                        recharges,
                        balances,
                        flows,
                        mock(BusinessMetrics.class),
                        List.of(unavailable));
        ReflectionTestUtils.setField(service, "baseMapper", refundsMapper);

        when(payments.getOne(any(Wrapper.class), eq(false)))
                .thenReturn(payment(new BigDecimal("20.00"), BigDecimal.ZERO));

        assertThrows(
                BusinessException.class,
                () -> service.applyRefund("PAY1", request(new BigDecimal("20.00")), "AA1"));

        verify(refundsMapper, never()).insert(any(PaymentRefunds.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void refundExceedingPaidAmountIsRejected() {
        when(payments.getOne(any(Wrapper.class), eq(false)))
                .thenReturn(payment(new BigDecimal("20.00"), new BigDecimal("15.00")));

        assertThrows(
                BusinessException.class,
                () -> service.applyRefund("PAY1", request(new BigDecimal("10.00")), "AA1"));

        verify(refundsMapper, never()).insert(any(PaymentRefunds.class));
        verify(balances, never()).updateById(any());
    }

    @Test
    void duplicateSuccessCallbackIsIdempotent() {
        PaymentRefunds refund = new PaymentRefunds();
        refund.setRefundNo("REF1");
        refund.setProvider(1);
        refund.setRefundStatus(2);
        refund.setRefundAmount(new BigDecimal("20.00"));
        refund.setCurrency("CNY");
        refund.setProviderRefundNo("WX-RF-1");
        persisted[0] = refund;

        service.postVerifiedRefund(
                1,
                new VerifiedRefundCallback(
                        "REF1",
                        "WX-RF-1",
                        new BigDecimal("20.00"),
                        "CNY",
                        ChannelRefundStatus.SUCCESS,
                        "wx-refund:SUCCESS"));

        verify(balances, never()).updateById(any());
        verify(flows, never()).save(any());
        verify(payments, never()).updateById(any());
    }

    @Test
    void refundCallbackAmountMismatchDoesNotPost() {
        PaymentRefunds refund = new PaymentRefunds();
        refund.setRefundNo("REF1");
        refund.setProvider(1);
        refund.setRefundStatus(1);
        refund.setRefundAmount(new BigDecimal("20.00"));
        refund.setCurrency("CNY");
        persisted[0] = refund;

        assertThrows(
                BusinessException.class,
                () ->
                        service.postVerifiedRefund(
                                1,
                                new VerifiedRefundCallback(
                                        "REF1",
                                        "WX-RF-1",
                                        new BigDecimal("19.99"),
                                        "CNY",
                                        ChannelRefundStatus.SUCCESS,
                                        "wx-refund:SUCCESS")));

        verify(balances, never()).updateById(any());
        verify(flows, never()).save(any());
    }

    private RefundApplyRequest request(BigDecimal amount) {
        RefundApplyRequest request = new RefundApplyRequest();
        request.setAmount(amount);
        request.setReason("测试退款");
        return request;
    }

    @Test
    void orderChannelRefundDoesNotTouchUserBalance() {
        PaymentRefunds refund = new PaymentRefunds();
        refund.setRefundNo("REF1");
        refund.setProvider(1);
        refund.setRefundStatus(1);
        refund.setRefundAmount(new BigDecimal("20.00"));
        refund.setCurrency("CNY");
        refund.setPaymentId("PR1");
        refund.setRefundReason("售后退款");
        persisted[0] = refund;

        PaymentRecords payment = new PaymentRecords();
        payment.setId("PR1");
        payment.setOrderId("RO1");
        payment.setOrderType(1);
        payment.setAccountId("U1");
        payment.setPaymentMethod(1);
        payment.setPaymentStatus(3);
        payment.setPaymentAmount(new BigDecimal("20.00"));
        payment.setCurrency("CNY");
        payment.setRefundAmount(BigDecimal.ZERO);
        when(payments.getOne(any(Wrapper.class), eq(false))).thenReturn(payment);
        when(payments.updateById(any(PaymentRecords.class))).thenReturn(true);

        service.postVerifiedRefund(
                1,
                new VerifiedRefundCallback(
                        "REF1",
                        "WX-RF-1",
                        new BigDecimal("20.00"),
                        "CNY",
                        ChannelRefundStatus.SUCCESS,
                        "wx-refund:SUCCESS"));

        verify(balances, never()).updateById(any());
        assertEquals(new BigDecimal("20.00"), payment.getRefundAmount());
        assertEquals(5, payment.getPaymentStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    void refundOrderChannelPaymentsReturnsZeroWhenNone() {
        when(payments.list(any(Wrapper.class))).thenReturn(List.of());

        int count = service.refundOrderChannelPayments("RO1", "售后退款", "AA1");

        assertEquals(0, count);
    }

    private PaymentRecords payment(BigDecimal amount, BigDecimal refunded) {
        PaymentRecords payment = new PaymentRecords();
        payment.setId("PR1");
        payment.setPaymentNo("PAY1");
        payment.setOrderId("RC1");
        payment.setOrderType(3);
        payment.setAccountId("U1");
        payment.setPaymentMethod(1);
        payment.setPaymentAmount(amount);
        payment.setCurrency("CNY");
        payment.setPaymentStatus(3);
        payment.setThirdPartyNo("WX-TX-1");
        payment.setRefundAmount(refunded);
        return payment;
    }

    private RechargeOrders recharge(BigDecimal amount) {
        RechargeOrders recharge = new RechargeOrders();
        recharge.setId("RC1");
        recharge.setAccountId("U1");
        recharge.setAmount(amount);
        recharge.setStatus(3);
        return recharge;
    }

    private AccountBalances balance(BigDecimal balance) {
        AccountBalances entity = new AccountBalances();
        entity.setId("AB1");
        entity.setAccountId("U1");
        entity.setAccountType(1);
        entity.setBalance(balance);
        entity.setTotalExpense(BigDecimal.ZERO.setScale(2));
        entity.setTotalIncome(BigDecimal.ZERO.setScale(2));
        entity.setFrozenBalance(BigDecimal.ZERO.setScale(2));
        return entity;
    }
}
