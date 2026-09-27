package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.AccountBalances;
import com.example.backend.entity.FundFlows;
import com.example.backend.entity.PaymentRecords;
import com.example.backend.entity.PaymentRefunds;
import com.example.backend.entity.RechargeOrders;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.PaymentRefundsMapper;
import com.example.backend.model.payment.PaymentRefundResponse;
import com.example.backend.model.payment.RefundApplyRequest;
import com.example.backend.payment.ChannelRefundStatus;
import com.example.backend.payment.PaymentGateway;
import com.example.backend.payment.RefundRequest;
import com.example.backend.payment.RefundResult;
import com.example.backend.payment.VerifiedRefundCallback;
import com.example.backend.service.AccountBalancesService;
import com.example.backend.service.BusinessMetrics;
import com.example.backend.service.FundFlowsService;
import com.example.backend.service.PaymentRecordsService;
import com.example.backend.service.PaymentRefundsService;
import com.example.backend.service.RechargeOrdersService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PaymentRefundsServiceImpl extends ServiceImpl<PaymentRefundsMapper, PaymentRefunds>
        implements PaymentRefundsService {
    static final int REFUND_STATUS_PROCESSING = 1;
    static final int REFUND_STATUS_SUCCESS = 2;
    static final int REFUND_STATUS_FAILED = 3;

    static final int PAYMENT_STATUS_SUCCESS = 3;
    static final int PAYMENT_STATUS_REFUNDED = 5;

    static final int ORDER_TYPE_REPAIR = 1;
    static final int ORDER_TYPE_PRODUCT = 2;
    static final int ORDER_TYPE_RECHARGE = 3;
    static final int RECHARGE_STATUS_REFUNDED = 6;

    private static final int ACCOUNT_TYPE_USER = 1;
    private static final int FLOW_TYPE_EXPENSE = 2;
    private static final int PAYMENT_METHOD_WALLET = 5;
    private static final String BUSINESS_TYPE_RECHARGE_REFUND = "USER_WALLET_RECHARGE_REFUND";
    private static final String CURRENCY_CNY = "CNY";

    private final PaymentRecordsService paymentRecordsService;
    private final RechargeOrdersService rechargeOrdersService;
    private final AccountBalancesService accountBalancesService;
    private final FundFlowsService fundFlowsService;
    private final BusinessMetrics businessMetrics;
    private final Map<Integer, PaymentGateway> gateways = new HashMap<>();

    public PaymentRefundsServiceImpl(
            PaymentRecordsService paymentRecordsService,
            RechargeOrdersService rechargeOrdersService,
            AccountBalancesService accountBalancesService,
            FundFlowsService fundFlowsService,
            BusinessMetrics businessMetrics,
            List<PaymentGateway> gateways) {
        this.paymentRecordsService = paymentRecordsService;
        this.rechargeOrdersService = rechargeOrdersService;
        this.accountBalancesService = accountBalancesService;
        this.fundFlowsService = fundFlowsService;
        this.businessMetrics = businessMetrics;
        for (PaymentGateway gateway : gateways) {
            if (this.gateways.put(gateway.provider(), gateway) != null) {
                throw new IllegalStateException("支付渠道重复注册: " + gateway.provider());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentRefundResponse applyRefund(
            String paymentNo, RefundApplyRequest request, String operatorAccountId) {
        if (!StringUtils.hasText(paymentNo)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "支付单号不能为空");
        }
        if (request == null || request.getAmount() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "退款金额不能为空");
        }
        BigDecimal amount = normalizeMoney(request.getAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "退款金额必须大于 0");
        }

        PaymentRecords payment =
                paymentRecordsService.getOne(
                        new LambdaQueryWrapper<PaymentRecords>()
                                .eq(PaymentRecords::getPaymentNo, paymentNo.trim())
                                .eq(PaymentRecords::getIsDelete, 0)
                                .last("limit 1 for update"),
                        false);
        if (payment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "支付单不存在");
        }

        String idempotencyKey = buildIdempotencyKey(paymentNo, amount, request);
        PaymentRefunds existing =
                getOne(
                        new LambdaQueryWrapper<PaymentRefunds>()
                                .eq(PaymentRefunds::getIdempotencyKey, idempotencyKey)
                                .eq(PaymentRefunds::getIsDelete, 0)
                                .last("limit 1"),
                        false);
        if (existing != null) {
            return toResponse(existing);
        }

        validateRefundable(payment, amount);
        PaymentGateway gateway = requireAvailableGateway(payment.getPaymentMethod());

        long now = System.currentTimeMillis();
        PaymentRefunds refund = new PaymentRefunds();
        refund.setId(SnowflakeIdUtil.nextPaymentRefundId());
        refund.setRefundNo("REF" + refund.getId().substring(2));
        refund.setPaymentId(payment.getId());
        refund.setPaymentNo(payment.getPaymentNo());
        refund.setAccountId(payment.getAccountId());
        refund.setRefundAmount(amount);
        refund.setCurrency(payment.getCurrency());
        refund.setRefundReason(request.getReason());
        refund.setRefundStatus(REFUND_STATUS_PROCESSING);
        refund.setProvider(payment.getPaymentMethod());
        refund.setIdempotencyKey(idempotencyKey);
        refund.setInitiatedTime(now);
        refund.setCreatedTime(now);
        refund.setUpdatedTime(now);
        refund.setVersion(0);
        refund.setIsDelete(0);
        try {
            if (!save(refund)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建退款单失败");
            }
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.DUPLICATE_KEY, "该退款请求已存在");
        }
        businessMetrics.refundInitiated();

        String reason = StringUtils.hasText(request.getReason()) ? request.getReason() : "用户申请退款";
        RefundResult result =
                gateway.refund(
                        new RefundRequest(
                                payment.getPaymentNo(),
                                refund.getRefundNo(),
                                payment.getThirdPartyNo(),
                                normalizeMoney(payment.getPaymentAmount()),
                                amount,
                                payment.getCurrency(),
                                reason));
        return handleChannelResult(refund, result);
    }

    private PaymentRefundResponse handleChannelResult(PaymentRefunds refund, RefundResult result) {
        if (result == null
                || !refund.getRefundNo().equals(result.refundNo())
                || result.refundAmount() == null
                || normalizeMoney(result.refundAmount())
                                .compareTo(normalizeMoney(refund.getRefundAmount()))
                        != 0
                || !refund.getCurrency().equalsIgnoreCase(result.currency())
                || result.status() == null) {
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR, "渠道退款结果与退款单不匹配");
        }
        if (result.status() == ChannelRefundStatus.SUCCESS) {
            postVerifiedRefund(
                    refund.getProvider(),
                    new VerifiedRefundCallback(
                            refund.getRefundNo(),
                            result.providerRefundNo(),
                            result.refundAmount(),
                            result.currency(),
                            ChannelRefundStatus.SUCCESS,
                            result.verificationDigest()));
            PaymentRefunds refreshed = getById(refund.getId());
            return toResponse(refreshed == null ? refund : refreshed);
        }
        long now = System.currentTimeMillis();
        if (result.status() == ChannelRefundStatus.CLOSED
                || result.status() == ChannelRefundStatus.ABNORMAL) {
            refund.setRefundStatus(REFUND_STATUS_FAILED);
        }
        refund.setProviderRefundNo(result.providerRefundNo());
        refund.setUpdatedTime(now);
        if (!updateById(refund)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新退款单状态失败");
        }
        return toResponse(refund);
    }

    @Override
    public List<PaymentRefundResponse> listRefunds(String paymentNo) {
        if (!StringUtils.hasText(paymentNo)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "支付单号不能为空");
        }
        return list(
                        new LambdaQueryWrapper<PaymentRefunds>()
                                .eq(PaymentRefunds::getPaymentNo, paymentNo.trim())
                                .eq(PaymentRefunds::getIsDelete, 0)
                                .orderByDesc(PaymentRefunds::getCreatedTime))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void postVerifiedRefund(int provider, VerifiedRefundCallback callback) {
        validateRefundCallback(callback);
        PaymentRefunds refund =
                getOne(
                        new LambdaQueryWrapper<PaymentRefunds>()
                                .eq(PaymentRefunds::getRefundNo, callback.refundNo())
                                .eq(PaymentRefunds::getIsDelete, 0)
                                .last("limit 1 for update"),
                        false);
        if (refund == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "退款单不存在");
        }
        if (!Integer.valueOf(provider).equals(refund.getProvider())) {
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR, "退款回调渠道不匹配");
        }
        if (Integer.valueOf(REFUND_STATUS_SUCCESS).equals(refund.getRefundStatus())) {
            if (callback.providerRefundNo() == null
                    || callback.providerRefundNo().equals(refund.getProviderRefundNo())) {
                return;
            }
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR, "退款单渠道退款号冲突");
        }
        if (normalizeMoney(refund.getRefundAmount())
                                .compareTo(normalizeMoney(callback.refundAmount()))
                        != 0
                || !refund.getCurrency().equalsIgnoreCase(callback.currency())) {
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR, "退款回调金额或币种不匹配");
        }

        long now = System.currentTimeMillis();
        if (callback.status() == ChannelRefundStatus.SUCCESS) {
            postRefundAccounting(refund, callback, now);
            return;
        }
        if (callback.status() == ChannelRefundStatus.PROCESSING) {
            refund.setProviderRefundNo(callback.providerRefundNo());
            refund.setUpdatedTime(now);
            if (!updateById(refund)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新退款单状态失败");
            }
            return;
        }
        // 退款关闭或异常：只做合法状态迁移，不记账。
        refund.setRefundStatus(REFUND_STATUS_FAILED);
        refund.setProviderRefundNo(callback.providerRefundNo());
        refund.setCallbackData(callback.callbackDigest());
        refund.setUpdatedTime(now);
        if (!updateById(refund)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新退款单状态失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int refundOrderChannelPayments(String orderId, String reason, String operatorAccountId) {
        if (!StringUtils.hasText(orderId)) {
            return 0;
        }
        List<PaymentRecords> payments =
                paymentRecordsService.list(
                        new LambdaQueryWrapper<PaymentRecords>()
                                .eq(PaymentRecords::getOrderId, orderId)
                                .eq(PaymentRecords::getIsDelete, 0)
                                .ne(PaymentRecords::getPaymentMethod, PAYMENT_METHOD_WALLET));
        int count = 0;
        for (PaymentRecords payment : payments) {
            Integer orderType = payment.getOrderType();
            if (!Integer.valueOf(ORDER_TYPE_REPAIR).equals(orderType)
                    && !Integer.valueOf(ORDER_TYPE_PRODUCT).equals(orderType)) {
                continue;
            }
            if (!Integer.valueOf(PAYMENT_STATUS_SUCCESS).equals(payment.getPaymentStatus())) {
                continue;
            }
            BigDecimal remaining =
                    normalizeMoney(payment.getPaymentAmount())
                            .subtract(normalizeMoney(payment.getRefundAmount()));
            if (remaining.compareTo(new BigDecimal("0.01")) < 0) {
                continue;
            }
            RefundApplyRequest request = new RefundApplyRequest();
            request.setAmount(remaining);
            request.setReason(reason);
            applyRefund(payment.getPaymentNo(), request, operatorAccountId);
            count++;
        }
        return count;
    }

    private void postRefundAccounting(
            PaymentRefunds refund, VerifiedRefundCallback callback, long now) {
        PaymentRecords payment =
                paymentRecordsService.getOne(
                        new LambdaQueryWrapper<PaymentRecords>()
                                .eq(PaymentRecords::getId, refund.getPaymentId())
                                .eq(PaymentRecords::getIsDelete, 0)
                                .last("limit 1 for update"),
                        false);
        if (payment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "原支付单不存在");
        }
        if (!Integer.valueOf(PAYMENT_STATUS_SUCCESS).equals(payment.getPaymentStatus())
                && !Integer.valueOf(PAYMENT_STATUS_REFUNDED).equals(payment.getPaymentStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "原支付单不是已支付成功状态，不能退款");
        }
        Integer orderType = payment.getOrderType();
        boolean rechargeRefund = Integer.valueOf(ORDER_TYPE_RECHARGE).equals(orderType);
        if (!rechargeRefund
                && !Integer.valueOf(ORDER_TYPE_REPAIR).equals(orderType)
                && !Integer.valueOf(ORDER_TYPE_PRODUCT).equals(orderType)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该订单类型尚未接入统一退款记账");
        }
        BigDecimal amount = normalizeMoney(callback.refundAmount());
        BigDecimal already = normalizeMoney(payment.getRefundAmount());
        BigDecimal total = normalizeMoney(payment.getPaymentAmount());
        if (already.add(amount).compareTo(total) > 0) {
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR, "退款累计金额超过原支付金额");
        }
        BigDecimal newRefunded = already.add(amount);
        boolean fullyRefunded = newRefunded.compareTo(total) >= 0;

        RechargeOrders recharge = null;
        if (rechargeRefund) {
            // 充值退款：充值的钱已进入用户余额，退款时从余额冲减。
            recharge =
                    rechargeOrdersService.getOne(
                            new LambdaQueryWrapper<RechargeOrders>()
                                    .eq(RechargeOrders::getId, payment.getOrderId())
                                    .eq(RechargeOrders::getIsDelete, 0)
                                    .last("limit 1 for update"),
                            false);
            if (recharge == null || !recharge.getAccountId().equals(payment.getAccountId())) {
                throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR, "充值订单与支付单不匹配");
            }

            AccountBalances balance = lockUserBalance(payment.getAccountId());
            BigDecimal before = normalizeMoney(balance.getBalance());
            if (before.compareTo(amount) < 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户当前余额不足以冲减本次退款，请先处理余额消费");
            }
            BigDecimal after = before.subtract(amount);
            balance.setBalance(after);
            balance.setTotalExpense(normalizeMoney(balance.getTotalExpense()).add(amount));
            balance.setUpdatedTime(now);
            if (!accountBalancesService.updateById(balance)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退款余额冲减失败");
            }

            FundFlows flow = new FundFlows();
            flow.setId(SnowflakeIdUtil.nextFundFlowId());
            flow.setAccountId(payment.getAccountId());
            flow.setAccountType(ACCOUNT_TYPE_USER);
            flow.setFlowType(FLOW_TYPE_EXPENSE);
            flow.setAmount(amount);
            flow.setBalanceBefore(before);
            flow.setBalanceAfter(after);
            flow.setBusinessType(BUSINESS_TYPE_RECHARGE_REFUND);
            flow.setBusinessId(refund.getId());
            flow.setIdempotencyKey("PAYMENT_REFUND:" + refund.getRefundNo());
            flow.setDescription("钱包充值退款");
            flow.setRemark(
                    "refundNo=" + refund.getRefundNo() + ",paymentNo=" + payment.getPaymentNo());
            flow.setCreatedTime(now);
            flow.setVersion(0);
            flow.setIsDelete(0);
            try {
                if (!fundFlowsService.save(flow)) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存退款流水失败");
                }
            } catch (DuplicateKeyException ex) {
                throw new BusinessException(ErrorCode.DUPLICATE_KEY, "退款记账已处理");
            }
        }
        // 订单退款（维修/商品）：渠道原路退回微信，不冲减用户余额；订单侧状态由售后链路处理。

        payment.setRefundAmount(newRefunded);
        payment.setRefundTime(now);
        payment.setRefundReason(refund.getRefundReason());
        if (fullyRefunded) {
            payment.setPaymentStatus(PAYMENT_STATUS_REFUNDED);
        }
        payment.setUpdatedTime(now);
        if (!paymentRecordsService.updateById(payment)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新支付单退款状态失败");
        }

        if (recharge != null && fullyRefunded) {
            recharge.setStatus(RECHARGE_STATUS_REFUNDED);
            recharge.setUpdatedTime(now);
            if (!rechargeOrdersService.updateById(recharge)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新充值订单退款状态失败");
            }
        }

        refund.setRefundStatus(REFUND_STATUS_SUCCESS);
        refund.setProviderRefundNo(callback.providerRefundNo());
        refund.setCallbackData(callback.callbackDigest());
        refund.setCompletedTime(now);
        refund.setUpdatedTime(now);
        if (!updateById(refund)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新退款单状态失败");
        }
        businessMetrics.refundSucceeded();
    }

    private AccountBalances lockUserBalance(String accountId) {
        AccountBalances balance =
                accountBalancesService.getOne(
                        new LambdaQueryWrapper<AccountBalances>()
                                .eq(AccountBalances::getAccountId, accountId)
                                .eq(AccountBalances::getAccountType, ACCOUNT_TYPE_USER)
                                .eq(AccountBalances::getIsDelete, 0)
                                .last("limit 1 for update"),
                        false);
        if (balance == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户余额账户不存在，无法退款");
        }
        return balance;
    }

    private void validateRefundable(PaymentRecords payment, BigDecimal amount) {
        if (!Integer.valueOf(PAYMENT_STATUS_SUCCESS).equals(payment.getPaymentStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "只有支付成功的支付单才能退款");
        }
        Integer orderType = payment.getOrderType();
        if (!Integer.valueOf(ORDER_TYPE_RECHARGE).equals(orderType)
                && !Integer.valueOf(ORDER_TYPE_REPAIR).equals(orderType)
                && !Integer.valueOf(ORDER_TYPE_PRODUCT).equals(orderType)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该订单类型尚未接入渠道退款");
        }
        if (!CURRENCY_CNY.equalsIgnoreCase(payment.getCurrency())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前仅支持人民币退款");
        }
        BigDecimal remaining =
                normalizeMoney(payment.getPaymentAmount())
                        .subtract(normalizeMoney(payment.getRefundAmount()));
        if (amount.compareTo(remaining) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "退款金额超过可退金额");
        }
    }

    private PaymentGateway requireAvailableGateway(Integer provider) {
        PaymentGateway gateway = provider == null ? null : gateways.get(provider);
        if (gateway == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的支付渠道");
        }
        if (!gateway.isAvailable()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "支付渠道尚未配置，暂不可用，不能发起退款");
        }
        return gateway;
    }

    private String buildIdempotencyKey(
            String paymentNo, BigDecimal amount, RefundApplyRequest request) {
        if (StringUtils.hasText(request.getIdempotencyKey())) {
            return "REFUND:" + paymentNo.trim() + ":" + request.getIdempotencyKey().trim();
        }
        return "REFUND:" + paymentNo.trim() + ":" + amount.toPlainString();
    }

    private void validateRefundCallback(VerifiedRefundCallback callback) {
        if (callback == null
                || !StringUtils.hasText(callback.refundNo())
                || callback.refundAmount() == null
                || callback.refundAmount().compareTo(BigDecimal.ZERO) <= 0
                || !StringUtils.hasText(callback.currency())
                || callback.status() == null) {
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR, "退款回调字段不完整");
        }
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    private PaymentRefundResponse toResponse(PaymentRefunds refund) {
        PaymentRefundResponse response = new PaymentRefundResponse();
        response.setRefundNo(refund.getRefundNo());
        response.setPaymentNo(refund.getPaymentNo());
        response.setAccountId(refund.getAccountId());
        response.setAmount(normalizeMoney(refund.getRefundAmount()).toPlainString());
        response.setCurrency(refund.getCurrency());
        response.setStatus(refund.getRefundStatus());
        response.setStatusText(statusText(refund.getRefundStatus()));
        response.setProvider(refund.getProvider());
        response.setProviderRefundNo(refund.getProviderRefundNo());
        response.setReason(refund.getRefundReason());
        response.setInitiatedTime(refund.getInitiatedTime());
        response.setCompletedTime(refund.getCompletedTime());
        return response;
    }

    private String statusText(Integer status) {
        if (Integer.valueOf(REFUND_STATUS_PROCESSING).equals(status)) return "退款中";
        if (Integer.valueOf(REFUND_STATUS_SUCCESS).equals(status)) return "退款成功";
        if (Integer.valueOf(REFUND_STATUS_FAILED).equals(status)) return "退款失败";
        return "未知状态";
    }
}
