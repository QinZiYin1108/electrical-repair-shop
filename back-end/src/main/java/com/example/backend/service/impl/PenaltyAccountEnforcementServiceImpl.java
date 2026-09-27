package com.example.backend.service.impl;

import com.example.backend.common.ErrorCode;
import com.example.backend.entity.AdminAccounts;
import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.entity.UserAccounts;
import com.example.backend.exception.BusinessException;
import com.example.backend.security.token.TokenVersions;
import com.example.backend.service.AdminAccountsService;
import com.example.backend.service.PenaltyAccountEnforcementService;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.service.UserAccountsService;
import org.springframework.stereotype.Service;

@Service
public class PenaltyAccountEnforcementServiceImpl implements PenaltyAccountEnforcementService {

    private static final int USER_NORMAL = 1;
    private static final int USER_FROZEN = 2;
    private static final int TECHNICIAN_FROZEN = 3;
    private static final int ADMIN_FROZEN = 2;

    private final UserAccountsService userAccountsService;
    private final TechnicianAccountsService technicianAccountsService;
    private final AdminAccountsService adminAccountsService;

    public PenaltyAccountEnforcementServiceImpl(
            UserAccountsService userAccountsService,
            TechnicianAccountsService technicianAccountsService,
            AdminAccountsService adminAccountsService) {
        this.userAccountsService = userAccountsService;
        this.technicianAccountsService = technicianAccountsService;
        this.adminAccountsService = adminAccountsService;
    }

    @Override
    public Integer freezeAccount(String accountId, int accountType) {
        return switch (accountType) {
            case 1 -> freezeUser(accountId);
            case 2 -> freezeTechnician(accountId);
            case 3 -> freezeAdmin(accountId);
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的账号类型");
        };
    }

    @Override
    public void restoreAccount(String accountId, int accountType, Integer previousStatus) {
        if (previousStatus == null) {
            return;
        }
        switch (accountType) {
            case 1 -> restoreUser(accountId, previousStatus);
            case 2 -> restoreTechnician(accountId, previousStatus);
            case 3 -> restoreAdmin(accountId, previousStatus);
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的账号类型");
        }
    }

    private Integer freezeUser(String accountId) {
        UserAccounts account = requireUser(accountId);
        if (Integer.valueOf(USER_FROZEN).equals(account.getStatus())) {
            return null;
        }
        int previousStatus = account.getStatus() == null ? USER_NORMAL : account.getStatus();
        account.setStatus(USER_FROZEN);
        account.setTokenVersion(TokenVersions.next(account.getTokenVersion()));
        requireUpdated(userAccountsService.updateById(account));
        return previousStatus;
    }

    private Integer freezeTechnician(String accountId) {
        TechnicianAccounts account = requireTechnician(accountId);
        if (Integer.valueOf(TECHNICIAN_FROZEN).equals(account.getAccountStatus())) {
            return null;
        }
        int previousStatus =
                account.getAccountStatus() == null ? USER_NORMAL : account.getAccountStatus();
        account.setAccountStatus(TECHNICIAN_FROZEN);
        account.setTokenVersion(TokenVersions.next(account.getTokenVersion()));
        requireUpdated(technicianAccountsService.updateById(account));
        return previousStatus;
    }

    private Integer freezeAdmin(String accountId) {
        AdminAccounts account = requireAdmin(accountId);
        if (Integer.valueOf(ADMIN_FROZEN).equals(account.getAccountStatus())) {
            return null;
        }
        int previousStatus =
                account.getAccountStatus() == null ? USER_NORMAL : account.getAccountStatus();
        account.setAccountStatus(ADMIN_FROZEN);
        account.setTokenVersion(TokenVersions.next(account.getTokenVersion()));
        requireUpdated(adminAccountsService.updateById(account));
        return previousStatus;
    }

    private void restoreUser(String accountId, int previousStatus) {
        UserAccounts account = requireUser(accountId);
        if (!Integer.valueOf(USER_FROZEN).equals(account.getStatus())) {
            return;
        }
        account.setStatus(previousStatus);
        account.setTokenVersion(TokenVersions.next(account.getTokenVersion()));
        requireUpdated(userAccountsService.updateById(account));
    }

    private void restoreTechnician(String accountId, int previousStatus) {
        TechnicianAccounts account = requireTechnician(accountId);
        if (!Integer.valueOf(TECHNICIAN_FROZEN).equals(account.getAccountStatus())) {
            return;
        }
        account.setAccountStatus(previousStatus);
        account.setTokenVersion(TokenVersions.next(account.getTokenVersion()));
        requireUpdated(technicianAccountsService.updateById(account));
    }

    private void restoreAdmin(String accountId, int previousStatus) {
        AdminAccounts account = requireAdmin(accountId);
        if (!Integer.valueOf(ADMIN_FROZEN).equals(account.getAccountStatus())) {
            return;
        }
        account.setAccountStatus(previousStatus);
        account.setTokenVersion(TokenVersions.next(account.getTokenVersion()));
        requireUpdated(adminAccountsService.updateById(account));
    }

    private UserAccounts requireUser(String accountId) {
        UserAccounts account = userAccountsService.getById(accountId);
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户账号不存在");
        }
        return account;
    }

    private TechnicianAccounts requireTechnician(String accountId) {
        TechnicianAccounts account = technicianAccountsService.getById(accountId);
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "维修人员账号不存在");
        }
        return account;
    }

    private AdminAccounts requireAdmin(String accountId) {
        AdminAccounts account = adminAccountsService.getById(accountId);
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "管理员账号不存在");
        }
        return account;
    }

    private void requireUpdated(boolean updated) {
        if (!updated) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "账号状态已变化，请重试");
        }
    }
}
