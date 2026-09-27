package com.example.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.exception.BusinessException;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.ProductsService;
import com.example.backend.service.RepairOrdersService;
import com.example.backend.service.TechnicianAccountsService;
import org.junit.jupiter.api.Test;

class AdminDataScopeServiceImplTests {

    private final AdminDataScopeServiceImpl service =
            new AdminDataScopeServiceImpl(
                    mock(TechnicianAccountsService.class),
                    mock(RepairOrdersService.class),
                    mock(ProductsService.class));

    @Test
    void storeAdminCanOnlyAccessOwnStoreTechnician() {
        LoginUserInfo admin = storeAdmin("S1");
        TechnicianAccounts own = technician("S1");
        TechnicianAccounts other = technician("S2");

        assertDoesNotThrow(() -> service.requireTechnicianAccess(admin, own));
        assertThrows(BusinessException.class, () -> service.requireTechnicianAccess(admin, other));
    }

    @Test
    void superAdminCanAccessAnyStore() {
        LoginUserInfo admin = new LoginUserInfo();
        admin.setRole(AccountRole.ADMIN);
        admin.setAdminRole(1);

        assertDoesNotThrow(() -> service.requireStoreAccess(admin, "S2"));
    }

    private LoginUserInfo storeAdmin(String storeId) {
        LoginUserInfo admin = new LoginUserInfo();
        admin.setRole(AccountRole.ADMIN);
        admin.setAdminRole(2);
        admin.setStoreId(storeId);
        return admin;
    }

    private TechnicianAccounts technician(String storeId) {
        TechnicianAccounts technician = new TechnicianAccounts();
        technician.setStoreId(storeId);
        return technician;
    }
}
