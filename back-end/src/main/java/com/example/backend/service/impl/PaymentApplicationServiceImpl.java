package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.PaymentRecords;
import com.example.backend.entity.ProductOrders;
import com.example.backend.entity.RechargeOrders;
import com.example.backend.entity.RepairOrderPayments;
import com.example.backend.entity.RepairOrders;
import com.example.backend.entity.UserAccounts;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.payment.CreatePaymentIntentRequest;
import com.example.backend.model.payment.PaymentIntentResponse;
import com.example.backend.payment.ChannelPaymentStatus;
import com.example.backend.payment.PaymentGateway;
import com.example.backend.payment.PaymentPrepayRequest;
import com.example.backend.payment.PaymentQueryResult;
import com.example.backend.payment.VerifiedPaymentCallback;
import com.example.backend.service.BusinessMetrics;
import com.example.backend.service.PaymentApplicationService;
import com.example.backend.service.PaymentPostingService;
import com.example.backend.service.PaymentRecordsService;
import com.example.backend.service.ProductOrdersService;
import com.example.backend.service.RechargeOrdersService;
import com.example.backend.service.RepairOrderPaymentsService;
import com.example.backend.service.RepairOrdersService;
import com.example.backend.service.UserAccountsService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PaymentApplicationServiceImpl implements PaymentApplicationService {
    private static final Logger log = LoggerFactory.getLogger(PaymentApplicationServiceImpl.class);
    static final int ORDER_TYPE_REPAIR = 1;
    static final int ORDER_TYPE_PRODUCT = 2;
    static final int ORDER_TYPE_RECHARGE = 3;
    static final int BIZ_STAGE_PREPAY = 1;
    static final int BIZ_STAGE_TAIL = 2;
    static final int BIZ_STAGE_PRODUCT = 3;
    static final int PAYMENT_STATUS_PENDING = 1;
    static final int PAYMENT_STATUS_PAID = 2;
    static final int STATUS_PENDING = 1;
    static final int STATUS_PROCESSING = 2;
    static final int STATUS_SUCCESS = 3;
    static final int STATUS_FAILED = 4;
    static final int STATUS_CLOSED = 6;
    static final long INTENT_TTL_MILLIS = 15 * 60 * 1000L;
    private static final String CURRENCY_CNY = "CNY";

    private final PaymentRecordsService paymentRecordsService;
    private final RechargeOrdersService rechargeOrdersService;
    private final UserAccountsService userAccountsService;
    private final PaymentPostingService paymentPostingService;
    private final RepairOrdersService repairOrdersService;
    private final RepairOrderPaymentsService repairOrderPaymentsService;
    private final ProductOrdersService productOrdersService;
    private final BusinessMetrics businessMetrics;
    private final Map<Integer, PaymentGateway> gateways;

    public PaymentApplicationServiceImpl(
            PaymentRecordsService paymentRecordsService,
            RechargeOrdersService rechargeOrdersService,
            UserAccountsService userAccountsService,
            PaymentPostingService paymentPostingService,
            RepairOrdersService repairOrdersService,
            RepairOrderPaymentsService repairOrderPaymentsService,
            ProductOrdersService productOrdersService,
            BusinessMetrics businessMetrics,
            List<PaymentGateway> gateways) {
        this.paymentRecordsService = paymentRecordsService;
        this.rechargeOrdersService = rechargeOrdersService;
        this.userAccountsService = userAccountsService;
        this.paymentPostingService = paymentPostingService;
        this.repairOrdersService = repairOrdersService;
        this.repairOrderPaymentsService = repairOrderPaymentsService;
        this.productOrdersService = productOrdersService;
        this.businessMetrics = businessMetrics;
        this.gateways = new HashMap<>();
        for (PaymentGateway gateway : gateways) {
            PaymentGateway previous = this.gateways.put(gateway.provider(), gateway);
            if (previous != null) {
                throw new IllegalStateException("支付渠道重复注册: " + gateway.provider());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentIntentResponse createIntent(
            String accountId, CreatePaymentIntentRequest request) {
        requireAccountId(accountId);
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "支付参数不能为空");
        }
        if (Integer.valueOf(ORDER_TYPE_REPAIR).equals(request.getOrderType())) {
            return createRepairOrderIntent(accountId, request);
        }
        if (Integer.valueOf(ORDER_TYPE_PRODUCT).equals(request.getOrderType())) {
            return createProductOrderIntent(accountId, request);
        }
        if (!Integer.valueOf(ORDER_TYPE_RECHARGE).equals(request.getOrderType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前仅支持维修/商品订单与钱包充值支付意图");
        }
        businessMetrics.paymentIntentCreated(ORDER_TYPE_RECHARGE);
        BigDecimal amount = normalizeAmount(request.getAmount());
        PaymentGateway gateway = requireAvailableGateway(request.getProvider());
        UserAccounts account = userAccountsService.getById(accountId);
        if (account == null || !StringUtils.hasText(account.getWxOpenid())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前账号未绑定微信，无法发起微信支付");
        }
        long now = System.currentTimeMillis();
        long expiresAt = now + INTENT_TTL_MILLIS;

        RechargeOrders recharge = new RechargeOrders();
        recharge.setId(SnowflakeIdUtil.nextRechargeOrderId());
        recharge.setRechargeNo("RCG" + recharge.getId().substring(2));
        recharge.setAccountId(accountId);
        recharge.setAmount(amount);
        recharge.setCurrency(CURRENCY_CNY);
        recharge.setProvider(request.getProvider());
        recharge.setStatus(STATUS_PENDING);
        recharge.setExpiresAt(expiresAt);
        recharge.setCreatedTime(now);
        recharge.setUpdatedTime(now);
        recharge.setVersion(0);
        recharge.setIsDelete(0);
        if (!rechargeOrdersService.save(recharge)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建充值订单失败");
        }

        PaymentRecords payment = new PaymentRecords();
        payment.setId(SnowflakeIdUtil.nextPaymentRecordId());
        payment.setPaymentNo("PAY" + payment.getId().substring(2));
        payment.setOrderId(recharge.getId());
        payment.setOrderType(ORDER_TYPE_RECHARGE);
        payment.setAccountId(accountId);
        payment.setPaymentMethod(request.getProvider());
        payment.setPaymentAmount(amount);
        payment.setCurrency(CURRENCY_CNY);
        payment.setPaymentStatus(STATUS_PENDING);
        payment.setExpiresAt(expiresAt);
        payment.setRefundAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        payment.setRemark("钱包充值支付意图");
        payment.setCreatedTime(now);
        payment.setUpdatedTime(now);
        payment.setVersion(0);
        payment.setIsDelete(0);
        if (!paymentRecordsService.save(payment)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建支付单失败");
        }

        Map<String, String> invokeParameters =
                gateway.prepay(
                        new PaymentPrepayRequest(
                                payment.getPaymentNo(),
                                amount,
                                CURRENCY_CNY,
                                "钱包充值",
                                expiresAt,
                                account.getWxOpenid()));
        payment.setPaymentStatus(STATUS_PROCESSING);
        payment.setUpdatedTime(System.currentTimeMillis());
        recharge.setStatus(STATUS_PROCESSING);
        recharge.setUpdatedTime(payment.getUpdatedTime());
        if (!paymentRecordsService.updateById(payment)
                || !rechargeOrdersService.updateById(recharge)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新支付单状态失败");
        }
        return toResponse(payment, invokeParameters);
    }

    private PaymentIntentResponse createRepairOrderIntent(
            String accountId, CreatePaymentIntentRequest request) {
        if (!StringUtils.hasText(request.getOrderId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单ID不能为空");
        }
        businessMetrics.paymentIntentCreated(ORDER_TYPE_REPAIR);
        RepairOrders order =
                repairOrdersService.getOne(
                        new LambdaQueryWrapper<RepairOrders>()
                                .eq(RepairOrders::getId, request.getOrderId().trim())
                                .eq(RepairOrders::getIsDelete, 0)
                                .last("limit 1"),
                        false);
        if (order == null || !accountId.equals(order.getAccountId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "维修订单不存在");
        }
        RepairOrderPayments orderPayment =
                repairOrderPaymentsService.getOne(
                        new LambdaQueryWrapper<RepairOrderPayments>()
                                .eq(RepairOrderPayments::getRepairOrderId, order.getId())
                                .eq(RepairOrderPayments::getIsDelete, 0)
                                .last("limit 1"),
                        false);
        if (orderPayment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单支付信息不存在");
        }

        int stage;
        BigDecimal amount;
        if (order.getPaymentStatus() != null && order.getPaymentStatus() == PAYMENT_STATUS_PAID) {
            stage = BIZ_STAGE_TAIL;
            BigDecimal remaining =
                    normalizeMoney(orderPayment.getTotalAmount())
                            .subtract(normalizeMoney(orderPayment.getActualAmount()));
            if (remaining.compareTo(new BigDecimal("0.01")) < 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前订单暂无需要支付的尾款");
            }
            amount = normalizeAmount(remaining);
        } else {
            stage = BIZ_STAGE_PREPAY;
            amount = normalizeAmount(orderPayment.getTotalAmount());
            if (amount.compareTo(new BigDecimal("0.01")) < 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前订单无需预付");
            }
        }

        PaymentGateway gateway = requireAvailableGateway(request.getProvider());
        UserAccounts account = userAccountsService.getById(accountId);
        if (account == null || !StringUtils.hasText(account.getWxOpenid())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前账号未绑定微信，无法发起微信支付");
        }

        String description = stage == BIZ_STAGE_PREPAY ? "维修订单预付款" : "维修订单尾款";
        long now = System.currentTimeMillis();
        long expiresAt = now + INTENT_TTL_MILLIS;
        PaymentRecords payment = new PaymentRecords();
        payment.setId(SnowflakeIdUtil.nextPaymentRecordId());
        payment.setPaymentNo("PAY" + payment.getId().substring(2));
        payment.setOrderId(order.getId());
        payment.setOrderType(ORDER_TYPE_REPAIR);
        payment.setBizStage(stage);
        payment.setAccountId(accountId);
        payment.setPaymentMethod(request.getProvider());
        payment.setPaymentAmount(amount);
        payment.setCurrency(CURRENCY_CNY);
        payment.setPaymentStatus(STATUS_PENDING);
        payment.setExpiresAt(expiresAt);
        payment.setRefundAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        payment.setRemark(description);
        payment.setCreatedTime(now);
        payment.setUpdatedTime(now);
        payment.setVersion(0);
        payment.setIsDelete(0);
        if (!paymentRecordsService.save(payment)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建支付单失败");
        }

        Map<String, String> invokeParameters =
                gateway.prepay(
                        new PaymentPrepayRequest(
                                payment.getPaymentNo(),
                                amount,
                                CURRENCY_CNY,
                                description,
                                expiresAt,
                                account.getWxOpenid()));
        payment.setPaymentStatus(STATUS_PROCESSING);
        payment.setUpdatedTime(System.currentTimeMillis());
        if (!paymentRecordsService.updateById(payment)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新支付单状态失败");
        }
        return toResponse(payment, invokeParameters);
    }

    private PaymentIntentResponse createProductOrderIntent(
            String accountId, CreatePaymentIntentRequest request) {
        if (!StringUtils.hasText(request.getOrderId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单ID不能为空");
        }
        businessMetrics.paymentIntentCreated(ORDER_TYPE_PRODUCT);
        ProductOrders order =
                productOrdersService.getOne(
                        new LambdaQueryWrapper<ProductOrders>()
                                .eq(ProductOrders::getId, request.getOrderId().trim())
                                .eq(ProductOrders::getIsDelete, 0)
                                .last("limit 1"),
                        false);
        if (order == null || !accountId.equals(order.getAccountId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品订单不存在");
        }
        if (order.getPaymentStatus() == null
                || order.getPaymentStatus() != PAYMENT_STATUS_PENDING) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "订单不处于待支付状态");
        }
        BigDecimal amount = normalizeAmount(order.getActualAmount());
        PaymentGateway gateway = requireAvailableGateway(request.getProvider());
        UserAccounts account = userAccountsService.getById(accountId);
        if (account == null || !StringUtils.hasText(account.getWxOpenid())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前账号未绑定微信，无法发起微信支付");
        }

        long now = System.currentTimeMillis();
        long expiresAt = now + INTENT_TTL_MILLIS;
        PaymentRecords payment = new PaymentRecords();
        payment.setId(SnowflakeIdUtil.nextPaymentRecordId());
        payment.setPaymentNo("PAY" + payment.getId().substring(2));
        payment.setOrderId(order.getId());
        payment.setOrderType(ORDER_TYPE_PRODUCT);
        payment.setBizStage(BIZ_STAGE_PRODUCT);
        payment.setAccountId(accountId);
        payment.setPaymentMethod(request.getProvider());
        payment.setPaymentAmount(amount);
        payment.setCurrency(CURRENCY_CNY);
        payment.setPaymentStatus(STATUS_PENDING);
        payment.setExpiresAt(expiresAt);
        payment.setRefundAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        payment.setRemark("商品订单支付");
        payment.setCreatedTime(now);
        payment.setUpdatedTime(now);
        payment.setVersion(0);
        payment.setIsDelete(0);
        if (!paymentRecordsService.save(payment)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建支付单失败");
        }

        Map<String, String> invokeParameters =
                gateway.prepay(
                        new PaymentPrepayRequest(
                                payment.getPaymentNo(),
                                amount,
                                CURRENCY_CNY,
                                "商品订单支付",
                                expiresAt,
                                account.getWxOpenid()));
        payment.setPaymentStatus(STATUS_PROCESSING);
        payment.setUpdatedTime(System.currentTimeMillis());
        if (!paymentRecordsService.updateById(payment)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新支付单状态失败");
        }
        return toResponse(payment, invokeParameters);
    }

    @Override
    public PaymentIntentResponse getIntent(String accountId, String paymentNo) {
        requireAccountId(accountId);
        if (!StringUtils.hasText(paymentNo)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "支付单号不能为空");
        }
        PaymentRecords payment =
                paymentRecordsService.getOne(
                        new LambdaQueryWrapper<PaymentRecords>()
                                .eq(PaymentRecords::getPaymentNo, paymentNo.trim())
                                .eq(PaymentRecords::getAccountId, accountId)
                                .eq(PaymentRecords::getIsDelete, 0)
                                .last("limit 1"),
                        false);
        if (payment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "支付单不存在");
        }
        payment = refreshChannelStatus(payment);
        return toResponse(payment, Map.of());
    }

    private PaymentRecords refreshChannelStatus(PaymentRecords payment) {
        if (!Integer.valueOf(STATUS_PROCESSING).equals(payment.getPaymentStatus())) return payment;
        PaymentGateway gateway = gateways.get(payment.getPaymentMethod());
        if (gateway == null || !gateway.isAvailable()) return payment;
        PaymentQueryResult result;
        try {
            result = gateway.query(payment.getPaymentNo());
        } catch (BusinessException ex) {
            log.warn(
                    "刷新支付渠道状态失败: paymentNo={}, message={}",
                    payment.getPaymentNo(),
                    ex.getMessage());
            return payment;
        }
        validateQueryResult(payment, result);
        if (result.status() == ChannelPaymentStatus.SUCCESS) {
            paymentPostingService.postVerifiedPayment(
                    payment.getPaymentMethod(),
                    new VerifiedPaymentCallback(
                            result.paymentNo(),
                            result.providerTransactionNo(),
                            result.amount(),
                            result.currency(),
                            true,
                            result.verificationDigest()));
            PaymentRecords refreshed = paymentRecordsService.getById(payment.getId());
            return refreshed == null ? payment : refreshed;
        }
        if (result.status() == ChannelPaymentStatus.CLOSED) {
            return closeLocalPayment(payment, STATUS_CLOSED);
        }
        if (result.status() == ChannelPaymentStatus.FAILED) {
            return closeLocalPayment(payment, STATUS_FAILED);
        }
        if (payment.getExpiresAt() != null
                && payment.getExpiresAt() <= System.currentTimeMillis()) {
            try {
                gateway.close(payment.getPaymentNo());
            } catch (BusinessException ex) {
                log.warn(
                        "关闭过期支付单失败: paymentNo={}, message={}",
                        payment.getPaymentNo(),
                        ex.getMessage());
                return payment;
            }
            return closeLocalPayment(payment, STATUS_CLOSED);
        }
        return payment;
    }

    private PaymentRecords closeLocalPayment(PaymentRecords payment, int status) {
        long now = System.currentTimeMillis();
        payment.setPaymentStatus(status);
        payment.setUpdatedTime(now);
        if (!paymentRecordsService.updateById(payment)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新支付单状态失败");
        }
        if (Integer.valueOf(ORDER_TYPE_RECHARGE).equals(payment.getOrderType())) {
            RechargeOrders recharge = rechargeOrdersService.getById(payment.getOrderId());
            if (recharge != null) {
                recharge.setStatus(status);
                recharge.setUpdatedTime(now);
                if (!rechargeOrdersService.updateById(recharge)) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新充值订单状态失败");
                }
            }
        }
        return payment;
    }

    private void validateQueryResult(PaymentRecords payment, PaymentQueryResult result) {
        if (result == null
                || !payment.getPaymentNo().equals(result.paymentNo())
                || normalizeAmount(payment.getPaymentAmount()).compareTo(result.amount()) != 0
                || !payment.getCurrency().equalsIgnoreCase(result.currency())) {
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR, "渠道查单结果与支付单不匹配");
        }
    }

    private PaymentGateway requireAvailableGateway(Integer provider) {
        PaymentGateway gateway = provider == null ? null : gateways.get(provider);
        if (gateway == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的支付渠道");
        }
        if (!gateway.isAvailable()) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR, provider == 1 ? "微信支付渠道尚未配置，暂不可用" : "当前客户端不支持支付宝支付");
        }
        return gateway;
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "支付金额不能为空");
        }
        BigDecimal value = amount.setScale(2, RoundingMode.HALF_UP);
        if (value.compareTo(new BigDecimal("0.01")) < 0
                || value.compareTo(new BigDecimal("50000.00")) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "支付金额需在0.01至50000元之间");
        }
        return value;
    }

    private void requireAccountId(String accountId) {
        if (!StringUtils.hasText(accountId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
    }

    private PaymentIntentResponse toResponse(
            PaymentRecords payment, Map<String, String> invokeParameters) {
        PaymentIntentResponse response = new PaymentIntentResponse();
        response.setPaymentNo(payment.getPaymentNo());
        response.setOrderId(payment.getOrderId());
        response.setOrderType(payment.getOrderType());
        response.setProvider(payment.getPaymentMethod());
        response.setStatus(payment.getPaymentStatus());
        response.setStatusText(statusText(payment.getPaymentStatus()));
        response.setAmount(
                payment.getPaymentAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        response.setCurrency(payment.getCurrency());
        response.setExpiresAt(payment.getExpiresAt());
        response.setInvokeParameters(
                invokeParameters == null ? Map.of() : Map.copyOf(invokeParameters));
        return response;
    }

    private String statusText(Integer status) {
        if (Integer.valueOf(STATUS_PROCESSING).equals(status)) return "支付中";
        if (Integer.valueOf(STATUS_SUCCESS).equals(status)) return "支付成功";
        if (Integer.valueOf(STATUS_FAILED).equals(status)) return "支付失败";
        if (Integer.valueOf(STATUS_CLOSED).equals(status)) return "已关闭";
        return "待支付";
    }
}
