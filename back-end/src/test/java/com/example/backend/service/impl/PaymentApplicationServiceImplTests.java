package com.example.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.backend.entity.PaymentRecords;
import com.example.backend.entity.RechargeOrders;
import com.example.backend.entity.RepairOrderPayments;
import com.example.backend.entity.RepairOrders;
import com.example.backend.entity.UserAccounts;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.payment.CreatePaymentIntentRequest;
import com.example.backend.model.payment.PaymentIntentResponse;
import com.example.backend.payment.PaymentGateway;
import com.example.backend.payment.PaymentPrepayRequest;
import com.example.backend.payment.UnavailableWechatPayGateway;
import com.example.backend.payment.VerifiedPaymentCallback;
import com.example.backend.service.BusinessMetrics;
import com.example.backend.service.PaymentPostingService;
import com.example.backend.service.PaymentRecordsService;
import com.example.backend.service.ProductOrdersService;
import com.example.backend.service.RechargeOrdersService;
import com.example.backend.service.RepairOrderPaymentsService;
import com.example.backend.service.RepairOrdersService;
import com.example.backend.service.UserAccountsService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PaymentApplicationServiceImplTests {

    @Test
    void unavailableChannelDoesNotCreatePaymentOrRechargeOrder() {
        PaymentRecordsService payments = mock(PaymentRecordsService.class);
        RechargeOrdersService recharges = mock(RechargeOrdersService.class);
        UserAccountsService users = mock(UserAccountsService.class);
        PaymentPostingService posting = mock(PaymentPostingService.class);
        PaymentApplicationServiceImpl service =
                new PaymentApplicationServiceImpl(
                        payments,
                        recharges,
                        users,
                        posting,
                        mock(RepairOrdersService.class),
                        mock(RepairOrderPaymentsService.class),
                        mock(ProductOrdersService.class),
                        mock(BusinessMetrics.class),
                        List.of(new UnavailableWechatPayGateway()));

        BusinessException error =
                assertThrows(
                        BusinessException.class,
                        () -> service.createIntent("U1", request(new BigDecimal("100.00"))));

        assertTrue(error.getMessage().contains("尚未配置"));
        verifyNoInteractions(payments, recharges, users, posting);
    }

    @Test
    void availableGatewayCreatesProcessingIntentWithoutPostingFunds() {
        PaymentRecordsService payments = mock(PaymentRecordsService.class);
        RechargeOrdersService recharges = mock(RechargeOrdersService.class);
        UserAccountsService users = mock(UserAccountsService.class);
        PaymentPostingService posting = mock(PaymentPostingService.class);
        UserAccounts user = new UserAccounts();
        user.setId("U1");
        user.setWxOpenid("openid-1");
        when(users.getById("U1")).thenReturn(user);
        AtomicInteger statusAtInsert = new AtomicInteger();
        when(payments.save(any(PaymentRecords.class)))
                .thenAnswer(
                        invocation -> {
                            statusAtInsert.set(
                                    invocation
                                            .getArgument(0, PaymentRecords.class)
                                            .getPaymentStatus());
                            return true;
                        });
        when(payments.updateById(any(PaymentRecords.class))).thenReturn(true);
        when(recharges.save(any(RechargeOrders.class))).thenReturn(true);
        when(recharges.updateById(any(RechargeOrders.class))).thenReturn(true);
        PaymentApplicationServiceImpl service =
                new PaymentApplicationServiceImpl(
                        payments,
                        recharges,
                        users,
                        posting,
                        mock(RepairOrdersService.class),
                        mock(RepairOrderPaymentsService.class),
                        mock(ProductOrdersService.class),
                        mock(BusinessMetrics.class),
                        List.of(availableGateway()));

        PaymentIntentResponse result = service.createIntent("U1", request(new BigDecimal("88.8")));

        assertEquals(2, result.getStatus());
        assertEquals("88.80", result.getAmount());
        assertEquals("CNY", result.getCurrency());
        assertEquals("prepay-1", result.getInvokeParameters().get("prepayId"));

        ArgumentCaptor<PaymentRecords> paymentCaptor =
                ArgumentCaptor.forClass(PaymentRecords.class);
        org.mockito.Mockito.verify(payments).save(paymentCaptor.capture());
        PaymentRecords payment = paymentCaptor.getValue();
        assertEquals(1, statusAtInsert.get());
        assertEquals(3, payment.getOrderType());
        assertTrue(payment.getThirdPartyNo() == null);

        ArgumentCaptor<RechargeOrders> rechargeCaptor =
                ArgumentCaptor.forClass(RechargeOrders.class);
        org.mockito.Mockito.verify(recharges).save(rechargeCaptor.capture());
        assertEquals(payment.getOrderId(), rechargeCaptor.getValue().getId());
    }

    @Test
    void accountWithoutWechatBindingDoesNotCreatePaymentIntent() {
        PaymentRecordsService payments = mock(PaymentRecordsService.class);
        RechargeOrdersService recharges = mock(RechargeOrdersService.class);
        UserAccountsService users = mock(UserAccountsService.class);
        PaymentPostingService posting = mock(PaymentPostingService.class);
        UserAccounts user = new UserAccounts();
        user.setId("U1");
        when(users.getById("U1")).thenReturn(user);
        PaymentApplicationServiceImpl service =
                new PaymentApplicationServiceImpl(
                        payments,
                        recharges,
                        users,
                        posting,
                        mock(RepairOrdersService.class),
                        mock(RepairOrderPaymentsService.class),
                        mock(ProductOrdersService.class),
                        mock(BusinessMetrics.class),
                        List.of(availableGateway()));

        BusinessException error =
                assertThrows(
                        BusinessException.class,
                        () -> service.createIntent("U1", request(new BigDecimal("20.00"))));

        assertTrue(error.getMessage().contains("未绑定微信"));
        verifyNoInteractions(payments, recharges);
    }

    @Test
    void repairPrepayIntentDerivesAmountServerSide() {
        PaymentRecordsService payments = mock(PaymentRecordsService.class);
        RechargeOrdersService recharges = mock(RechargeOrdersService.class);
        UserAccountsService users = mock(UserAccountsService.class);
        PaymentPostingService posting = mock(PaymentPostingService.class);
        RepairOrdersService repairOrders = mock(RepairOrdersService.class);
        RepairOrderPaymentsService repairOrderPayments = mock(RepairOrderPaymentsService.class);
        UserAccounts user = new UserAccounts();
        user.setId("U1");
        user.setWxOpenid("openid-1");
        when(users.getById("U1")).thenReturn(user);
        RepairOrders order = new RepairOrders();
        order.setId("RO1");
        order.setAccountId("U1");
        order.setPaymentStatus(1);
        when(repairOrders.getOne(any(), eq(false))).thenReturn(order);
        RepairOrderPayments orderPayment = new RepairOrderPayments();
        orderPayment.setRepairOrderId("RO1");
        orderPayment.setTotalAmount(new BigDecimal("30.00"));
        orderPayment.setActualAmount(BigDecimal.ZERO);
        when(repairOrderPayments.getOne(any(), eq(false))).thenReturn(orderPayment);
        when(payments.save(any(PaymentRecords.class))).thenReturn(true);
        when(payments.updateById(any(PaymentRecords.class))).thenReturn(true);
        PaymentApplicationServiceImpl service =
                new PaymentApplicationServiceImpl(
                        payments,
                        recharges,
                        users,
                        posting,
                        repairOrders,
                        repairOrderPayments,
                        mock(ProductOrdersService.class),
                        mock(BusinessMetrics.class),
                        List.of(availableGateway()));

        CreatePaymentIntentRequest request = new CreatePaymentIntentRequest();
        request.setOrderType(1);
        request.setOrderId("RO1");
        request.setProvider(1);
        PaymentIntentResponse result = service.createIntent("U1", request);

        assertEquals("30.00", result.getAmount());
        ArgumentCaptor<PaymentRecords> captor = ArgumentCaptor.forClass(PaymentRecords.class);
        org.mockito.Mockito.verify(payments).save(captor.capture());
        assertEquals(1, captor.getValue().getOrderType());
        assertEquals(1, captor.getValue().getBizStage());
        verifyNoInteractions(recharges);
    }

    private CreatePaymentIntentRequest request(BigDecimal amount) {
        CreatePaymentIntentRequest request = new CreatePaymentIntentRequest();
        request.setOrderType(3);
        request.setProvider(1);
        request.setAmount(amount);
        return request;
    }

    private PaymentGateway availableGateway() {
        return new PaymentGateway() {
            @Override
            public int provider() {
                return 1;
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public Map<String, String> prepay(PaymentPrepayRequest request) {
                assertEquals("openid-1", request.payerOpenId());
                return Map.of("prepayId", "prepay-1");
            }

            @Override
            public VerifiedPaymentCallback verifyCallback(
                    Map<String, String> headers, String body) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
