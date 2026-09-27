package com.example.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.entity.UserAccounts;
import com.example.backend.service.AdminAccountsService;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.service.UserAccountsService;
import org.junit.jupiter.api.Test;

class PenaltyAccountEnforcementServiceImplTests {

    private final UserAccountsService userAccountsService = mock(UserAccountsService.class);
    private final TechnicianAccountsService technicianAccountsService =
            mock(TechnicianAccountsService.class);
    private final PenaltyAccountEnforcementServiceImpl service =
            new PenaltyAccountEnforcementServiceImpl(
                    userAccountsService,
                    technicianAccountsService,
                    mock(AdminAccountsService.class));

    @Test
    void freezesUserAndInvalidatesExistingTokens() {
        UserAccounts account = new UserAccounts();
        account.setId("U1");
        account.setStatus(1);
        account.setTokenVersion(4);
        when(userAccountsService.getById("U1")).thenReturn(account);
        when(userAccountsService.updateById(account)).thenReturn(true);

        Integer previousStatus = service.freezeAccount("U1", 1);

        assertEquals(1, previousStatus);
        assertEquals(2, account.getStatus());
        assertEquals(5, account.getTokenVersion());
        verify(userAccountsService).updateById(account);
    }

    @Test
    void doesNotClaimOwnershipOfAnAlreadyFrozenAccount() {
        UserAccounts account = new UserAccounts();
        account.setId("U1");
        account.setStatus(2);
        when(userAccountsService.getById("U1")).thenReturn(account);

        assertNull(service.freezeAccount("U1", 1));
    }

    @Test
    void restoresTechnicianOnlyFromPenaltyFrozenStatus() {
        TechnicianAccounts account = new TechnicianAccounts();
        account.setId("T1");
        account.setAccountStatus(3);
        account.setTokenVersion(2);
        when(technicianAccountsService.getById("T1")).thenReturn(account);
        when(technicianAccountsService.updateById(account)).thenReturn(true);

        service.restoreAccount("T1", 2, 1);

        assertEquals(1, account.getAccountStatus());
        assertEquals(3, account.getTokenVersion());
        verify(technicianAccountsService).updateById(account);
    }
}
