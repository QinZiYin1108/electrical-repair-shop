package com.example.backend.controller.worker;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.AccountBalances;
import com.example.backend.entity.FundFlows;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.common.FundFlowItemResponse;
import com.example.backend.model.common.FundSummaryResponse;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.AccountBalancesService;
import com.example.backend.service.FundFlowsService;
import com.example.backend.service.RepairOrderFundService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/worker/funds")
public class WorkerFundsController {

    private static final int ACCOUNT_TYPE_TECHNICIAN = 2;

    private final AccountBalancesService accountBalancesService;
    private final FundFlowsService fundFlowsService;
    private final RepairOrderFundService repairOrderFundService;

    public WorkerFundsController(
        AccountBalancesService accountBalancesService,
        FundFlowsService fundFlowsService,
        RepairOrderFundService repairOrderFundService
    ) {
        this.accountBalancesService = accountBalancesService;
        this.fundFlowsService = fundFlowsService;
        this.repairOrderFundService = repairOrderFundService;
    }

    @GetMapping("/summary")
    public Result<FundSummaryResponse> getSummary() {
        LoginUserInfo user = requireWorker();
        repairOrderFundService.releaseEligibleTechnicianFunds(user.getAccountId(), System.currentTimeMillis());
        AccountBalances balance = getBalance(user.getAccountId());

        FundSummaryResponse response = new FundSummaryResponse();
        response.setBalance(formatMoney(balance == null ? null : balance.getBalance()));
        response.setFrozenBalance(formatMoney(balance == null ? null : balance.getFrozenBalance()));
        response.setTotalIncome(formatMoney(balance == null ? null : balance.getTotalIncome()));
        response.setTotalExpense(formatMoney(balance == null ? null : balance.getTotalExpense()));
        return Result.success(response);
    }

    @GetMapping("/flows")
    public Result<Page<FundFlowItemResponse>> listFlows(
        @RequestParam(value = "pageNo", required = false, defaultValue = "1") Integer pageNo,
        @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize
    ) {
        LoginUserInfo user = requireWorker();
        repairOrderFundService.releaseEligibleTechnicianFunds(user.getAccountId(), System.currentTimeMillis());
        int current = normalizePage(pageNo);
        int size = normalizePageSize(pageSize);

        Page<FundFlows> page = fundFlowsService.page(
            new Page<>(current, size),
            new LambdaQueryWrapper<FundFlows>()
                .eq(FundFlows::getAccountId, user.getAccountId())
                .eq(FundFlows::getAccountType, ACCOUNT_TYPE_TECHNICIAN)
                .eq(FundFlows::getIsDelete, 0)
                .orderByDesc(FundFlows::getCreatedTime)
        );

        List<FundFlowItemResponse> items = page.getRecords().stream()
            .map(this::toFlowItem)
            .collect(Collectors.toList());

        Page<FundFlowItemResponse> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(items);
        return Result.success(result);
    }

    private FundFlowItemResponse toFlowItem(FundFlows flow) {
        FundFlowItemResponse item = new FundFlowItemResponse();
        item.setId(flow.getId());
        item.setFlowType(flow.getFlowType());
        item.setFlowTypeText(getFlowTypeText(flow.getFlowType()));
        item.setAmount(formatMoney(flow.getAmount()));
        item.setBalanceBefore(formatMoney(flow.getBalanceBefore()));
        item.setBalanceAfter(formatMoney(flow.getBalanceAfter()));
        item.setBusinessType(flow.getBusinessType());
        item.setBusinessId(flow.getBusinessId());
        item.setDescription(flow.getDescription());
        item.setCreatedTime(flow.getCreatedTime());
        return item;
    }

    private AccountBalances getBalance(String accountId) {
        if (!StringUtils.hasText(accountId)) {
            return null;
        }
        return accountBalancesService.getOne(
            new LambdaQueryWrapper<AccountBalances>()
                .eq(AccountBalances::getAccountId, accountId)
                .eq(AccountBalances::getAccountType, ACCOUNT_TYPE_TECHNICIAN)
                .eq(AccountBalances::getIsDelete, 0)
                .last("limit 1"),
            false
        );
    }

    private LoginUserInfo requireWorker() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.WORKER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问师傅资金信息");
        }
        return user;
    }

    private String getFlowTypeText(Integer value) {
        int type = value == null ? 0 : value;
        if (type == 1) {
            return "收入";
        }
        if (type == 2) {
            return "支出";
        }
        return "未知";
    }

    private int normalizePage(Integer pageNo) {
        int value = pageNo == null ? 1 : pageNo;
        return Math.max(1, value);
    }

    private int normalizePageSize(Integer pageSize) {
        int value = pageSize == null ? 20 : pageSize;
        return Math.min(50, Math.max(1, value));
    }

    private String formatMoney(BigDecimal value) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value;
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
