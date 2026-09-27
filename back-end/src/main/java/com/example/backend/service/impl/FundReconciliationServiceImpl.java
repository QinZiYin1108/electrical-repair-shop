package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.AccountBalances;
import com.example.backend.entity.FundFlows;
import com.example.backend.entity.PaymentRecords;
import com.example.backend.entity.ReconciliationBatches;
import com.example.backend.entity.ReconciliationIssues;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.finance.FundReconciliationReport;
import com.example.backend.service.AccountBalancesService;
import com.example.backend.service.BusinessMetrics;
import com.example.backend.service.FundFlowsService;
import com.example.backend.service.FundReconciliationService;
import com.example.backend.service.PaymentRecordsService;
import com.example.backend.service.ReconciliationBatchesService;
import com.example.backend.service.ReconciliationIssuesService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class FundReconciliationServiceImpl implements FundReconciliationService {

    private static final int PAYMENT_SUCCESS = 3;
    private static final int PAYMENT_REFUNDED = 5;
    private static final int REPAIR_ORDER = 1;
    private static final int BATCH_STATUS_OK = 1;
    private static final int BATCH_STATUS_ISSUES = 2;
    private static final int ISSUE_STATUS_PENDING = 1;

    private final AccountBalancesService accountBalancesService;
    private final FundFlowsService fundFlowsService;
    private final PaymentRecordsService paymentRecordsService;
    private final ReconciliationBatchesService reconciliationBatchesService;
    private final ReconciliationIssuesService reconciliationIssuesService;
    private final BusinessMetrics businessMetrics;

    public FundReconciliationServiceImpl(
            AccountBalancesService accountBalancesService,
            FundFlowsService fundFlowsService,
            PaymentRecordsService paymentRecordsService,
            ReconciliationBatchesService reconciliationBatchesService,
            ReconciliationIssuesService reconciliationIssuesService,
            BusinessMetrics businessMetrics) {
        this.accountBalancesService = accountBalancesService;
        this.fundFlowsService = fundFlowsService;
        this.paymentRecordsService = paymentRecordsService;
        this.reconciliationBatchesService = reconciliationBatchesService;
        this.reconciliationIssuesService = reconciliationIssuesService;
        this.businessMetrics = businessMetrics;
    }

    @Override
    public FundReconciliationReport reconcile(long since, long now) {
        List<FoundIssue> issues = collectIssues(since);
        return new FundReconciliationReport(
                now, issues.stream().map(FoundIssue::message).collect(Collectors.toList()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationBatches runBatch(int batchType, long since, long now, String triggeredBy) {
        List<FoundIssue> issues = collectIssues(since);
        businessMetrics.reconciliationIssue(issues.size());
        ReconciliationBatches batch = new ReconciliationBatches();
        batch.setId(SnowflakeIdUtil.nextReconciliationBatchId());
        batch.setBatchNo("RB" + batch.getId().substring(2));
        batch.setBatchType(batchType);
        batch.setStatus(issues.isEmpty() ? BATCH_STATUS_OK : BATCH_STATUS_ISSUES);
        batch.setWindowStart(since);
        batch.setWindowEnd(now);
        batch.setScannedCount(countScanned(since));
        batch.setIssueCount(issues.size());
        batch.setErrorSummary(issues.isEmpty() ? null : summarize(issues));
        batch.setCreatedTime(now);
        batch.setUpdatedTime(now);
        batch.setVersion(0);
        batch.setIsDelete(0);
        if (!reconciliationBatchesService.save(batch)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存对账批次失败");
        }
        if (!issues.isEmpty()) {
            List<ReconciliationIssues> rows = new ArrayList<>();
            for (FoundIssue issue : issues) {
                ReconciliationIssues row = new ReconciliationIssues();
                row.setId(SnowflakeIdUtil.nextReconciliationIssueId());
                row.setBatchId(batch.getId());
                row.setCategory(issue.category());
                row.setBizType(issue.bizType());
                row.setBizId(issue.bizId());
                row.setMessage(issue.message());
                row.setStatus(ISSUE_STATUS_PENDING);
                row.setCreatedTime(now);
                row.setUpdatedTime(now);
                row.setVersion(0);
                row.setIsDelete(0);
                rows.add(row);
            }
            if (!reconciliationIssuesService.saveBatch(rows)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存对账异常失败");
            }
        }
        return batch;
    }

    @Override
    public Long lastBatchWindowEnd() {
        ReconciliationBatches last =
                reconciliationBatchesService.getOne(
                        new LambdaQueryWrapper<ReconciliationBatches>()
                                .eq(ReconciliationBatches::getIsDelete, 0)
                                .orderByDesc(ReconciliationBatches::getWindowEnd)
                                .last("limit 1"),
                        false);
        return last == null ? null : last.getWindowEnd();
    }

    private List<FoundIssue> collectIssues(long since) {
        List<FoundIssue> issues = new ArrayList<>();
        checkBalances(issues);
        List<FundFlows> flows =
                fundFlowsService.list(
                        new LambdaQueryWrapper<FundFlows>()
                                .eq(FundFlows::getIsDelete, 0)
                                .ge(FundFlows::getCreatedTime, since));
        checkIdempotencyKeys(flows, issues);
        checkRepairPayments(flows, since, issues);
        checkRefundBounds(since, issues);
        return issues;
    }

    private int countScanned(long since) {
        return (int)
                fundFlowsService.count(
                        new LambdaQueryWrapper<FundFlows>()
                                .eq(FundFlows::getIsDelete, 0)
                                .ge(FundFlows::getCreatedTime, since));
    }

    private void checkBalances(List<FoundIssue> issues) {
        for (AccountBalances balance :
                accountBalancesService.list(
                        new LambdaQueryWrapper<AccountBalances>()
                                .eq(AccountBalances::getIsDelete, 0))) {
            if (isNegative(balance.getBalance()) || isNegative(balance.getFrozenBalance())) {
                issues.add(
                        new FoundIssue(
                                "BALANCE_NEGATIVE",
                                "ACCOUNT",
                                balance.getAccountId(),
                                "账户余额为负: accountId=" + balance.getAccountId()));
            }
        }
    }

    private void checkIdempotencyKeys(List<FundFlows> flows, List<FoundIssue> issues) {
        Set<String> seen = new HashSet<>();
        for (FundFlows flow : flows) {
            String key = flow.getIdempotencyKey();
            if (!StringUtils.hasText(key)) {
                issues.add(
                        new FoundIssue(
                                "FLOW_MISSING_IDEMPOTENCY_KEY",
                                "FUND_FLOW",
                                flow.getId(),
                                "资金流水缺少幂等键: flowId=" + flow.getId()));
            } else if (!seen.add(key)) {
                issues.add(
                        new FoundIssue(
                                "FLOW_DUPLICATE_IDEMPOTENCY_KEY",
                                "FUND_FLOW",
                                flow.getId(),
                                "资金流水幂等键重复: " + key));
            }
        }
    }

    private void checkRepairPayments(List<FundFlows> flows, long since, List<FoundIssue> issues) {
        Map<String, List<FundFlows>> flowsByOrder = new HashMap<>();
        for (FundFlows flow : flows) {
            if (StringUtils.hasText(flow.getBusinessId())) {
                flowsByOrder
                        .computeIfAbsent(flow.getBusinessId(), ignored -> new ArrayList<>())
                        .add(flow);
            }
        }
        for (PaymentRecords payment :
                paymentRecordsService.list(
                        new LambdaQueryWrapper<PaymentRecords>()
                                .eq(PaymentRecords::getOrderType, REPAIR_ORDER)
                                .eq(PaymentRecords::getIsDelete, 0)
                                .ge(PaymentRecords::getCreatedTime, since)
                                .in(
                                        PaymentRecords::getPaymentStatus,
                                        PAYMENT_SUCCESS,
                                        PAYMENT_REFUNDED))) {
            List<FundFlows> orderFlows = flowsByOrder.getOrDefault(payment.getOrderId(), List.of());
            if (orderFlows.isEmpty()) {
                issues.add(
                        new FoundIssue(
                                "REPAIR_PAYMENT_MISSING_FLOW",
                                "PAYMENT",
                                payment.getPaymentNo(),
                                "维修支付缺少资金流水: paymentNo=" + payment.getPaymentNo()));
                continue;
            }
            if (Integer.valueOf(PAYMENT_REFUNDED).equals(payment.getPaymentStatus())
                    && orderFlows.stream()
                            .noneMatch(
                                    flow ->
                                            flow.getBusinessType() != null
                                                    && flow.getBusinessType().contains("REFUND"))) {
                issues.add(
                        new FoundIssue(
                                "REPAIR_REFUND_MISSING_FLOW",
                                "PAYMENT",
                                payment.getPaymentNo(),
                                "维修退款缺少退款流水: paymentNo=" + payment.getPaymentNo()));
            }
        }
    }

    private void checkRefundBounds(long since, List<FoundIssue> issues) {
        for (PaymentRecords payment :
                paymentRecordsService.list(
                        new LambdaQueryWrapper<PaymentRecords>()
                                .eq(PaymentRecords::getIsDelete, 0)
                                .ge(PaymentRecords::getCreatedTime, since))) {
            BigDecimal refunded =
                    payment.getRefundAmount() == null ? BigDecimal.ZERO : payment.getRefundAmount();
            BigDecimal total =
                    payment.getPaymentAmount() == null
                            ? BigDecimal.ZERO
                            : payment.getPaymentAmount();
            if (refunded.compareTo(total) > 0) {
                issues.add(
                        new FoundIssue(
                                "REFUND_EXCEEDS_PAYMENT",
                                "PAYMENT",
                                payment.getPaymentNo(),
                                "退款累计超过支付金额: paymentNo=" + payment.getPaymentNo()));
            }
        }
    }

    private boolean isNegative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) < 0;
    }

    private String summarize(List<FoundIssue> issues) {
        return issues.stream().limit(5).map(FoundIssue::message).collect(Collectors.joining("; "));
    }

    private record FoundIssue(String category, String bizType, String bizId, String message) {}
}
