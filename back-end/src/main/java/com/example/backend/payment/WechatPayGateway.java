package com.example.backend.payment;

import com.example.backend.common.ErrorCode;
import com.example.backend.exception.BusinessException;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.RSAPublicKeyConfig;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.CloseOrderRequest;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.jsapi.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.QueryByOutRefundNoRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.RefundNotification;
import com.wechat.pay.java.service.refund.model.Status;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "payment.wechat.enabled", havingValue = "true")
public class WechatPayGateway implements PaymentGateway {
    private static final int PROVIDER_WECHAT = 1;
    private static final Logger log = LoggerFactory.getLogger(WechatPayGateway.class);

    private final WechatPayProperties properties;
    private final boolean ready;
    private final JsapiServiceExtension jsapiService;
    private final NotificationParser notificationParser;
    private final RefundService refundService;

    public WechatPayGateway(WechatPayProperties properties) {
        this.properties = properties;
        validateConfiguration(properties);
        Config config = null;
        boolean initialized = false;
        try {
            config = buildConfig(properties);
            initialized = true;
        } catch (RuntimeException ex) {
            // 支付通道初始化失败不应拖垮整个后端：降级为不可用，仅支付相关接口报错
            log.error("微信支付通道初始化失败，已降级为不可用（请检查 WX_PAY_* 配置/证书）: {}", ex.getMessage(), ex);
        }
        if (initialized) {
            this.jsapiService = new JsapiServiceExtension.Builder().config(config).build();
            this.notificationParser = new NotificationParser((NotificationConfig) config);
            this.refundService = new RefundService.Builder().config(config).build();
            this.ready = true;
        } else {
            this.jsapiService = null;
            this.notificationParser = null;
            this.refundService = null;
            this.ready = false;
        }
    }

    /** 构建微信支付配置：新商户用「微信支付公钥」(RSAPublicKeyConfig，平台证书已不再下发)； 未配置公钥时回退旧方式（自动下载平台证书）。 */
    private static Config buildConfig(WechatPayProperties properties) {
        if (StringUtils.hasText(properties.getPublicKeyId())
                && StringUtils.hasText(properties.getPublicKeyPath())) {
            return new RSAPublicKeyConfig.Builder()
                    .merchantId(properties.getMerchantId())
                    .privateKeyFromPath(properties.getPrivateKeyPath())
                    .merchantSerialNumber(properties.getMerchantSerialNumber())
                    .apiV3Key(properties.getApiV3Key())
                    .publicKeyId(properties.getPublicKeyId())
                    .publicKeyFromPath(properties.getPublicKeyPath())
                    .build();
        }
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(properties.getMerchantId())
                .privateKeyFromPath(properties.getPrivateKeyPath())
                .merchantSerialNumber(properties.getMerchantSerialNumber())
                .apiV3Key(properties.getApiV3Key())
                .build();
    }

    @Override
    public int provider() {
        return PROVIDER_WECHAT;
    }

    @Override
    public boolean isAvailable() {
        return ready;
    }

    private void ensureReady() {
        if (!ready) {
            throw new BusinessException(
                    ErrorCode.SYSTEM_ERROR, "微信支付通道未就绪（配置/证书有误），请检查 WX_PAY_* 配置");
        }
    }

    @Override
    public Map<String, String> prepay(PaymentPrepayRequest request) {
        ensureReady();
        validatePrepayRequest(request);
        PrepayRequest channelRequest = new PrepayRequest();
        channelRequest.setAppid(properties.getAppId());
        channelRequest.setMchid(properties.getMerchantId());
        channelRequest.setDescription(request.description());
        channelRequest.setOutTradeNo(request.paymentNo());
        channelRequest.setNotifyUrl(properties.getNotifyUrl());
        channelRequest.setTimeExpire(
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                        Instant.ofEpochMilli(request.expiresAt()).atOffset(ZoneOffset.ofHours(8))));

        Amount amount = new Amount();
        amount.setTotal(toCents(request.amount()));
        amount.setCurrency(request.currency());
        channelRequest.setAmount(amount);

        Payer payer = new Payer();
        payer.setOpenid(request.payerOpenId());
        channelRequest.setPayer(payer);

        try {
            PrepayWithRequestPaymentResponse response =
                    jsapiService.prepayWithRequestPayment(channelRequest);
            Map<String, String> parameters = new LinkedHashMap<>();
            parameters.put("timeStamp", response.getTimeStamp());
            parameters.put("nonceStr", response.getNonceStr());
            parameters.put("package", response.getPackageVal());
            parameters.put("signType", response.getSignType());
            parameters.put("paySign", response.getPaySign());
            return parameters;
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信支付预下单失败，请稍后重试");
        }
    }

    @Override
    public PaymentQueryResult query(String paymentNo) {
        ensureReady();
        if (!StringUtils.hasText(paymentNo)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "支付单号不能为空");
        }
        QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
        request.setMchid(properties.getMerchantId());
        request.setOutTradeNo(paymentNo);
        try {
            Transaction transaction = jsapiService.queryOrderByOutTradeNo(request);
            return toQueryResult(transaction, properties.getAppId(), properties.getMerchantId());
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信支付查单失败，请稍后重试");
        }
    }

    @Override
    public void close(String paymentNo) {
        ensureReady();
        if (!StringUtils.hasText(paymentNo)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "支付单号不能为空");
        }
        CloseOrderRequest request = new CloseOrderRequest();
        request.setMchid(properties.getMerchantId());
        request.setOutTradeNo(paymentNo);
        try {
            jsapiService.closeOrder(request);
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信支付关单失败，请稍后重试");
        }
    }

    @Override
    public RefundResult refund(RefundRequest request) {
        ensureReady();
        validateRefundRequest(request);
        CreateRequest channelRequest = new CreateRequest();
        channelRequest.setOutTradeNo(request.paymentNo());
        channelRequest.setOutRefundNo(request.refundNo());
        channelRequest.setReason(request.reason());
        channelRequest.setNotifyUrl(properties.getRefundNotifyUrl());

        AmountReq amount = new AmountReq();
        amount.setRefund((long) toCents(request.refundAmount()));
        amount.setTotal((long) toCents(request.totalAmount()));
        amount.setCurrency(request.currency());
        channelRequest.setAmount(amount);

        try {
            Refund refund = refundService.create(channelRequest);
            return toRefundResult(refund, request.refundNo());
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信支付退款申请失败，请稍后重试");
        }
    }

    @Override
    public RefundResult queryRefund(String refundNo) {
        ensureReady();
        if (!StringUtils.hasText(refundNo)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "退款单号不能为空");
        }
        QueryByOutRefundNoRequest request = new QueryByOutRefundNoRequest();
        request.setOutRefundNo(refundNo);
        try {
            Refund refund = refundService.queryByOutRefundNo(request);
            return toRefundResult(refund, refundNo);
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信支付退款查询失败，请稍后重试");
        }
    }

    @Override
    public VerifiedPaymentCallback verifyCallback(Map<String, String> headers, String body) {
        ensureReady();
        try {
            RequestParam requestParam = buildRequestParam(headers, body);
            Transaction transaction = notificationParser.parse(requestParam, Transaction.class);
            return toVerifiedCallback(
                    transaction, body, properties.getAppId(), properties.getMerchantId());
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR, "微信支付回调验签或解密失败");
        }
    }

    @Override
    public VerifiedRefundCallback verifyRefundCallback(Map<String, String> headers, String body) {
        ensureReady();
        try {
            RequestParam requestParam = buildRequestParam(headers, body);
            RefundNotification notification =
                    notificationParser.parse(requestParam, RefundNotification.class);
            return toVerifiedRefundCallback(notification, body);
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR, "微信退款回调验签或解密失败");
        }
    }

    private static RequestParam buildRequestParam(Map<String, String> headers, String body) {
        return new RequestParam.Builder()
                .serialNumber(header(headers, "Wechatpay-Serial"))
                .signature(header(headers, "Wechatpay-Signature"))
                .timestamp(header(headers, "Wechatpay-Timestamp"))
                .nonce(header(headers, "Wechatpay-Nonce"))
                .signType(header(headers, "Wechatpay-Signature-Type"))
                .body(body)
                .build();
    }

    static VerifiedPaymentCallback toVerifiedCallback(
            Transaction transaction, String body, String expectedAppId, String expectedMerchantId) {
        if (transaction == null
                || !expectedAppId.equals(transaction.getAppid())
                || !expectedMerchantId.equals(transaction.getMchid())
                || !StringUtils.hasText(transaction.getOutTradeNo())
                || !StringUtils.hasText(transaction.getTransactionId())
                || transaction.getAmount() == null
                || transaction.getAmount().getTotal() == null
                || !StringUtils.hasText(transaction.getAmount().getCurrency())) {
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR, "微信支付回调商户或交易字段不匹配");
        }
        return new VerifiedPaymentCallback(
                transaction.getOutTradeNo(),
                transaction.getTransactionId(),
                BigDecimal.valueOf(transaction.getAmount().getTotal(), 2),
                transaction.getAmount().getCurrency(),
                transaction.getTradeState() == Transaction.TradeStateEnum.SUCCESS,
                sha256(body));
    }

    static VerifiedRefundCallback toVerifiedRefundCallback(
            RefundNotification notification, String body) {
        if (notification == null
                || !StringUtils.hasText(notification.getOutRefundNo())
                || notification.getRefundStatus() == null
                || notification.getAmount() == null
                || notification.getAmount().getRefund() == null
                || !StringUtils.hasText(notification.getAmount().getCurrency())) {
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR, "微信退款回调字段不完整");
        }
        ChannelRefundStatus status = mapRefundStatus(notification.getRefundStatus());
        if (status == ChannelRefundStatus.SUCCESS
                && !StringUtils.hasText(notification.getRefundId())) {
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR, "微信退款成功但渠道退款单号为空");
        }
        return new VerifiedRefundCallback(
                notification.getOutRefundNo(),
                notification.getRefundId(),
                BigDecimal.valueOf(notification.getAmount().getRefund(), 2),
                notification.getAmount().getCurrency(),
                status,
                sha256(body));
    }

    static PaymentQueryResult toQueryResult(
            Transaction transaction, String expectedAppId, String expectedMerchantId) {
        if (transaction == null
                || !expectedAppId.equals(transaction.getAppid())
                || !expectedMerchantId.equals(transaction.getMchid())
                || !StringUtils.hasText(transaction.getOutTradeNo())
                || transaction.getAmount() == null
                || transaction.getAmount().getTotal() == null
                || !StringUtils.hasText(transaction.getAmount().getCurrency())
                || transaction.getTradeState() == null) {
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR, "微信支付查单结果字段不完整");
        }
        ChannelPaymentStatus status = mapTradeState(transaction.getTradeState());
        if (status == ChannelPaymentStatus.SUCCESS
                && !StringUtils.hasText(transaction.getTransactionId())) {
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR, "微信支付成功交易号为空");
        }
        return new PaymentQueryResult(
                transaction.getOutTradeNo(),
                transaction.getTransactionId(),
                BigDecimal.valueOf(transaction.getAmount().getTotal(), 2),
                transaction.getAmount().getCurrency(),
                status,
                "wechat-query:" + transaction.getTradeState());
    }

    private static ChannelPaymentStatus mapTradeState(Transaction.TradeStateEnum state) {
        if (state == Transaction.TradeStateEnum.SUCCESS) return ChannelPaymentStatus.SUCCESS;
        if (state == Transaction.TradeStateEnum.CLOSED
                || state == Transaction.TradeStateEnum.REVOKED
                || state == Transaction.TradeStateEnum.REFUND) {
            return ChannelPaymentStatus.CLOSED;
        }
        if (state == Transaction.TradeStateEnum.PAYERROR) return ChannelPaymentStatus.FAILED;
        return ChannelPaymentStatus.PENDING;
    }

    private static int toCents(BigDecimal amount) {
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).intValueExact();
        } catch (ArithmeticException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "支付金额精度或范围不正确");
        }
    }

    private static String header(Map<String, String> headers, String name) {
        if (headers == null) return null;
        return headers.entrySet().stream()
                .filter(entry -> name.equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static String sha256(String body) {
        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256")
                            .digest((body == null ? "" : body).getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }

    private static void validateConfiguration(WechatPayProperties properties) {
        if (!StringUtils.hasText(properties.getAppId())
                || !StringUtils.hasText(properties.getMerchantId())
                || !StringUtils.hasText(properties.getMerchantSerialNumber())
                || !StringUtils.hasText(properties.getPrivateKeyPath())
                || !StringUtils.hasText(properties.getApiV3Key())
                || properties.getApiV3Key().length() != 32
                || !StringUtils.hasText(properties.getNotifyUrl())
                || !properties.getNotifyUrl().startsWith("https://")
                || !StringUtils.hasText(properties.getRefundNotifyUrl())
                || !properties.getRefundNotifyUrl().startsWith("https://")) {
            throw new IllegalStateException("微信支付配置不完整或格式不正确");
        }
    }

    private static void validatePrepayRequest(PaymentPrepayRequest request) {
        if (request == null
                || !StringUtils.hasText(request.paymentNo())
                || request.amount() == null
                || request.amount().compareTo(BigDecimal.ZERO) <= 0
                || !StringUtils.hasText(request.currency())
                || !StringUtils.hasText(request.description())
                || request.expiresAt() <= System.currentTimeMillis()
                || !StringUtils.hasText(request.payerOpenId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "微信支付预下单参数不完整");
        }
    }

    private static void validateRefundRequest(RefundRequest request) {
        if (request == null
                || !StringUtils.hasText(request.paymentNo())
                || !StringUtils.hasText(request.refundNo())
                || request.totalAmount() == null
                || request.totalAmount().compareTo(BigDecimal.ZERO) <= 0
                || request.refundAmount() == null
                || request.refundAmount().compareTo(BigDecimal.ZERO) <= 0
                || request.refundAmount().compareTo(request.totalAmount()) > 0
                || !StringUtils.hasText(request.currency())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "微信支付退款参数不完整");
        }
    }

    static RefundResult toRefundResult(Refund refund, String expectedRefundNo) {
        if (refund == null
                || !StringUtils.hasText(refund.getOutRefundNo())
                || !expectedRefundNo.equals(refund.getOutRefundNo())
                || refund.getAmount() == null
                || refund.getAmount().getRefund() == null
                || !StringUtils.hasText(refund.getAmount().getCurrency())
                || refund.getStatus() == null) {
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR, "微信支付退款结果字段不完整");
        }
        ChannelRefundStatus status = mapRefundStatus(refund.getStatus());
        if (status == ChannelRefundStatus.SUCCESS && !StringUtils.hasText(refund.getRefundId())) {
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR, "微信支付退款成功但渠道退款单号为空");
        }
        return new RefundResult(
                refund.getOutRefundNo(),
                refund.getRefundId(),
                BigDecimal.valueOf(refund.getAmount().getRefund(), 2),
                refund.getAmount().getCurrency(),
                status,
                "wechat-refund:" + refund.getStatus());
    }

    private static ChannelRefundStatus mapRefundStatus(Status status) {
        if (status == Status.SUCCESS) return ChannelRefundStatus.SUCCESS;
        if (status == Status.CLOSED) return ChannelRefundStatus.CLOSED;
        if (status == Status.ABNORMAL) return ChannelRefundStatus.ABNORMAL;
        return ChannelRefundStatus.PROCESSING;
    }
}
