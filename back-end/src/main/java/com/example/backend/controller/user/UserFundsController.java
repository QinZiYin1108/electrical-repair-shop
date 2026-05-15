package com.example.backend.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.AccountBalances;
import com.example.backend.entity.FundFlows;
import com.example.backend.entity.PaymentRecords;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.common.FundFlowItemResponse;
import com.example.backend.model.common.FundSummaryResponse;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.AccountBalancesService;
import com.example.backend.service.FundFlowsService;
import com.example.backend.service.PaymentRecordsService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import lombok.Data;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user/funds")
public class UserFundsController {

    private static final int ACCOUNT_TYPE_USER = 1;
    private static final int FLOW_TYPE_INCOME = 1;
    private static final int PAYMENT_METHOD_WECHAT = 1;
    private static final int PAYMENT_METHOD_ALIPAY = 2;
    private static final int PAYMENT_RECORD_STATUS_SUCCESS = 3;
    private static final int PAYMENT_ORDER_TYPE_RECHARGE = 3;
    private static final String BUSINESS_TYPE_USER_RECHARGE = "USER_WALLET_RECHARGE";

    private final AccountBalancesService accountBalancesService;
    private final FundFlowsService fundFlowsService;
    private final PaymentRecordsService paymentRecordsService;

    public UserFundsController(
        AccountBalancesService accountBalancesService,
        FundFlowsService fundFlowsService,
        PaymentRecordsService paymentRecordsService
    ) {
        this.accountBalancesService = accountBalancesService;
        this.fundFlowsService = fundFlowsService;
        this.paymentRecordsService = paymentRecordsService;
    }

    @GetMapping("/summary")
    public Result<FundSummaryResponse> getSummary() {
        LoginUserInfo user = requireUser();
        AccountBalances balance = getBalance(user.getAccountId());
        return Result.success(buildSummary(balance));
    }

    @GetMapping("/flows")
    public Result<Page<FundFlowItemResponse>> listFlows(
        @RequestParam(value = "pageNo", required = false, defaultValue = "1") Integer pageNo,
        @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize
    ) {
        LoginUserInfo user = requireUser();
        int current = normalizePage(pageNo);
        int size = normalizePageSize(pageSize);

        Page<FundFlows> page = fundFlowsService.page(
            new Page<>(current, size),
            new LambdaQueryWrapper<FundFlows>()
                .eq(FundFlows::getAccountId, user.getAccountId())
                .eq(FundFlows::getAccountType, ACCOUNT_TYPE_USER)
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

    @PostMapping("/recharge")
    @Transactional(rollbackFor = Exception.class)
    public Result<FundSummaryResponse> recharge(@RequestBody(required = false) RechargeRequest request) {
        LoginUserInfo user = requireUser();
        BigDecimal amount = normalizeRechargeAmount(request == null ? null : request.getAmount());
        int paymentMethod = normalizeRechargeMethod(request == null ? null : request.getPaymentMethod());
        long now = System.currentTimeMillis();

        AccountBalances balance = ensureBalance(user.getAccountId(), now);
        BigDecimal balanceBefore = normalizeMoney(balance.getBalance());
        BigDecimal balanceAfter = balanceBefore.add(amount).setScale(2, RoundingMode.HALF_UP);

        balance.setBalance(balanceAfter);
        balance.setTotalIncome(normalizeMoney(balance.getTotalIncome()).add(amount).setScale(2, RoundingMode.HALF_UP));
        balance.setUpdatedTime(now);
        if (!accountBalancesService.updateById(balance)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新账户余额失败");
        }

        FundFlows flow = new FundFlows();
        flow.setId(SnowflakeIdUtil.nextFundFlowId());
        flow.setAccountId(user.getAccountId());
        flow.setAccountType(ACCOUNT_TYPE_USER);
        flow.setFlowType(FLOW_TYPE_INCOME);
        flow.setAmount(amount);
        flow.setBalanceBefore(balanceBefore);
        flow.setBalanceAfter(balanceAfter);
        flow.setBusinessType(BUSINESS_TYPE_USER_RECHARGE);
        flow.setBusinessId(flow.getId());
        flow.setDescription(buildRechargeDescription(paymentMethod));
        flow.setRemark("paymentMethod=" + paymentMethod);
        flow.setCreatedTime(now);
        flow.setVersion(0);
        flow.setIsDelete(0);
        if (!fundFlowsService.save(flow)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存充值流水失败");
        }

        PaymentRecords paymentRecord = new PaymentRecords();
        paymentRecord.setId(SnowflakeIdUtil.nextPaymentRecordId());
        paymentRecord.setPaymentNo(buildRechargePaymentNo(paymentRecord.getId()));
        paymentRecord.setOrderId(flow.getId());
        paymentRecord.setOrderType(PAYMENT_ORDER_TYPE_RECHARGE);
        paymentRecord.setAccountId(user.getAccountId());
        paymentRecord.setPaymentMethod(paymentMethod);
        paymentRecord.setPaymentAmount(amount);
        paymentRecord.setPaymentStatus(PAYMENT_RECORD_STATUS_SUCCESS);
        paymentRecord.setThirdPartyNo(buildThirdPartyNo(paymentRecord.getPaymentNo(), paymentMethod));
        paymentRecord.setPaymentTime(now);
        paymentRecord.setRefundAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        paymentRecord.setRemark(buildRechargeRemark(paymentMethod));
        paymentRecord.setCreatedTime(now);
        paymentRecord.setUpdatedTime(now);
        paymentRecord.setVersion(0);
        paymentRecord.setIsDelete(0);
        if (!paymentRecordsService.save(paymentRecord)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存充值支付记录失败");
        }

        return Result.success(buildSummary(balance));
    }

    private FundSummaryResponse buildSummary(AccountBalances balance) {
        FundSummaryResponse response = new FundSummaryResponse();
        response.setBalance(formatMoney(balance == null ? null : balance.getBalance()));
        response.setFrozenBalance(formatMoney(balance == null ? null : balance.getFrozenBalance()));
        response.setTotalIncome(formatMoney(balance == null ? null : balance.getTotalIncome()));
        response.setTotalExpense(formatMoney(balance == null ? null : balance.getTotalExpense()));
        return response;
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
                .eq(AccountBalances::getAccountType, ACCOUNT_TYPE_USER)
                .eq(AccountBalances::getIsDelete, 0)
                .last("limit 1"),
            false
        );
    }

    private AccountBalances ensureBalance(String accountId, long now) {
        AccountBalances balance = getBalance(accountId);
        if (balance != null) {
            return balance;
        }
        AccountBalances created = new AccountBalances();
        created.setId(SnowflakeIdUtil.nextAccountBalanceId());
        created.setAccountId(accountId);
        created.setAccountType(ACCOUNT_TYPE_USER);
        created.setBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        created.setFrozenBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        created.setTotalIncome(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        created.setTotalExpense(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        created.setCreatedTime(now);
        created.setUpdatedTime(now);
        created.setVersion(0);
        created.setIsDelete(0);
        if (!accountBalancesService.save(created)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化账户余额失败");
        }
        return created;
    }

    private LoginUserInfo requireUser() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.USER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问用户资金信息");
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

    private BigDecimal normalizeRechargeAmount(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount.setScale(2, RoundingMode.HALF_UP);
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "充值金额必须大于0");
        }
        if (value.compareTo(new BigDecimal("50000.00")) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "单次充值金额不能超过50000元");
        }
        return value;
    }

    private int normalizeRechargeMethod(Integer paymentMethod) {
        int value = paymentMethod == null ? PAYMENT_METHOD_WECHAT : paymentMethod;
        if (value != PAYMENT_METHOD_WECHAT && value != PAYMENT_METHOD_ALIPAY) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "充值方式仅支持微信或支付宝");
        }
        return value;
    }

    private String buildRechargeDescription(int paymentMethod) {
        if (paymentMethod == PAYMENT_METHOD_ALIPAY) {
            return "支付宝充值";
        }
        return "微信充值";
    }

    private String buildRechargeRemark(int paymentMethod) {
        if (paymentMethod == PAYMENT_METHOD_ALIPAY) {
            return "钱包支付宝充值";
        }
        return "钱包微信充值";
    }

    private String buildRechargePaymentNo(String paymentRecordId) {
        if (!StringUtils.hasText(paymentRecordId) || paymentRecordId.length() <= 2) {
            return "RCG" + System.currentTimeMillis();
        }
        return "RCG" + paymentRecordId.substring(2);
    }

    private String buildThirdPartyNo(String paymentNo, int paymentMethod) {
        String prefix = paymentMethod == PAYMENT_METHOD_ALIPAY ? "ALI" : "WX";
        return prefix + compactTradeNo(paymentNo);
    }

    private String compactTradeNo(String source) {
        String value = StringUtils.hasText(source) ? source.replaceAll("[^0-9A-Za-z]", "") : "";
        if (!StringUtils.hasText(value)) {
            value = String.valueOf(System.currentTimeMillis());
        }
        return value.length() > 28 ? value.substring(value.length() - 28) : value;
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String formatMoney(BigDecimal value) {
        return normalizeMoney(value).toPlainString();
    }

    @Data
    public static class RechargeRequest {
        private BigDecimal amount;
        private Integer paymentMethod;
    }
}
