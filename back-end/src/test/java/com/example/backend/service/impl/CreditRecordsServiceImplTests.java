package com.example.backend.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.backend.entity.CreditRecords;
import com.example.backend.entity.UserAccounts;
import com.example.backend.service.AdminAccountsService;
import com.example.backend.service.SystemConfigsService;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.service.UserAccountsService;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class CreditRecordsServiceImplTests {

    private final UserAccountsService userAccountsService = mock(UserAccountsService.class);
    private final TechnicianAccountsService technicianAccountsService =
            mock(TechnicianAccountsService.class);
    private final SystemConfigsService systemConfigsService = mock(SystemConfigsService.class);
    private final CreditRecordsServiceImpl service =
            spy(
                    new CreditRecordsServiceImpl(
                            userAccountsService,
                            technicianAccountsService,
                            mock(AdminAccountsService.class),
                            systemConfigsService));

    @Test
    void recoversEachUserOnceAndCapsAtInitialScore() {
        CreditRecords latest = violation("CR2", "U1", 1, 40);
        CreditRecords older = violation("CR1", "U1", 1, 80);
        UserAccounts account = new UserAccounts();
        account.setId("U1");
        account.setCreditScore(95);

        doReturn(100).when(systemConfigsService).getIntegerConfig("credit.score_initial", 100);
        doReturn(account).when(userAccountsService).getById("U1");
        doReturn(List.of(latest, older), List.of()).when(service).list(any(Wrapper.class));
        doReturn(false).when(service).hasRecoveryForViolation("U1", 1, "CR2");
        doReturn(100)
                .when(service)
                .changeScore(eq("U1"), eq(1), eq(5), eq(2), any(String.class), eq("CR2"));

        service.recoverNoViolationScore(30, 10);

        verify(service).changeScore("U1", 1, 5, 2, "30天无新增违规，自动恢复5分", "CR2");
    }

    @Test
    void doesNotRepeatRecoveryForTheSameViolationCycle() {
        CreditRecords violation = violation("CR1", "U1", 1, 40);
        doReturn(100).when(systemConfigsService).getIntegerConfig("credit.score_initial", 100);
        doReturn(List.of(violation), List.of()).when(service).list(any(Wrapper.class));
        doReturn(true).when(service).hasRecoveryForViolation("U1", 1, "CR1");

        service.recoverNoViolationScore(30, 10);

        verify(service).hasRecoveryForViolation("U1", 1, "CR1");
        verifyNoMoreInteractions(userAccountsService);
    }

    private CreditRecords violation(String id, String accountId, int accountType, int daysAgo) {
        CreditRecords record = new CreditRecords();
        record.setId(id);
        record.setAccountId(accountId);
        record.setAccountType(accountType);
        record.setChangeType(1);
        record.setCreatedTime(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysAgo));
        return record;
    }
}
