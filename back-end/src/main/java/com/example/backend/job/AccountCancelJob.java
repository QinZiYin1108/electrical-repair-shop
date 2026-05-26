package com.example.backend.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.backend.entity.AccountCancelRecords;
import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.entity.UserAccounts;
import com.example.backend.service.AccountCancelRecordsService;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.service.UserAccountsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 账号注销自动生效任务（用户 + 师傅）
 * 以 account_cancel_records 为主表：
 * - cancel_type=1 表示“申请中”（cancel_time 为申请时写入的到期时间戳）
 * - 到期后执行逻辑注销，并把 cancel_type 更新为 2（系统注销），保留记录不删除
 */
@Component
public class AccountCancelJob {

    private static final int BATCH_SIZE = 200;
    private static final int CANCEL_TYPE_APPLY = 1;
    private static final int CANCEL_TYPE_SYSTEM = 2;

    private final AccountCancelRecordsService accountCancelRecordsService;
    private final UserAccountsService userAccountsService;
    private final TechnicianAccountsService technicianAccountsService;

    public AccountCancelJob(
        AccountCancelRecordsService accountCancelRecordsService,
        UserAccountsService userAccountsService,
        TechnicianAccountsService technicianAccountsService
    ) {
        this.accountCancelRecordsService = accountCancelRecordsService;
        this.userAccountsService = userAccountsService;
        this.technicianAccountsService = technicianAccountsService;
    }

    // Every 10 minutes
    @Scheduled(cron = "0 */10 * * * ?")
    public void run() {
        long now = System.currentTimeMillis();

        // 找一批到期的“注销申请”记录（cancel_time 为到期时间戳）
        List<AccountCancelRecords> records = accountCancelRecordsService.list(
            new LambdaQueryWrapper<AccountCancelRecords>()
                .eq(AccountCancelRecords::getCancelType, CANCEL_TYPE_APPLY)
                .eq(AccountCancelRecords::getIsDelete, 0)
                .le(AccountCancelRecords::getCancelTime, now)
                .orderByAsc(AccountCancelRecords::getCancelTime)
                .last("limit " + BATCH_SIZE)
        );
        if (records == null || records.isEmpty()) {
            return;
        }

        Set<String> recordIdSet = new LinkedHashSet<>();
        Set<String> userIdSet = new LinkedHashSet<>();
        Set<String> technicianIdSet = new LinkedHashSet<>();

        for (AccountCancelRecords record : records) {
            if (record == null) {
                continue;
            }
            if (record.getId() != null && !record.getId().isBlank()) {
                recordIdSet.add(record.getId());
            }
            String accountId = record.getAccountId();
            if (accountId == null || accountId.isBlank()) {
                continue;
            }
            if (accountId.startsWith("U")) {
                userIdSet.add(accountId);
            } else if (accountId.startsWith("TA")) {
                technicianIdSet.add(accountId);
            }
        }

        if (!userIdSet.isEmpty()) {
            userAccountsService.update(
                new LambdaUpdateWrapper<UserAccounts>()
                    .eq(UserAccounts::getIsDelete, 0)
                    .in(UserAccounts::getId, userIdSet)
                    .set(UserAccounts::getStatus, 4)
                    .set(UserAccounts::getCancelTime, now)
                    .set(UserAccounts::getUpdatedTime, now)
                    .set(UserAccounts::getIsDelete, 1)
            );
        }

        if (!technicianIdSet.isEmpty()) {
            technicianAccountsService.update(
                new LambdaUpdateWrapper<TechnicianAccounts>()
                    .eq(TechnicianAccounts::getIsDelete, 0)
                    .in(TechnicianAccounts::getId, technicianIdSet)
                    .set(TechnicianAccounts::getUpdatedTime, now)
                    .set(TechnicianAccounts::getIsDelete, 1)
            );
        }

        // 标记为系统注销（主表不删除记录）
        if (!recordIdSet.isEmpty()) {
            accountCancelRecordsService.update(
                null,
                new UpdateWrapper<AccountCancelRecords>()
                    .set("cancel_type", CANCEL_TYPE_SYSTEM)
                    .set("cancel_time", now)
                    .set("operator_id", "SYSTEM")
                    .in("id", recordIdSet)
                    .eq("cancel_type", CANCEL_TYPE_APPLY)
                    .eq("is_delete", 0)
            );
        }
    }
}

