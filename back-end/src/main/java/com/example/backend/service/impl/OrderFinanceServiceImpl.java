package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.common.ErrorCode;
import com.example.backend.domain.finance.FinanceTimeBasis;
import com.example.backend.entity.FinanceOrderSnapshots;
import com.example.backend.entity.ProductOrders;
import com.example.backend.entity.RepairOrderPayments;
import com.example.backend.entity.RepairOrders;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.FinanceOrderSnapshotsMapper;
import com.example.backend.model.finance.FinanceReportResponse;
import com.example.backend.model.finance.FinanceReportRow;
import com.example.backend.service.BusinessMetrics;
import com.example.backend.service.OrderFinanceService;
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

/** 订单财务核算实现。佣金率/税率默认 0（不改变既有结算），仅将收入归属与分摊结构化记录，便于报表核对。 */
@Service
public class OrderFinanceServiceImpl
        extends ServiceImpl<FinanceOrderSnapshotsMapper, FinanceOrderSnapshots>
        implements OrderFinanceService {

    static final int ORDER_TYPE_REPAIR = 1;
    static final int ORDER_TYPE_PRODUCT = 2;
    static final String DISCOUNT_BEARER_PLATFORM = "PLATFORM";
    private static final int STATUS_NORMAL = 1;

    private final RepairOrdersService repairOrdersService;
    private final RepairOrderPaymentsService repairOrderPaymentsService;
    private final ProductOrdersService productOrdersService;
    private final BusinessMetrics businessMetrics;

    @Value("${finance.commission-rate:0}")
    private BigDecimal commissionRate = BigDecimal.ZERO;

    @Value("${finance.tax-rate:0}")
    private BigDecimal taxRate = BigDecimal.ZERO;

    public OrderFinanceServiceImpl(
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
    public FinanceOrderSnapshots rebuild(int orderType, String orderId) {
        if (!StringUtils.hasText(orderId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单ID不能为空");
        }
        FinanceOrderSnapshots snapshot =
                orderType == ORDER_TYPE_PRODUCT ? buildProduct(orderId) : buildRepair(orderId);
        snapshot.setOrderType(orderType);
        long now = System.currentTimeMillis();
        snapshot.setStatus(STATUS_NORMAL);
        snapshot.setUpdatedTime(now);
        snapshot.setNetAmount(round(snapshot.getTotalPaid().subtract(snapshot.getTotalRefunded())));
        snapshot.setDiscountBearer(
                snapshot.getDiscountAmount() != null
                                && snapshot.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0
                        ? DISCOUNT_BEARER_PLATFORM
                        : null);
        if (snapshot.getTaxRate() == null) {
            snapshot.setTaxRate(normalizeRate(taxRate));
        }
        snapshot.setTaxAmount(round(snapshot.getNetAmount().multiply(snapshot.getTaxRate())));
        BigDecimal commission =
                round(snapshot.getNetAmount().multiply(normalizeRate(commissionRate)));
        snapshot.setPlatformCommission(commission);
        snapshot.setTechnicianIncome(
                orderType == ORDER_TYPE_REPAIR
                        ? round(snapshot.getNetAmount().subtract(commission))
                        : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        snapshot.setIsDelete(0);
        snapshot.setVersion(0);

        FinanceOrderSnapshots existing =
                getOne(
                        new LambdaQueryWrapper<FinanceOrderSnapshots>()
                                .eq(FinanceOrderSnapshots::getOrderType, orderType)
                                .eq(FinanceOrderSnapshots::getOrderId, orderId)
                                .last("limit 1"),
                        false);
        if (existing == null) {
            snapshot.setId(SnowflakeIdUtil.nextFinanceSnapshotId());
            snapshot.setCreatedTime(now);
            if (!save(snapshot)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成财务快照失败");
            }
        } else {
            snapshot.setId(existing.getId());
            snapshot.setCreatedTime(existing.getCreatedTime());
            snapshot.setVersion(existing.getVersion());
            if (!updateById(snapshot)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新财务快照失败");
            }
        }
        businessMetrics.financeSnapshotRebuilt();
        return snapshot;
    }

    @Override
    public List<FinanceOrderSnapshots> listSnapshots(Integer orderType, String orderId, int limit) {
        LambdaQueryWrapper<FinanceOrderSnapshots> wrapper =
                new LambdaQueryWrapper<FinanceOrderSnapshots>()
                        .orderByDesc(FinanceOrderSnapshots::getCreatedTime)
                        .last("limit " + Math.min(Math.max(limit, 1), 200));
        if (orderType != null) {
            wrapper.eq(FinanceOrderSnapshots::getOrderType, orderType);
        }
        if (StringUtils.hasText(orderId)) {
            wrapper.eq(FinanceOrderSnapshots::getOrderId, orderId);
        }
        return list(wrapper);
    }

    @Override
    public FinanceReportResponse report(long from, long to, String timeBasis) {
        FinanceTimeBasis basis = FinanceTimeBasis.fromValue(timeBasis);
        LambdaQueryWrapper<FinanceOrderSnapshots> wrapper =
                new LambdaQueryWrapper<FinanceOrderSnapshots>();
        switch (basis) {
            case PERFORM_TIME ->
                    wrapper.ge(FinanceOrderSnapshots::getPerformTime, from)
                            .le(FinanceOrderSnapshots::getPerformTime, to);
            case REFUND_TIME ->
                    wrapper.ge(FinanceOrderSnapshots::getRefundTime, from)
                            .le(FinanceOrderSnapshots::getRefundTime, to);
            default ->
                    wrapper.ge(FinanceOrderSnapshots::getPayTime, from)
                            .le(FinanceOrderSnapshots::getPayTime, to);
        }
        List<FinanceOrderSnapshots> rows = list(wrapper);
        FinanceReportRow row = aggregate(rows);
        return new FinanceReportResponse(basis.name(), from, to, row);
    }

    private FinanceReportRow aggregate(List<FinanceOrderSnapshots> rows) {
        BigDecimal service = BigDecimal.ZERO;
        BigDecimal material = BigDecimal.ZERO;
        BigDecimal product = BigDecimal.ZERO;
        BigDecimal doorFee = BigDecimal.ZERO;
        BigDecimal shipping = BigDecimal.ZERO;
        BigDecimal platformService = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        BigDecimal refunded = BigDecimal.ZERO;
        BigDecimal net = BigDecimal.ZERO;
        BigDecimal commission = BigDecimal.ZERO;
        BigDecimal technician = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        for (FinanceOrderSnapshots row : rows) {
            service = service.add(nz(row.getIncomeService()));
            material = material.add(nz(row.getIncomeMaterial()));
            product = product.add(nz(row.getIncomeProduct()));
            doorFee = doorFee.add(nz(row.getIncomeDoorFee()));
            shipping = shipping.add(nz(row.getIncomeShipping()));
            platformService = platformService.add(nz(row.getIncomePlatformService()));
            paid = paid.add(nz(row.getTotalPaid()));
            refunded = refunded.add(nz(row.getTotalRefunded()));
            net = net.add(nz(row.getNetAmount()));
            commission = commission.add(nz(row.getPlatformCommission()));
            technician = technician.add(nz(row.getTechnicianIncome()));
            tax = tax.add(nz(row.getTaxAmount()));
            discount = discount.add(nz(row.getDiscountAmount()));
        }
        return new FinanceReportRow(
                rows.size(),
                service,
                material,
                product,
                doorFee,
                shipping,
                platformService,
                paid,
                refunded,
                net,
                commission,
                technician,
                tax,
                discount);
    }

    private FinanceOrderSnapshots buildRepair(String orderId) {
        RepairOrders order = repairOrdersService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "维修订单不存在");
        }
        RepairOrderPayments payment =
                repairOrderPaymentsService.getOne(
                        new LambdaQueryWrapper<RepairOrderPayments>()
                                .eq(RepairOrderPayments::getRepairOrderId, orderId)
                                .last("limit 1"),
                        false);
        FinanceOrderSnapshots s =
                base(order.getOrderNo(), order.getAccountId(), order.getRefundAmount());
        s.setPayTime(payment == null ? null : payment.getPaymentTime());
        s.setPerformTime(order.getCompletionTime());
        s.setRefundTime(order.getRefundTime());
        s.setTotalPaid(normalizeMoney(payment == null ? null : payment.getActualAmount()));
        s.setDiscountAmount(normalizeMoney(payment == null ? null : payment.getDiscountAmount()));
        s.setIncomeService(
                normalizeMoney(payment == null ? null : payment.getServiceFee())
                        .add(normalizeMoney(payment == null ? null : payment.getOvertimeFee())));
        s.setIncomeMaterial(normalizeMoney(payment == null ? null : payment.getMaterialFee()));
        s.setIncomeDoorFee(
                normalizeMoney(payment == null ? null : payment.getDoorFee())
                        .add(normalizeMoney(payment == null ? null : payment.getDistanceFee())));
        s.setIncomeProduct(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        s.setIncomeShipping(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        s.setIncomePlatformService(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        return s;
    }

    private FinanceOrderSnapshots buildProduct(String orderId) {
        ProductOrders order = productOrdersService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品订单不存在");
        }
        FinanceOrderSnapshots s =
                base(order.getOrderNo(), order.getAccountId(), order.getRefundAmount());
        s.setPayTime(order.getPaymentTime());
        s.setPerformTime(order.getCompletionTime());
        s.setRefundTime(order.getRefundTime());
        s.setTotalPaid(normalizeMoney(order.getActualAmount()));
        s.setDiscountAmount(normalizeMoney(order.getDiscountAmount()));
        s.setIncomeProduct(normalizeMoney(order.getProductAmount()));
        s.setIncomeShipping(normalizeMoney(order.getShippingFee()));
        s.setIncomeService(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        s.setIncomeMaterial(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        s.setIncomeDoorFee(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        s.setIncomePlatformService(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        return s;
    }

    private FinanceOrderSnapshots base(String orderNo, String accountId, BigDecimal refundAmount) {
        FinanceOrderSnapshots s = new FinanceOrderSnapshots();
        s.setOrderNo(orderNo);
        s.setUserAccountId(accountId);
        s.setTotalRefunded(normalizeMoney(refundAmount));
        s.setTaxRate(normalizeRate(taxRate));
        return s;
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeRate(BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return rate;
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
