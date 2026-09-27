package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.AccountBalances;
import com.example.backend.entity.FundFlows;
import com.example.backend.entity.PaymentRecords;
import com.example.backend.entity.RechargeOrders;
import com.example.backend.exception.BusinessException;
import com.example.backend.payment.OrderPaymentFinalizer;
import com.example.backend.payment.VerifiedPaymentCallback;
import com.example.backend.service.AccountBalancesService;
import com.example.backend.service.BusinessMetrics;
import com.example.backend.service.FundFlowsService;
import com.example.backend.service.PaymentPostingService;
import com.example.backend.service.PaymentRecordsService;
import com.example.backend.service.RechargeOrdersService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PaymentPostingServiceImpl implements PaymentPostingService {
    private static final int ORDER_TYPE_RECHARGE = 3;
    private static final int STATUS_SUCCESS = 3;
    private static final int ACCOUNT_TYPE_USER = 1;
    private static final int FLOW_TYPE_INCOME = 1;

    private final PaymentRecordsService paymentRecordsService;
    private final RechargeOrdersService rechargeOrdersService;
    private final AccountBalancesService accountBalancesService;
    private final FundFlowsService fundFlowsService;
    private final BusinessMetrics businessMetrics;
    private final Map<Integer, OrderPaymentFinalizer> finalizers = new HashMap<>();

    public PaymentPostingServiceImpl(
            PaymentRecordsService paymentRecordsService,
            RechargeOrdersService rechargeOrdersService,
            AccountBalancesService accountBalancesService,
            FundFlowsService fundFlowsService,
            BusinessMetrics businessMetrics,
            List<OrderPaymentFinalizer> finalizers) {
        this.paymentRecordsService = paymentRecordsService;
        this.rechargeOrdersService = rechargeOrdersService;
        this.accountBalancesService = accountBalancesService;
        this.fundFlowsService = fundFlowsService;
        this.businessMetrics = businessMetrics;
        for (OrderPaymentFinalizer finalizer : finalizers) {
            if (this.finalizers.put(finalizer.orderType(), finalizer) != null) {
                throw new IllegalStateException("订单支付 finalizer 重复注册: " + finalizer.orderType());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void postVerifiedPayment(int provider, VerifiedPaymentCallback callback) {
        validateCallback(callback);
        PaymentRecords payment =
                paymentRecordsService.getOne(
                        new LambdaQueryWrapper<PaymentRecords>()
                                .eq(PaymentRecords::getPaymentNo, callback.paymentNo())
                                .eq(PaymentRecords::getIsDelete, 0)
                                .last("limit 1 for update"),
                        false);
        if (payment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "支付单不存在");
        }
        if (!Integer.valueOf(provider).equals(payment.getPaymentMethod())) {
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR, "支付回调渠道不匹配");
        }
        if (Integer.valueOf(STATUS_SUCCESS).equals(payment.getPaymentStatus())) {
            if (!callback.providerTransactionNo().equals(payment.getThirdPartyNo())) {
                throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR, "支付单渠道交易号冲突");
            }
            return;
        }
        if (normalizeMoney(payment.getPaymentAmount()).compareTo(normalizeMoney(callback.amount()))
                        != 0
                || !payment.getCurrency().equalsIgnoreCase(callback.currency())) {
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR, "支付回调渠道、金额或币种不匹配");
        }
        if (!callback.paid()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "渠道交易尚未支付成功");
        }
        if (!Integer.valueOf(ORDER_TYPE_RECHARGE).equals(payment.getOrderType())) {
            postOrderPayment(payment, callback);
            return;
        }

        RechargeOrders recharge =
                rechargeOrdersService.getOne(
                        new LambdaQueryWrapper<RechargeOrders>()
                                .eq(RechargeOrders::getId, payment.getOrderId())
                                .eq(RechargeOrders::getIsDelete, 0)
                                .last("limit 1 for update"),
                        false);
        if (recharge == null
                || !recharge.getAccountId().equals(payment.getAccountId())
                || normalizeMoney(recharge.getAmount()).compareTo(normalizeMoney(callback.amount()))
                        != 0) {
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR, "充值订单与支付单不匹配");
        }

        long now = System.currentTimeMillis();
        AccountBalances balance = lockOrCreateBalance(payment.getAccountId(), now);
        BigDecimal before = normalizeMoney(balance.getBalance());
        BigDecimal after = before.add(normalizeMoney(callback.amount()));
        balance.setBalance(after);
        balance.setTotalIncome(normalizeMoney(balance.getTotalIncome()).add(callback.amount()));
        balance.setUpdatedTime(now);
        if (!accountBalancesService.updateById(balance)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "充值余额入账失败");
        }

        FundFlows flow = new FundFlows();
        flow.setId(SnowflakeIdUtil.nextFundFlowId());
        flow.setAccountId(payment.getAccountId());
        flow.setAccountType(ACCOUNT_TYPE_USER);
        flow.setFlowType(FLOW_TYPE_INCOME);
        flow.setAmount(normalizeMoney(callback.amount()));
        flow.setBalanceBefore(before);
        flow.setBalanceAfter(after);
        flow.setBusinessType("USER_WALLET_RECHARGE");
        flow.setBusinessId(recharge.getId());
        flow.setIdempotencyKey("PAYMENT_POSTING:" + payment.getPaymentNo());
        flow.setDescription("钱包充值入账");
        flow.setRemark("paymentNo=" + payment.getPaymentNo());
        flow.setCreatedTime(now);
        flow.setVersion(0);
        flow.setIsDelete(0);
        try {
            if (!fundFlowsService.save(flow)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存充值流水失败");
            }
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.DUPLICATE_KEY, "支付记账已处理");
        }

        payment.setPaymentStatus(STATUS_SUCCESS);
        payment.setThirdPartyNo(callback.providerTransactionNo());
        payment.setPaymentTime(now);
        payment.setCallbackData(callback.callbackDigest());
        payment.setCallbackProcessedTime(now);
        payment.setUpdatedTime(now);
        recharge.setStatus(STATUS_SUCCESS);
        recharge.setPostedTime(now);
        recharge.setUpdatedTime(now);
        if (!paymentRecordsService.updateById(payment)
                || !rechargeOrdersService.updateById(recharge)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新支付结果失败");
        }
        businessMetrics.paymentSucceeded(ORDER_TYPE_RECHARGE);
    }

    private void postOrderPayment(PaymentRecords payment, VerifiedPaymentCallback callback) {
        OrderPaymentFinalizer finalizer = finalizers.get(payment.getOrderType());
        if (finalizer == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该订单类型尚未接入统一支付记账");
        }
        long now = System.currentTimeMillis();
        payment.setPaymentStatus(STATUS_SUCCESS);
        payment.setThirdPartyNo(callback.providerTransactionNo());
        payment.setPaymentTime(now);
        payment.setCallbackData(callback.callbackDigest());
        payment.setCallbackProcessedTime(now);
        payment.setUpdatedTime(now);
        if (!paymentRecordsService.updateById(payment)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新支付结果失败");
        }
        finalizer.onPaymentSuccess(payment, now);
        businessMetrics.paymentSucceeded(payment.getOrderType());
    }

    private AccountBalances lockOrCreateBalance(String accountId, long now) {
        AccountBalances balance =
                accountBalancesService.getOne(
                        new LambdaQueryWrapper<AccountBalances>()
                                .eq(AccountBalances::getAccountId, accountId)
                                .eq(AccountBalances::getAccountType, ACCOUNT_TYPE_USER)
                                .eq(AccountBalances::getIsDelete, 0)
                                .last("limit 1 for update"),
                        false);
        if (balance != null) return balance;
        AccountBalances created = new AccountBalances();
        created.setId(SnowflakeIdUtil.nextAccountBalanceId());
        created.setAccountId(accountId);
        created.setAccountType(ACCOUNT_TYPE_USER);
        created.setBalance(BigDecimal.ZERO.setScale(2));
        created.setFrozenBalance(BigDecimal.ZERO.setScale(2));
        created.setTotalIncome(BigDecimal.ZERO.setScale(2));
        created.setTotalExpense(BigDecimal.ZERO.setScale(2));
        created.setCreatedTime(now);
        created.setUpdatedTime(now);
        created.setVersion(0);
        created.setIsDelete(0);
        if (!accountBalancesService.save(created)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化账户余额失败");
        }
        return created;
    }

    private void validateCallback(VerifiedPaymentCallback callback) {
        if (callback == null
                || !StringUtils.hasText(callback.paymentNo())
                || !StringUtils.hasText(callback.providerTransactionNo())
                || callback.amount() == null
                || callback.amount().compareTo(BigDecimal.ZERO) <= 0
                || !StringUtils.hasText(callback.currency())) {
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR, "支付回调字段不完整");
        }
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }
}
