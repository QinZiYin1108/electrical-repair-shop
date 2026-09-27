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
import com.example.backend.entity.TechnicianWithdrawals;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.TechnicianWithdrawalsMapper;
import com.example.backend.model.worker.TechnicianWithdrawalApplyRequest;
import com.example.backend.service.AccountBalancesService;
import com.example.backend.service.FundFlowsService;
import com.example.backend.service.NotificationOutboxService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TechnicianWithdrawalServiceImplTests {
    private AccountBalancesService balances;
    private FundFlowsService flows;
    private TechnicianWithdrawalsMapper mapper;
    private TechnicianWithdrawalServiceImpl service;
    private TechnicianWithdrawals[] persisted = new TechnicianWithdrawals[1];

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        balances = mock(AccountBalancesService.class);
        flows = mock(FundFlowsService.class);
        mapper = mock(TechnicianWithdrawalsMapper.class);
        service =
                new TechnicianWithdrawalServiceImpl(
                        balances, flows, mock(NotificationOutboxService.class));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);

        persisted = new TechnicianWithdrawals[1];
        when(mapper.insert(any(TechnicianWithdrawals.class)))
                .thenAnswer(
                        inv -> {
                            persisted[0] = inv.getArgument(0);
                            return 1;
                        });
        when(mapper.selectOne(any(), anyBoolean())).thenAnswer(inv -> persisted[0]);
        when(mapper.updateById(any(TechnicianWithdrawals.class))).thenReturn(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void applyFreezesAvailableBalance() {
        AccountBalances balance = balance("100.00", "0.00");
        when(balances.getOne(any(Wrapper.class), eq(false))).thenReturn(balance);
        when(balances.updateById(any(AccountBalances.class))).thenReturn(true);
        when(flows.save(any(FundFlows.class))).thenReturn(true);

        TechnicianWithdrawals withdrawal = service.apply("TA1", request("30.00", "K1"));

        assertEquals(new BigDecimal("70.00"), balance.getBalance());
        assertEquals(new BigDecimal("30.00"), balance.getFrozenBalance());
        assertEquals(1, withdrawal.getStatus());
        verify(flows).save(any(FundFlows.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void applyRejectsInsufficientBalance() {
        when(balances.getOne(any(Wrapper.class), eq(false))).thenReturn(balance("10.00", "0.00"));

        assertThrows(BusinessException.class, () -> service.apply("TA1", request("30.00", null)));

        verify(balances, never()).updateById(any());
        verify(mapper, never()).insert(any(TechnicianWithdrawals.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void applyIsIdempotentByKey() {
        TechnicianWithdrawals existing = new TechnicianWithdrawals();
        existing.setId("TW1");
        existing.setIdempotencyKey("K1");
        existing.setStatus(1);
        persisted[0] = existing;

        TechnicianWithdrawals result = service.apply("TA1", request("30.00", "K1"));

        assertEquals("TW1", result.getId());
        verify(balances, never()).updateById(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void markPaidReducesFrozenBalance() {
        persisted[0] = withdrawal(2, "30.00");
        AccountBalances balance = balance("70.00", "30.00");
        when(balances.getOne(any(Wrapper.class), eq(false))).thenReturn(balance);
        when(balances.updateById(any(AccountBalances.class))).thenReturn(true);

        TechnicianWithdrawals result = service.markPaid("TW1", "AA1", "TX-1");

        assertEquals(3, result.getStatus());
        assertEquals(new BigDecimal("0.00"), balance.getFrozenBalance());
        assertEquals(new BigDecimal("70.00"), balance.getBalance());
    }

    @Test
    @SuppressWarnings("unchecked")
    void rejectRefundsFrozenAmount() {
        persisted[0] = withdrawal(1, "30.00");
        AccountBalances balance = balance("70.00", "30.00");
        when(balances.getOne(any(Wrapper.class), eq(false))).thenReturn(balance);
        when(balances.updateById(any(AccountBalances.class))).thenReturn(true);
        when(flows.save(any(FundFlows.class))).thenReturn(true);

        TechnicianWithdrawals result = service.review("TW1", "AA1", false, "信息不全");

        assertEquals(5, result.getStatus());
        assertEquals(new BigDecimal("100.00"), balance.getBalance());
        assertEquals(new BigDecimal("0.00"), balance.getFrozenBalance());
    }

    private TechnicianWithdrawalApplyRequest request(String amount, String idempotencyKey) {
        TechnicianWithdrawalApplyRequest request = new TechnicianWithdrawalApplyRequest();
        request.setAmount(new BigDecimal(amount));
        request.setIdempotencyKey(idempotencyKey);
        return request;
    }

    private TechnicianWithdrawals withdrawal(int status, String amount) {
        TechnicianWithdrawals withdrawal = new TechnicianWithdrawals();
        withdrawal.setId("TW1");
        withdrawal.setTechnicianAccountId("TA1");
        withdrawal.setAmount(new BigDecimal(amount));
        withdrawal.setStatus(status);
        return withdrawal;
    }

    private AccountBalances balance(String available, String frozen) {
        AccountBalances balance = new AccountBalances();
        balance.setId("AB1");
        balance.setAccountId("TA1");
        balance.setAccountType(2);
        balance.setBalance(new BigDecimal(available));
        balance.setFrozenBalance(new BigDecimal(frozen));
        return balance;
    }

    @Test
    @SuppressWarnings("unchecked")
    void applyRejectsOverDailyLimit() {
        when(mapper.selectList(any())).thenReturn(List.of(withdrawal(1, "49000.00")));

        assertThrows(BusinessException.class, () -> service.apply("TA1", request("2000.00", null)));

        verify(mapper, never()).insert(any(TechnicianWithdrawals.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void applyHighAmountRequiresReview() {
        AccountBalances balance = balance("10000.00", "0.00");
        when(balances.getOne(any(Wrapper.class), eq(false))).thenReturn(balance);
        when(balances.updateById(any(AccountBalances.class))).thenReturn(true);
        when(flows.save(any(FundFlows.class))).thenReturn(true);
        when(mapper.selectList(any())).thenReturn(List.of());

        TechnicianWithdrawals withdrawal = service.apply("TA1", request("6000.00", null));

        assertEquals(1, withdrawal.getRequiresReview());
        assertEquals(0, withdrawal.getReviewConfirmed());
        assertEquals(0, withdrawal.getIntercepted());
    }

    @Test
    @SuppressWarnings("unchecked")
    void markPaidRejectedWhenIntercepted() {
        TechnicianWithdrawals withdrawal = withdrawal(2, "30.00");
        withdrawal.setIntercepted(1);
        persisted[0] = withdrawal;

        assertThrows(BusinessException.class, () -> service.markPaid("TW1", "AA1", "TX-1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void markPaidRejectedWhenReviewNotConfirmed() {
        TechnicianWithdrawals withdrawal = withdrawal(2, "30.00");
        withdrawal.setRequiresReview(1);
        withdrawal.setReviewConfirmed(0);
        persisted[0] = withdrawal;

        assertThrows(BusinessException.class, () -> service.markPaid("TW1", "AA1", "TX-1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void confirmReviewSetsConfirmed() {
        TechnicianWithdrawals withdrawal = withdrawal(2, "30.00");
        withdrawal.setRequiresReview(1);
        persisted[0] = withdrawal;

        TechnicianWithdrawals result = service.confirmReview("TW1", "AA1");

        assertEquals(1, result.getReviewConfirmed());
    }

    @Test
    @SuppressWarnings("unchecked")
    void interceptSetsFlag() {
        persisted[0] = withdrawal(2, "30.00");

        TechnicianWithdrawals result = service.intercept("TW1", "AA1", "风险");

        assertEquals(1, result.getIntercepted());
    }
}
