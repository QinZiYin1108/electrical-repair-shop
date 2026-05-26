package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.AccountBalances;
import com.example.backend.entity.FundFlows;
import com.example.backend.entity.PaymentRecords;
import com.example.backend.entity.RepairOrderPayments;
import com.example.backend.entity.RepairOrders;
import com.example.backend.exception.BusinessException;
import com.example.backend.service.AccountBalancesService;
import com.example.backend.service.FundFlowsService;
import com.example.backend.service.PaymentRecordsService;
import com.example.backend.service.RepairOrderFundService;
import com.example.backend.service.RepairOrdersService;
import com.example.backend.service.SystemConfigsService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class RepairOrderFundServiceImpl implements RepairOrderFundService {

    private static final int ACCOUNT_TYPE_USER = 1;
    private static final int ACCOUNT_TYPE_TECHNICIAN = 2;
    private static final int ACCOUNT_TYPE_PLATFORM = 3;

    private static final String PLATFORM_ACCOUNT_ID = "PLATFORM";

    private static final int FLOW_TYPE_INCOME = 1;
    private static final int FLOW_TYPE_EXPENSE = 2;

    private static final int PAYMENT_METHOD_WECHAT = 1;
    private static final int PAYMENT_METHOD_ALIPAY = 2;
    private static final int PAYMENT_METHOD_WALLET = 5;

    private static final int PAYMENT_RECORD_STATUS_SUCCESS = 3;
    private static final int PAYMENT_RECORD_STATUS_REFUNDED = 5;

    // Business types (fund_flows.business_type)
    private static final String BT_USER_PREPAY = "REPAIR_ORDER_PREPAY";
    private static final String BT_USER_TAIL_PAY = "REPAIR_ORDER_TAIL_PAY";
    private static final String BT_PLATFORM_ESCROW_IN = "REPAIR_ORDER_ESCROW_IN";
    private static final String BT_PLATFORM_ESCROW_IN_TAIL = "REPAIR_ORDER_ESCROW_IN_TAIL";
    private static final String BT_PLATFORM_ESCROW_OUT_SETTLE = "REPAIR_ORDER_ESCROW_OUT_SETTLE";
    private static final String BT_PLATFORM_ESCROW_OUT_CANCEL_SETTLE = "REPAIR_ORDER_ESCROW_OUT_CANCEL_SETTLE";
    private static final String BT_PLATFORM_ESCROW_OUT_REFUND = "REPAIR_ORDER_ESCROW_OUT_REFUND";
    private static final String BT_TECHNICIAN_SETTLEMENT = "REPAIR_ORDER_SETTLEMENT";
    private static final String BT_TECHNICIAN_SETTLEMENT_PENDING = "REPAIR_ORDER_SETTLEMENT_PENDING";
    private static final String BT_TECHNICIAN_SETTLEMENT_RELEASE = "REPAIR_ORDER_SETTLEMENT_RELEASE";
    private static final String BT_TECHNICIAN_CANCEL_SETTLEMENT = "REPAIR_ORDER_CANCEL_SETTLEMENT";
    private static final String BT_TECHNICIAN_REFUND_DEDUCT = "REPAIR_ORDER_REFUND_DEDUCT";
    private static final String BT_USER_REFUND = "REPAIR_ORDER_REFUND";

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final long DEFAULT_AFTER_SALES_PROTECTION_DAYS = 7L;

    private final AccountBalancesService accountBalancesService;
    private final FundFlowsService fundFlowsService;
    private final PaymentRecordsService paymentRecordsService;
    private final RepairOrdersService repairOrdersService;
    private final SystemConfigsService systemConfigsService;

    public RepairOrderFundServiceImpl(
        AccountBalancesService accountBalancesService,
        FundFlowsService fundFlowsService,
        PaymentRecordsService paymentRecordsService,
        RepairOrdersService repairOrdersService,
        SystemConfigsService systemConfigsService
    ) {
        this.accountBalancesService = accountBalancesService;
        this.fundFlowsService = fundFlowsService;
        this.paymentRecordsService = paymentRecordsService;
        this.repairOrdersService = repairOrdersService;
        this.systemConfigsService = systemConfigsService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordOrderPrepay(
        String userAccountId,
        String technicianAccountId,
        String orderId,
        String orderNo,
        Integer paymentMethod,
        BigDecimal amount,
        long now
    ) {
        BigDecimal normalizedAmount = normalizeAmount(amount);
        if (!StringUtils.hasText(orderId) || normalizedAmount.compareTo(ZERO) <= 0) {
            return;
        }
        if (!StringUtils.hasText(userAccountId)) {
            return;
        }

        ensureAccountBalance(PLATFORM_ACCOUNT_ID, ACCOUNT_TYPE_PLATFORM, now);

        if (safeInt(paymentMethod) == PAYMENT_METHOD_WALLET) {
            ensureAccountBalance(userAccountId, ACCOUNT_TYPE_USER, now);
            FundFlows existingUserFlow = fundFlowsService.getOne(
                new LambdaQueryWrapper<FundFlows>()
                    .eq(FundFlows::getAccountId, userAccountId)
                    .eq(FundFlows::getAccountType, ACCOUNT_TYPE_USER)
                    .eq(FundFlows::getBusinessType, BT_USER_PREPAY)
                    .eq(FundFlows::getBusinessId, orderId)
                    .eq(FundFlows::getIsDelete, 0)
                    .last("limit 1"),
                false
            );
            if (existingUserFlow == null) {
                recordUserWalletExpense(
                    userAccountId,
                    orderId,
                    orderNo,
                    normalizedAmount,
                    now,
                    BT_USER_PREPAY,
                    "维修订单预付费用（钱包支付）"
                );
            }
        }

        // Idempotency check: platform escrow inflow
        FundFlows existingEscrowInFlow = fundFlowsService.getOne(
            new LambdaQueryWrapper<FundFlows>()
                .eq(FundFlows::getAccountId, PLATFORM_ACCOUNT_ID)
                .eq(FundFlows::getAccountType, ACCOUNT_TYPE_PLATFORM)
                .eq(FundFlows::getBusinessType, BT_PLATFORM_ESCROW_IN)
                .eq(FundFlows::getBusinessId, orderId)
                .eq(FundFlows::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (existingEscrowInFlow == null) {
            escrowPlatformIn(
                orderId,
                orderNo,
                normalizedAmount,
                now,
                BT_PLATFORM_ESCROW_IN,
                "维修订单费用托管入账"
            );
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordOrderTailPay(
        String userAccountId,
        String technicianAccountId,
        String orderId,
        String orderNo,
        Integer paymentMethod,
        BigDecimal amount,
        long now
    ) {
        BigDecimal normalizedAmount = normalizeAmount(amount);
        if (!StringUtils.hasText(orderId) || normalizedAmount.compareTo(ZERO) <= 0) {
            return;
        }
        if (!StringUtils.hasText(userAccountId)) {
            return;
        }

        ensureAccountBalance(PLATFORM_ACCOUNT_ID, ACCOUNT_TYPE_PLATFORM, now);

        if (safeInt(paymentMethod) == PAYMENT_METHOD_WALLET) {
            ensureAccountBalance(userAccountId, ACCOUNT_TYPE_USER, now);
            FundFlows existingUserFlow = fundFlowsService.getOne(
                new LambdaQueryWrapper<FundFlows>()
                    .eq(FundFlows::getAccountId, userAccountId)
                    .eq(FundFlows::getAccountType, ACCOUNT_TYPE_USER)
                    .eq(FundFlows::getBusinessType, BT_USER_TAIL_PAY)
                    .eq(FundFlows::getBusinessId, orderId)
                    .eq(FundFlows::getIsDelete, 0)
                    .last("limit 1"),
                false
            );
            if (existingUserFlow == null) {
                recordUserWalletExpense(
                    userAccountId,
                    orderId,
                    orderNo,
                    normalizedAmount,
                    now,
                    BT_USER_TAIL_PAY,
                    "维修订单尾款支付（钱包支付）"
                );
            }
        }

        FundFlows existingEscrowInFlow = fundFlowsService.getOne(
            new LambdaQueryWrapper<FundFlows>()
                .eq(FundFlows::getAccountId, PLATFORM_ACCOUNT_ID)
                .eq(FundFlows::getAccountType, ACCOUNT_TYPE_PLATFORM)
                .eq(FundFlows::getBusinessType, BT_PLATFORM_ESCROW_IN_TAIL)
                .eq(FundFlows::getBusinessId, orderId)
                .eq(FundFlows::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (existingEscrowInFlow == null) {
            escrowPlatformIn(
                orderId,
                orderNo,
                normalizedAmount,
                now,
                BT_PLATFORM_ESCROW_IN_TAIL,
                "维修订单尾款托管入账"
            );
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleOnOrderCompleted(RepairOrders order, RepairOrderPayments payment, long now) {
        if (order == null || payment == null) {
            return;
        }
        String orderId = order.getId();
        if (!StringUtils.hasText(orderId)) {
            return;
        }
        String technicianAccountId = order.getTechnicianAccountId();
        if (!StringUtils.hasText(technicianAccountId)) {
            return;
        }

        BigDecimal amount = normalizeAmount(payment.getActualAmount());
        if (amount.compareTo(ZERO) <= 0) {
            return;
        }

        // Only settle when order completed.
        if (safeInt(order.getStatus()) != 6) {
            return;
        }

        ensureAccountBalance(technicianAccountId, ACCOUNT_TYPE_TECHNICIAN, now);
        ensureAccountBalance(PLATFORM_ACCOUNT_ID, ACCOUNT_TYPE_PLATFORM, now);

        // Idempotency check: settlement flow exists
        FundFlows existingSettlementFlow = fundFlowsService.getOne(
            new LambdaQueryWrapper<FundFlows>()
                .eq(FundFlows::getAccountId, technicianAccountId)
                .eq(FundFlows::getAccountType, ACCOUNT_TYPE_TECHNICIAN)
                .in(FundFlows::getBusinessType, BT_TECHNICIAN_SETTLEMENT, BT_TECHNICIAN_SETTLEMENT_PENDING)
                .eq(FundFlows::getBusinessId, orderId)
                .eq(FundFlows::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (existingSettlementFlow != null) {
            return;
        }

        // Release platform escrow first
        platformEscrowOutSettle(orderId, order.getOrderNo(), amount, now);

        // Credit technician available income
        AccountBalances balance = requireAccountBalance(technicianAccountId, ACCOUNT_TYPE_TECHNICIAN);
        BigDecimal availableBefore = defaultZero(balance.getBalance());
        BigDecimal availableAfter = availableBefore.setScale(2, RoundingMode.HALF_UP);
        BigDecimal frozenAfter = defaultZero(balance.getFrozenBalance()).add(amount).setScale(2, RoundingMode.HALF_UP);
        balance.setFrozenBalance(frozenAfter);
        balance.setTotalIncome(defaultZero(balance.getTotalIncome()).add(amount).setScale(2, RoundingMode.HALF_UP));
        balance.setUpdatedTime(now);

        if (!accountBalancesService.updateById(balance)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "冻结师傅待提现收入失败");
        }

        FundFlows flow = new FundFlows();
        flow.setId(SnowflakeIdUtil.nextFundFlowId());
        flow.setAccountId(technicianAccountId);
        flow.setAccountType(ACCOUNT_TYPE_TECHNICIAN);
        flow.setFlowType(FLOW_TYPE_INCOME);
        flow.setAmount(amount);
        flow.setBalanceBefore(availableBefore.setScale(2, RoundingMode.HALF_UP));
        flow.setBalanceAfter(availableAfter);
        flow.setBusinessType(BT_TECHNICIAN_SETTLEMENT_PENDING);
        flow.setBusinessId(orderId);
        flow.setDescription("维修订单收入已冻结，完成满" + getAfterSalesProtectionDays() + "天后可提现");
        flow.setRemark("orderNo=" + safe(order.getOrderNo()) + ",frozenBalance=" + frozenAfter.toPlainString());
        flow.setCreatedTime(now);
        flow.setIsDelete(0);
        if (!fundFlowsService.save(flow)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建资金流水失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseEligibleTechnicianFunds(String technicianAccountId, long now) {
        if (!StringUtils.hasText(technicianAccountId)) {
            return;
        }

        List<FundFlows> pendingFlows = fundFlowsService.list(
            new LambdaQueryWrapper<FundFlows>()
                .eq(FundFlows::getAccountId, technicianAccountId)
                .eq(FundFlows::getAccountType, ACCOUNT_TYPE_TECHNICIAN)
                .eq(FundFlows::getBusinessType, BT_TECHNICIAN_SETTLEMENT_PENDING)
                .eq(FundFlows::getIsDelete, 0)
                .orderByAsc(FundFlows::getCreatedTime)
        );
        if (pendingFlows == null || pendingFlows.isEmpty()) {
            return;
        }

        ensureAccountBalance(technicianAccountId, ACCOUNT_TYPE_TECHNICIAN, now);

        for (FundFlows pendingFlow : pendingFlows) {
            String orderId = pendingFlow.getBusinessId();
            if (!StringUtils.hasText(orderId)) {
                continue;
            }

            FundFlows existingReleaseFlow = fundFlowsService.getOne(
                new LambdaQueryWrapper<FundFlows>()
                    .eq(FundFlows::getAccountId, technicianAccountId)
                    .eq(FundFlows::getAccountType, ACCOUNT_TYPE_TECHNICIAN)
                    .eq(FundFlows::getBusinessType, BT_TECHNICIAN_SETTLEMENT_RELEASE)
                    .eq(FundFlows::getBusinessId, orderId)
                    .eq(FundFlows::getIsDelete, 0)
                    .last("limit 1"),
                false
            );
            if (existingReleaseFlow != null) {
                continue;
            }

            FundFlows existingDeductFlow = fundFlowsService.getOne(
                new LambdaQueryWrapper<FundFlows>()
                    .eq(FundFlows::getAccountId, technicianAccountId)
                    .eq(FundFlows::getAccountType, ACCOUNT_TYPE_TECHNICIAN)
                    .eq(FundFlows::getBusinessType, BT_TECHNICIAN_REFUND_DEDUCT)
                    .eq(FundFlows::getBusinessId, orderId)
                    .eq(FundFlows::getIsDelete, 0)
                    .last("limit 1"),
                false
            );
            if (existingDeductFlow != null) {
                continue;
            }

            RepairOrders order = paymentOrder(orderId);
            if (order == null || safeInt(order.getStatus()) != 6) {
                continue;
            }
            long completionTime = order.getCompletionTime() == null ? 0L : order.getCompletionTime();
            if (completionTime <= 0L || now - completionTime < getAfterSalesProtectionPeriodMillis()) {
                continue;
            }

            BigDecimal amount = normalizeAmount(pendingFlow.getAmount());
            if (amount.compareTo(ZERO) <= 0) {
                continue;
            }

            AccountBalances balance = requireAccountBalance(technicianAccountId, ACCOUNT_TYPE_TECHNICIAN);
            BigDecimal availableBefore = defaultZero(balance.getBalance()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal frozenBefore = defaultZero(balance.getFrozenBalance()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal frozenAfter = frozenBefore.subtract(amount);
            if (frozenAfter.compareTo(ZERO) < 0) {
                frozenAfter = ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            BigDecimal availableAfter = availableBefore.add(amount).setScale(2, RoundingMode.HALF_UP);

            balance.setBalance(availableAfter);
            balance.setFrozenBalance(frozenAfter);
            balance.setUpdatedTime(now);
            if (!accountBalancesService.updateById(balance)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "释放可提现金额失败");
            }

            FundFlows releaseFlow = new FundFlows();
            releaseFlow.setId(SnowflakeIdUtil.nextFundFlowId());
            releaseFlow.setAccountId(technicianAccountId);
            releaseFlow.setAccountType(ACCOUNT_TYPE_TECHNICIAN);
            releaseFlow.setFlowType(FLOW_TYPE_INCOME);
            releaseFlow.setAmount(amount);
            releaseFlow.setBalanceBefore(availableBefore);
            releaseFlow.setBalanceAfter(availableAfter);
            releaseFlow.setBusinessType(BT_TECHNICIAN_SETTLEMENT_RELEASE);
            releaseFlow.setBusinessId(orderId);
            releaseFlow.setDescription("维修订单售后期已结束，收入转为可提现");
            releaseFlow.setRemark(
                "orderNo=" + safe(order.getOrderNo())
                    + ",frozenBefore=" + frozenBefore.toPlainString()
                    + ",frozenAfter=" + frozenAfter.toPlainString()
            );
            releaseFlow.setCreatedTime(now);
            releaseFlow.setIsDelete(0);
            if (!fundFlowsService.save(releaseFlow)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建资金流水失败");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleRetainedAmountOnCancel(RepairOrders order, BigDecimal amount, long now) {
        if (order == null) {
            return;
        }
        String orderId = order.getId();
        if (!StringUtils.hasText(orderId)) {
            return;
        }
        String technicianAccountId = order.getTechnicianAccountId();
        if (!StringUtils.hasText(technicianAccountId)) {
            return;
        }

        BigDecimal normalizedAmount = normalizeAmount(amount);
        if (normalizedAmount.compareTo(ZERO) <= 0) {
            return;
        }

        ensureAccountBalance(technicianAccountId, ACCOUNT_TYPE_TECHNICIAN, now);
        ensureAccountBalance(PLATFORM_ACCOUNT_ID, ACCOUNT_TYPE_PLATFORM, now);

        FundFlows existingSettlementFlow = fundFlowsService.getOne(
            new LambdaQueryWrapper<FundFlows>()
                .eq(FundFlows::getAccountId, technicianAccountId)
                .eq(FundFlows::getAccountType, ACCOUNT_TYPE_TECHNICIAN)
                .eq(FundFlows::getBusinessType, BT_TECHNICIAN_CANCEL_SETTLEMENT)
                .eq(FundFlows::getBusinessId, orderId)
                .eq(FundFlows::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (existingSettlementFlow != null) {
            return;
        }

        platformEscrowOutCancelSettle(orderId, order.getOrderNo(), normalizedAmount, now);

        AccountBalances balance = requireAccountBalance(technicianAccountId, ACCOUNT_TYPE_TECHNICIAN);
        BigDecimal availableBefore = defaultZero(balance.getBalance()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal availableAfter = availableBefore.add(normalizedAmount).setScale(2, RoundingMode.HALF_UP);
        balance.setBalance(availableAfter);
        balance.setTotalIncome(
            defaultZero(balance.getTotalIncome()).add(normalizedAmount).setScale(2, RoundingMode.HALF_UP)
        );
        balance.setUpdatedTime(now);

        if (!accountBalancesService.updateById(balance)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "取消后的上门费结算失败");
        }

        FundFlows flow = new FundFlows();
        flow.setId(SnowflakeIdUtil.nextFundFlowId());
        flow.setAccountId(technicianAccountId);
        flow.setAccountType(ACCOUNT_TYPE_TECHNICIAN);
        flow.setFlowType(FLOW_TYPE_INCOME);
        flow.setAmount(normalizedAmount);
        flow.setBalanceBefore(availableBefore);
        flow.setBalanceAfter(availableAfter);
        flow.setBusinessType(BT_TECHNICIAN_CANCEL_SETTLEMENT);
        flow.setBusinessId(orderId);
        flow.setDescription("维修订单取消后的上门费结算");
        flow.setRemark("orderNo=" + safe(order.getOrderNo()));
        flow.setCreatedTime(now);
        flow.setIsDelete(0);
        if (!fundFlowsService.save(flow)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建资金流水失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundOnOrderClosed(RepairOrders order, RepairOrderPayments payment, String reason, long now) {
        if (order == null || payment == null) {
            return;
        }
        String orderId = order.getId();
        if (!StringUtils.hasText(orderId)) {
            return;
        }
        String userAccountId = order.getAccountId();
        if (!StringUtils.hasText(userAccountId)) {
            return;
        }

        BigDecimal amount = normalizeAmount(payment.getActualAmount());
        if (amount.compareTo(ZERO) <= 0) {
            return;
        }

        // Only allow refund for cancelled/refunded orders.
        int status = safeInt(order.getStatus());
        if (status != 7 && status != 8) {
            return;
        }

        ensureAccountBalance(PLATFORM_ACCOUNT_ID, ACCOUNT_TYPE_PLATFORM, now);

        // Release platform escrow and create platform refund flow
        platformEscrowOutRefund(orderId, order.getOrderNo(), amount, reason, now);

        BigDecimal walletRefundAmount = sumWalletPaidAmount(orderId);
        if (walletRefundAmount.compareTo(ZERO) > 0) {
            ensureAccountBalance(userAccountId, ACCOUNT_TYPE_USER, now);
            FundFlows existingUserRefundFlow = fundFlowsService.getOne(
                new LambdaQueryWrapper<FundFlows>()
                    .eq(FundFlows::getAccountId, userAccountId)
                    .eq(FundFlows::getAccountType, ACCOUNT_TYPE_USER)
                    .eq(FundFlows::getBusinessType, BT_USER_REFUND)
                    .eq(FundFlows::getBusinessId, orderId)
                    .eq(FundFlows::getIsDelete, 0)
                    .last("limit 1"),
                false
            );
            if (existingUserRefundFlow == null) {
                refundToWallet(userAccountId, orderId, order.getOrderNo(), walletRefundAmount, now);
            }
        }

        deductTechnicianIncomeOnRefund(order, amount, reason, now);
        markPaymentRecordsRefunded(orderId, reason, now);
    }

    private void deductTechnicianIncomeOnRefund(RepairOrders order, BigDecimal amount, String reason, long now) {
        if (order == null) {
            return;
        }
        String orderId = order.getId();
        String technicianAccountId = order.getTechnicianAccountId();
        if (!StringUtils.hasText(orderId) || !StringUtils.hasText(technicianAccountId)) {
            return;
        }

        BigDecimal normalizedAmount = normalizeAmount(amount);
        if (normalizedAmount.compareTo(ZERO) <= 0) {
            return;
        }

        FundFlows existingDeductFlow = fundFlowsService.getOne(
            new LambdaQueryWrapper<FundFlows>()
                .eq(FundFlows::getAccountId, technicianAccountId)
                .eq(FundFlows::getAccountType, ACCOUNT_TYPE_TECHNICIAN)
                .eq(FundFlows::getBusinessType, BT_TECHNICIAN_REFUND_DEDUCT)
                .eq(FundFlows::getBusinessId, orderId)
                .eq(FundFlows::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (existingDeductFlow != null) {
            return;
        }

        FundFlows pendingSettlementFlow = fundFlowsService.getOne(
            new LambdaQueryWrapper<FundFlows>()
                .eq(FundFlows::getAccountId, technicianAccountId)
                .eq(FundFlows::getAccountType, ACCOUNT_TYPE_TECHNICIAN)
                .in(FundFlows::getBusinessType, BT_TECHNICIAN_SETTLEMENT_PENDING, BT_TECHNICIAN_SETTLEMENT_RELEASE, BT_TECHNICIAN_SETTLEMENT)
                .eq(FundFlows::getBusinessId, orderId)
                .eq(FundFlows::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (pendingSettlementFlow == null) {
            return;
        }

        ensureAccountBalance(technicianAccountId, ACCOUNT_TYPE_TECHNICIAN, now);
        AccountBalances balance = requireAccountBalance(technicianAccountId, ACCOUNT_TYPE_TECHNICIAN);
        BigDecimal frozenBefore = defaultZero(balance.getFrozenBalance()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal availableBefore = defaultZero(balance.getBalance()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal frozenDeduction = frozenBefore.min(normalizedAmount);
        BigDecimal remaining = normalizedAmount.subtract(frozenDeduction).setScale(2, RoundingMode.HALF_UP);
        BigDecimal availableDeduction = remaining.compareTo(ZERO) > 0 ? availableBefore.min(remaining) : ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDeduction = frozenDeduction.add(availableDeduction).setScale(2, RoundingMode.HALF_UP);

        if (totalDeduction.compareTo(normalizedAmount) < 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "师傅账户金额不足，无法完成退款扣减");
        }

        BigDecimal frozenAfter = frozenBefore.subtract(frozenDeduction).setScale(2, RoundingMode.HALF_UP);
        BigDecimal availableAfter = availableBefore.subtract(availableDeduction).setScale(2, RoundingMode.HALF_UP);
        balance.setFrozenBalance(frozenAfter);
        balance.setBalance(availableAfter);
        balance.setTotalExpense(defaultZero(balance.getTotalExpense()).add(totalDeduction).setScale(2, RoundingMode.HALF_UP));
        balance.setUpdatedTime(now);
        if (!accountBalancesService.updateById(balance)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "扣减师傅退款金额失败");
        }

        FundFlows flow = new FundFlows();
        flow.setId(SnowflakeIdUtil.nextFundFlowId());
        flow.setAccountId(technicianAccountId);
        flow.setAccountType(ACCOUNT_TYPE_TECHNICIAN);
        flow.setFlowType(FLOW_TYPE_EXPENSE);
        flow.setAmount(totalDeduction);
        flow.setBalanceBefore(availableBefore);
        flow.setBalanceAfter(availableAfter);
        flow.setBusinessType(BT_TECHNICIAN_REFUND_DEDUCT);
        flow.setBusinessId(orderId);
        flow.setDescription("维修订单售后退款，扣减师傅收入");
        flow.setRemark(
            "orderNo=" + safe(order.getOrderNo())
                + ",reason=" + safe(reason)
                + ",frozenDeduction=" + frozenDeduction.toPlainString()
                + ",availableDeduction=" + availableDeduction.toPlainString()
        );
        flow.setCreatedTime(now);
        flow.setIsDelete(0);
        if (!fundFlowsService.save(flow)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建资金流水失败");
        }
    }

    private void recordUserWalletExpense(
        String userAccountId,
        String orderId,
        String orderNo,
        BigDecimal amount,
        long now,
        String businessType,
        String description
    ) {
        AccountBalances balance = requireAccountBalance(userAccountId, ACCOUNT_TYPE_USER);
        BigDecimal balanceBefore = defaultZero(balance.getBalance()).setScale(2, RoundingMode.HALF_UP);
        if (balanceBefore.compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "钱包余额不足");
        }
        BigDecimal balanceAfter = balanceBefore.subtract(amount).setScale(2, RoundingMode.HALF_UP);

        balance.setBalance(balanceAfter);
        balance.setTotalExpense(defaultZero(balance.getTotalExpense()).add(amount).setScale(2, RoundingMode.HALF_UP));
        balance.setUpdatedTime(now);
        if (!accountBalancesService.updateById(balance)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户资金统计失败");
        }

        FundFlows flow = new FundFlows();
        flow.setId(SnowflakeIdUtil.nextFundFlowId());
        flow.setAccountId(userAccountId);
        flow.setAccountType(ACCOUNT_TYPE_USER);
        flow.setFlowType(FLOW_TYPE_EXPENSE);
        flow.setAmount(amount);
        flow.setBalanceBefore(balanceBefore);
        flow.setBalanceAfter(balanceAfter);
        flow.setBusinessType(businessType);
        flow.setBusinessId(orderId);
        flow.setDescription(description);
        flow.setRemark("orderNo=" + orderNo);
        flow.setCreatedTime(now);
        flow.setIsDelete(0);
        if (!fundFlowsService.save(flow)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建资金流水失败");
        }
    }

    private void refundToWallet(String userAccountId, String orderId, String orderNo, BigDecimal amount, long now) {
        AccountBalances userBalance = requireAccountBalance(userAccountId, ACCOUNT_TYPE_USER);
        BigDecimal balanceBefore = defaultZero(userBalance.getBalance()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal balanceAfter = balanceBefore.add(amount).setScale(2, RoundingMode.HALF_UP);

        userBalance.setBalance(balanceAfter);
        userBalance.setTotalIncome(defaultZero(userBalance.getTotalIncome()).add(amount).setScale(2, RoundingMode.HALF_UP));
        userBalance.setUpdatedTime(now);
        if (!accountBalancesService.updateById(userBalance)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户资金统计失败");
        }

        FundFlows flow = new FundFlows();
        flow.setId(SnowflakeIdUtil.nextFundFlowId());
        flow.setAccountId(userAccountId);
        flow.setAccountType(ACCOUNT_TYPE_USER);
        flow.setFlowType(FLOW_TYPE_INCOME);
        flow.setAmount(amount);
        flow.setBalanceBefore(balanceBefore);
        flow.setBalanceAfter(balanceAfter);
        flow.setBusinessType(BT_USER_REFUND);
        flow.setBusinessId(orderId);
        flow.setDescription("维修订单退款（退回钱包）");
        flow.setRemark("orderNo=" + safe(orderNo));
        flow.setCreatedTime(now);
        flow.setIsDelete(0);
        if (!fundFlowsService.save(flow)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建资金流水失败");
        }
    }

    private void markPaymentRecordsRefunded(String orderId, String reason, long now) {
        if (!StringUtils.hasText(orderId)) {
            return;
        }
        for (PaymentRecords record : paymentRecordsService.list(
            new LambdaQueryWrapper<PaymentRecords>()
                .eq(PaymentRecords::getOrderId, orderId)
                .eq(PaymentRecords::getOrderType, 1)
                .eq(PaymentRecords::getIsDelete, 0)
                .orderByAsc(PaymentRecords::getCreatedTime)
        )) {
            if (safeInt(record.getPaymentStatus()) == PAYMENT_RECORD_STATUS_REFUNDED) {
                continue;
            }
            record.setPaymentStatus(PAYMENT_RECORD_STATUS_REFUNDED);
            record.setRefundAmount(normalizeAmount(defaultZero(record.getPaymentAmount())));
            record.setRefundTime(now);
            record.setRefundReason(StringUtils.hasText(reason) ? reason : "订单取消退款");
            record.setUpdatedTime(now);
            if (!paymentRecordsService.updateById(record)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新支付记录退款状态失败");
            }
        }
    }

    private void escrowPlatformIn(
        String orderId,
        String orderNo,
        BigDecimal amount,
        long now,
        String businessType,
        String description
    ) {
        AccountBalances platform = requireAccountBalance(PLATFORM_ACCOUNT_ID, ACCOUNT_TYPE_PLATFORM);
        BigDecimal balanceBefore = defaultZero(platform.getBalance()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal balanceAfter = balanceBefore;

        platform.setFrozenBalance(defaultZero(platform.getFrozenBalance()).add(amount).setScale(2, RoundingMode.HALF_UP));
        platform.setTotalIncome(defaultZero(platform.getTotalIncome()).add(amount).setScale(2, RoundingMode.HALF_UP));
        platform.setUpdatedTime(now);
        if (!accountBalancesService.updateById(platform)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新平台扫码托管金额失败");
        }

        FundFlows flow = new FundFlows();
        flow.setId(SnowflakeIdUtil.nextFundFlowId());
        flow.setAccountId(PLATFORM_ACCOUNT_ID);
        flow.setAccountType(ACCOUNT_TYPE_PLATFORM);
        flow.setFlowType(FLOW_TYPE_INCOME);
        flow.setAmount(amount);
        flow.setBalanceBefore(balanceBefore);
        flow.setBalanceAfter(balanceAfter);
        flow.setBusinessType(businessType);
        flow.setBusinessId(orderId);
        flow.setDescription(description);
        flow.setRemark("orderNo=" + orderNo);
        flow.setCreatedTime(now);
        flow.setIsDelete(0);
        if (!fundFlowsService.save(flow)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建资金流水失败");
        }
    }

    private BigDecimal sumWalletPaidAmount(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            return ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal total = ZERO.setScale(2, RoundingMode.HALF_UP);
        for (PaymentRecords record : paymentRecordsService.list(
            new LambdaQueryWrapper<PaymentRecords>()
                .eq(PaymentRecords::getOrderId, orderId)
                .eq(PaymentRecords::getOrderType, 1)
                .eq(PaymentRecords::getPaymentMethod, PAYMENT_METHOD_WALLET)
                .eq(PaymentRecords::getIsDelete, 0)
        )) {
            int status = safeInt(record.getPaymentStatus());
            if (status == PAYMENT_RECORD_STATUS_SUCCESS || status == PAYMENT_RECORD_STATUS_REFUNDED) {
                total = total.add(normalizeAmount(record.getPaymentAmount()));
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private RepairOrders paymentOrder(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            return null;
        }
        return repairOrdersService.getOne(
            new LambdaQueryWrapper<RepairOrders>()
                .eq(RepairOrders::getId, orderId)
                .eq(RepairOrders::getIsDelete, 0)
                .last("limit 1"),
            false
        );
    }

    private void platformEscrowOutSettle(String orderId, String orderNo, BigDecimal amount, long now) {
        // idempotency: platform settle-out flow
        FundFlows existing = fundFlowsService.getOne(
            new LambdaQueryWrapper<FundFlows>()
                .eq(FundFlows::getAccountId, PLATFORM_ACCOUNT_ID)
                .eq(FundFlows::getAccountType, ACCOUNT_TYPE_PLATFORM)
                .eq(FundFlows::getBusinessType, BT_PLATFORM_ESCROW_OUT_SETTLE)
                .eq(FundFlows::getBusinessId, orderId)
                .eq(FundFlows::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (existing != null) {
            return;
        }

        AccountBalances platform = requireAccountBalance(PLATFORM_ACCOUNT_ID, ACCOUNT_TYPE_PLATFORM);
        BigDecimal balanceBefore = defaultZero(platform.getBalance()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal balanceAfter = balanceBefore;

        BigDecimal frozenAfter = defaultZero(platform.getFrozenBalance()).subtract(amount);
        if (frozenAfter.compareTo(ZERO) < 0) {
            frozenAfter = ZERO;
        }
        platform.setFrozenBalance(frozenAfter.setScale(2, RoundingMode.HALF_UP));
        platform.setTotalExpense(defaultZero(platform.getTotalExpense()).add(amount).setScale(2, RoundingMode.HALF_UP));
        platform.setUpdatedTime(now);
        if (!accountBalancesService.updateById(platform)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新平台托管金额出账失败");
        }

        FundFlows flow = new FundFlows();
        flow.setId(SnowflakeIdUtil.nextFundFlowId());
        flow.setAccountId(PLATFORM_ACCOUNT_ID);
        flow.setAccountType(ACCOUNT_TYPE_PLATFORM);
        flow.setFlowType(FLOW_TYPE_EXPENSE);
        flow.setAmount(amount);
        flow.setBalanceBefore(balanceBefore);
        flow.setBalanceAfter(balanceAfter);
        flow.setBusinessType(BT_PLATFORM_ESCROW_OUT_SETTLE);
        flow.setBusinessId(orderId);
        flow.setDescription("维修订单结算出账（打款给师傅）");
        flow.setRemark("orderNo=" + orderNo);
        flow.setCreatedTime(now);
        flow.setIsDelete(0);
        if (!fundFlowsService.save(flow)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建资金流水失败");
        }
    }

    private void platformEscrowOutCancelSettle(String orderId, String orderNo, BigDecimal amount, long now) {
        FundFlows existing = fundFlowsService.getOne(
            new LambdaQueryWrapper<FundFlows>()
                .eq(FundFlows::getAccountId, PLATFORM_ACCOUNT_ID)
                .eq(FundFlows::getAccountType, ACCOUNT_TYPE_PLATFORM)
                .eq(FundFlows::getBusinessType, BT_PLATFORM_ESCROW_OUT_CANCEL_SETTLE)
                .eq(FundFlows::getBusinessId, orderId)
                .eq(FundFlows::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (existing != null) {
            return;
        }

        AccountBalances platform = requireAccountBalance(PLATFORM_ACCOUNT_ID, ACCOUNT_TYPE_PLATFORM);
        BigDecimal balanceBefore = defaultZero(platform.getBalance()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal balanceAfter = balanceBefore;

        BigDecimal frozenAfter = defaultZero(platform.getFrozenBalance()).subtract(amount);
        if (frozenAfter.compareTo(ZERO) < 0) {
            frozenAfter = ZERO;
        }
        platform.setFrozenBalance(frozenAfter.setScale(2, RoundingMode.HALF_UP));
        platform.setTotalExpense(defaultZero(platform.getTotalExpense()).add(amount).setScale(2, RoundingMode.HALF_UP));
        platform.setUpdatedTime(now);
        if (!accountBalancesService.updateById(platform)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "取消后的托管金额结算失败");
        }

        FundFlows flow = new FundFlows();
        flow.setId(SnowflakeIdUtil.nextFundFlowId());
        flow.setAccountId(PLATFORM_ACCOUNT_ID);
        flow.setAccountType(ACCOUNT_TYPE_PLATFORM);
        flow.setFlowType(FLOW_TYPE_EXPENSE);
        flow.setAmount(amount);
        flow.setBalanceBefore(balanceBefore);
        flow.setBalanceAfter(balanceAfter);
        flow.setBusinessType(BT_PLATFORM_ESCROW_OUT_CANCEL_SETTLE);
        flow.setBusinessId(orderId);
        flow.setDescription("维修订单取消后的上门费结算出账");
        flow.setRemark("orderNo=" + orderNo);
        flow.setCreatedTime(now);
        flow.setIsDelete(0);
        if (!fundFlowsService.save(flow)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建资金流水失败");
        }
    }

    private void platformEscrowOutRefund(String orderId, String orderNo, BigDecimal amount, String reason, long now) {
        // idempotency: platform refund flow
        FundFlows existing = fundFlowsService.getOne(
            new LambdaQueryWrapper<FundFlows>()
                .eq(FundFlows::getAccountId, PLATFORM_ACCOUNT_ID)
                .eq(FundFlows::getAccountType, ACCOUNT_TYPE_PLATFORM)
                .eq(FundFlows::getBusinessType, BT_PLATFORM_ESCROW_OUT_REFUND)
                .eq(FundFlows::getBusinessId, orderId)
                .eq(FundFlows::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (existing != null) {
            return;
        }

        AccountBalances platform = requireAccountBalance(PLATFORM_ACCOUNT_ID, ACCOUNT_TYPE_PLATFORM);
        BigDecimal balanceBefore = defaultZero(platform.getBalance()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal balanceAfter = balanceBefore;

        BigDecimal frozenAfter = defaultZero(platform.getFrozenBalance()).subtract(amount);
        if (frozenAfter.compareTo(ZERO) < 0) {
            frozenAfter = ZERO;
        }
        platform.setFrozenBalance(frozenAfter.setScale(2, RoundingMode.HALF_UP));
        platform.setTotalExpense(defaultZero(platform.getTotalExpense()).add(amount).setScale(2, RoundingMode.HALF_UP));
        platform.setUpdatedTime(now);
        if (!accountBalancesService.updateById(platform)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新平台退款出账失败");
        }

        FundFlows flow = new FundFlows();
        flow.setId(SnowflakeIdUtil.nextFundFlowId());
        flow.setAccountId(PLATFORM_ACCOUNT_ID);
        flow.setAccountType(ACCOUNT_TYPE_PLATFORM);
        flow.setFlowType(FLOW_TYPE_EXPENSE);
        flow.setAmount(amount);
        flow.setBalanceBefore(balanceBefore);
        flow.setBalanceAfter(balanceAfter);
        flow.setBusinessType(BT_PLATFORM_ESCROW_OUT_REFUND);
        flow.setBusinessId(orderId);
        flow.setDescription("维修订单退款出账（退给用户）");
        flow.setRemark("orderNo=" + orderNo + (StringUtils.hasText(reason) ? (",reason=" + reason) : ""));
        flow.setCreatedTime(now);
        flow.setIsDelete(0);
        if (!fundFlowsService.save(flow)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建资金流水失败");
        }
    }

    private void ensureAccountBalance(String accountId, int accountType, long now) {
        AccountBalances existing = accountBalancesService.getOne(
            new LambdaQueryWrapper<AccountBalances>()
                .eq(AccountBalances::getAccountId, accountId)
                .eq(AccountBalances::getAccountType, accountType)
                .eq(AccountBalances::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (existing != null) {
            return;
        }
        AccountBalances balance = new AccountBalances();
        balance.setId(SnowflakeIdUtil.nextAccountBalanceId());
        balance.setAccountId(accountId);
        balance.setAccountType(accountType);
        balance.setBalance(ZERO.setScale(2, RoundingMode.HALF_UP));
        balance.setFrozenBalance(ZERO.setScale(2, RoundingMode.HALF_UP));
        balance.setTotalIncome(ZERO.setScale(2, RoundingMode.HALF_UP));
        balance.setTotalExpense(ZERO.setScale(2, RoundingMode.HALF_UP));
        balance.setCreatedTime(now);
        balance.setUpdatedTime(now);
        balance.setIsDelete(0);
        if (!accountBalancesService.save(balance)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化账户余额失败");
        }
    }

    private AccountBalances requireAccountBalance(String accountId, int accountType) {
        AccountBalances balance = accountBalancesService.getOne(
            new LambdaQueryWrapper<AccountBalances>()
                .eq(AccountBalances::getAccountId, accountId)
                .eq(AccountBalances::getAccountType, accountType)
                .eq(AccountBalances::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (balance == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "账户余额不存在");
        }
        return balance;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        return defaultZero(value).setScale(2, RoundingMode.HALF_UP);
    }

    private long getAfterSalesProtectionDays() {
        Long value = systemConfigsService.getLongConfig("after_sales.valid_days", DEFAULT_AFTER_SALES_PROTECTION_DAYS);
        return value == null || value <= 0L ? DEFAULT_AFTER_SALES_PROTECTION_DAYS : value;
    }

    private long getAfterSalesProtectionPeriodMillis() {
        return getAfterSalesProtectionDays() * 24L * 60L * 60L * 1000L;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
