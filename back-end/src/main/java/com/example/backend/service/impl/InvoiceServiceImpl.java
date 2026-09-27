package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.common.ErrorCode;
import com.example.backend.domain.finance.InvoiceStatus;
import com.example.backend.entity.Invoices;
import com.example.backend.entity.ProductOrders;
import com.example.backend.entity.RepairOrderPayments;
import com.example.backend.entity.RepairOrders;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.InvoicesMapper;
import com.example.backend.model.finance.InvoiceApplyRequest;
import com.example.backend.service.BusinessMetrics;
import com.example.backend.service.InvoiceService;
import com.example.backend.service.ProductOrdersService;
import com.example.backend.service.RepairOrderPaymentsService;
import com.example.backend.service.RepairOrdersService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 发票实现。开票额度 = 订单实付 - 已退款 - 已申请/已开票金额。 */
@Service
public class InvoiceServiceImpl extends ServiceImpl<InvoicesMapper, Invoices>
        implements InvoiceService {

    static final int ORDER_TYPE_REPAIR = 1;
    static final int ORDER_TYPE_PRODUCT = 2;
    private static final int TITLE_TYPE_PERSONAL = 1;

    private final RepairOrdersService repairOrdersService;
    private final RepairOrderPaymentsService repairOrderPaymentsService;
    private final ProductOrdersService productOrdersService;
    private final BusinessMetrics businessMetrics;

    @Value("${finance.tax-rate:0}")
    private BigDecimal taxRate = BigDecimal.ZERO;

    public InvoiceServiceImpl(
            RepairOrdersService repairOrdersService,
            RepairOrderPaymentsService repairOrderPaymentsService,
            ProductOrdersService productOrdersService,
            BusinessMetrics businessMetrics) {
        this.repairOrdersService = repairOrdersService;
        this.repairOrderPaymentsService = repairOrderPaymentsService;
        this.productOrdersService = productOrdersService;
        this.businessMetrics = businessMetrics;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoices apply(String userAccountId, InvoiceApplyRequest request) {
        if (!StringUtils.hasText(userAccountId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (request == null || !StringUtils.hasText(request.getOrderId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单ID不能为空");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "发票抬头不能为空");
        }
        BigDecimal amount = normalizeMoney(request.getAmount());
        if (amount.compareTo(new BigDecimal("0.01")) < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "开票金额需不小于 0.01 元");
        }
        int orderType = resolveOrderType(request.getOrderType());
        OrderAmounts orderAmounts =
                resolveOrderAmounts(orderType, request.getOrderId(), userAccountId);
        BigDecimal invoiced = sumInvoiced(orderType, request.getOrderId());
        BigDecimal available =
                orderAmounts.paid().subtract(orderAmounts.refunded()).subtract(invoiced);
        if (amount.compareTo(available) > 0) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR, "开票金额超过可开票额度（剩余 " + available + " 元）");
        }

        long now = System.currentTimeMillis();
        BigDecimal rate = normalizeRate(taxRate);
        BigDecimal taxAmount =
                amount.multiply(rate).divide(BigDecimal.ONE.add(rate), 2, RoundingMode.HALF_UP);
        Invoices invoice = new Invoices();
        invoice.setId(SnowflakeIdUtil.nextInvoiceId());
        invoice.setInvoiceNo("IV" + invoice.getId().substring(2));
        invoice.setUserAccountId(userAccountId);
        invoice.setOrderType(orderType);
        invoice.setOrderId(request.getOrderId());
        invoice.setAmount(amount);
        invoice.setTaxRate(rate);
        invoice.setTaxAmount(taxAmount);
        invoice.setTitleType(
                request.getTitleType() == null ? TITLE_TYPE_PERSONAL : request.getTitleType());
        invoice.setTitle(request.getTitle().trim());
        invoice.setTaxpayerNo(trimToNull(request.getTaxpayerNo()));
        invoice.setContentType(trimToNull(request.getContentType()));
        invoice.setStatus(InvoiceStatus.PENDING.getCode());
        invoice.setApplyRemark(trimToNull(request.getApplyRemark()));
        invoice.setCreatedTime(now);
        invoice.setUpdatedTime(now);
        invoice.setVersion(0);
        invoice.setIsDelete(0);
        if (!save(invoice)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "提交发票申请失败");
        }
        businessMetrics.invoiceApplied();
        return invoice;
    }

    @Override
    public List<Invoices> listForUser(String userAccountId, int limit) {
        return list(
                new LambdaQueryWrapper<Invoices>()
                        .eq(Invoices::getUserAccountId, userAccountId)
                        .orderByDesc(Invoices::getCreatedTime)
                        .last("limit " + clampLimit(limit)));
    }

    @Override
    public List<Invoices> listForAdmin(Integer status, Integer orderType, int limit) {
        LambdaQueryWrapper<Invoices> wrapper =
                new LambdaQueryWrapper<Invoices>()
                        .orderByDesc(Invoices::getCreatedTime)
                        .last("limit " + clampLimit(limit));
        if (status != null) {
            wrapper.eq(Invoices::getStatus, status);
        }
        if (orderType != null) {
            wrapper.eq(Invoices::getOrderType, orderType);
        }
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoices issue(String id, String adminId, String invoiceUrl) {
        Invoices invoice = requirePending(id);
        long now = System.currentTimeMillis();
        invoice.setStatus(InvoiceStatus.ISSUED.getCode());
        invoice.setInvoiceUrl(trimToNull(invoiceUrl));
        invoice.setIssueTime(now);
        invoice.setOperatorAdminId(trimToNull(adminId));
        invoice.setUpdatedTime(now);
        updateInvoice(invoice);
        businessMetrics.invoiceIssued();
        return invoice;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoices reject(String id, String adminId, String reason) {
        Invoices invoice = requirePending(id);
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "驳回原因不能为空");
        }
        long now = System.currentTimeMillis();
        invoice.setStatus(InvoiceStatus.REJECTED.getCode());
        invoice.setRejectReason(trimToNull(reason));
        invoice.setOperatorAdminId(trimToNull(adminId));
        invoice.setUpdatedTime(now);
        updateInvoice(invoice);
        return invoice;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoices red(String id, String adminId, String reason) {
        Invoices invoice = requireInvoice(id);
        if (InvoiceStatus.fromCode(invoice.getStatus()) != InvoiceStatus.ISSUED) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "仅已开票的记录可红冲");
        }
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "红冲原因不能为空");
        }
        long now = System.currentTimeMillis();
        invoice.setStatus(InvoiceStatus.RED.getCode());
        invoice.setRedReason(trimToNull(reason));
        invoice.setRedTime(now);
        invoice.setOperatorAdminId(trimToNull(adminId));
        invoice.setUpdatedTime(now);
        updateInvoice(invoice);
        businessMetrics.invoiceRed();
        return invoice;
    }

    private OrderAmounts resolveOrderAmounts(int orderType, String orderId, String userAccountId) {
        if (orderType == ORDER_TYPE_REPAIR) {
            RepairOrders order = repairOrdersService.getById(orderId);
            if (order == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "维修订单不存在");
            }
            if (!userAccountId.equals(order.getAccountId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权对该订单开票");
            }
            RepairOrderPayments payment =
                    repairOrderPaymentsService.getOne(
                            new LambdaQueryWrapper<RepairOrderPayments>()
                                    .eq(RepairOrderPayments::getRepairOrderId, orderId)
                                    .last("limit 1"),
                            false);
            BigDecimal paid = payment == null ? BigDecimal.ZERO : payment.getActualAmount();
            return new OrderAmounts(normalizeMoney(paid), normalizeMoney(order.getRefundAmount()));
        }
        ProductOrders order = productOrdersService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品订单不存在");
        }
        if (!userAccountId.equals(order.getAccountId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权对该订单开票");
        }
        return new OrderAmounts(
                normalizeMoney(order.getActualAmount()), normalizeMoney(order.getRefundAmount()));
    }

    private BigDecimal sumInvoiced(int orderType, String orderId) {
        List<Invoices> list =
                list(
                        new LambdaQueryWrapper<Invoices>()
                                .eq(Invoices::getOrderType, orderType)
                                .eq(Invoices::getOrderId, orderId)
                                .in(
                                        Invoices::getStatus,
                                        InvoiceStatus.PENDING.getCode(),
                                        InvoiceStatus.ISSUED.getCode()));
        BigDecimal sum = BigDecimal.ZERO;
        for (Invoices item : list) {
            sum = sum.add(normalizeMoney(item.getAmount()));
        }
        return sum;
    }

    private Invoices requireInvoice(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "发票ID不能为空");
        }
        Invoices invoice = getById(id);
        if (invoice == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "发票记录不存在");
        }
        return invoice;
    }

    private Invoices requirePending(String id) {
        Invoices invoice = requireInvoice(id);
        if (InvoiceStatus.fromCode(invoice.getStatus()) != InvoiceStatus.PENDING) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "仅待开票的记录可操作");
        }
        return invoice;
    }

    private void updateInvoice(Invoices invoice) {
        if (!updateById(invoice)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新发票记录失败");
        }
    }

    private int resolveOrderType(Integer orderType) {
        if (orderType == null
                || (orderType != ORDER_TYPE_REPAIR && orderType != ORDER_TYPE_PRODUCT)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单类型仅支持 1-维修 / 2-商品");
        }
        return orderType;
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeRate(BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return rate;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private int clampLimit(int limit) {
        return Math.min(Math.max(limit, 1), 200);
    }

    private record OrderAmounts(BigDecimal paid, BigDecimal refunded) {}
}
