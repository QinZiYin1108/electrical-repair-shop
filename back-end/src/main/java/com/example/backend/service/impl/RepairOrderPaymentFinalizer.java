package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.OrderProgress;
import com.example.backend.entity.PaymentRecords;
import com.example.backend.entity.RepairOrderPayments;
import com.example.backend.entity.RepairOrders;
import com.example.backend.exception.BusinessException;
import com.example.backend.payment.OrderPaymentFinalizer;
import com.example.backend.service.OrderProgressService;
import com.example.backend.service.RepairOrderFundService;
import com.example.backend.service.RepairOrderPaymentsService;
import com.example.backend.service.RepairOrdersService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 维修订单外部支付成功后的推进。钱包支付仍走原有同步链路；本类只处理渠道支付（微信）回调成功后的订单推进、托管与进度记录。 */
@Component
public class RepairOrderPaymentFinalizer implements OrderPaymentFinalizer {
    private static final int ORDER_TYPE_REPAIR = 1;
    private static final int BIZ_STAGE_TAIL = 2;
    private static final int PAYMENT_STATUS_PAID = 2;
    private static final int OPERATOR_TYPE_USER = 1;
    private static final int ORDER_PROGRESS_STATUS_PAY = 4;

    private final RepairOrdersService repairOrdersService;
    private final RepairOrderPaymentsService repairOrderPaymentsService;
    private final RepairOrderFundService repairOrderFundService;
    private final OrderProgressService orderProgressService;

    public RepairOrderPaymentFinalizer(
            RepairOrdersService repairOrdersService,
            RepairOrderPaymentsService repairOrderPaymentsService,
            RepairOrderFundService repairOrderFundService,
            OrderProgressService orderProgressService) {
        this.repairOrdersService = repairOrdersService;
        this.repairOrderPaymentsService = repairOrderPaymentsService;
        this.repairOrderFundService = repairOrderFundService;
        this.orderProgressService = orderProgressService;
    }

    @Override
    public int orderType() {
        return ORDER_TYPE_REPAIR;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onPaymentSuccess(PaymentRecords payment, long now) {
        if (payment == null || !Integer.valueOf(ORDER_TYPE_REPAIR).equals(payment.getOrderType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非维修订单支付单");
        }
        RepairOrders order =
                repairOrdersService.getOne(
                        new LambdaQueryWrapper<RepairOrders>()
                                .eq(RepairOrders::getId, payment.getOrderId())
                                .eq(RepairOrders::getIsDelete, 0)
                                .last("limit 1 for update"),
                        false);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "维修订单不存在");
        }
        if (!order.getAccountId().equals(payment.getAccountId())) {
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR, "支付单与订单账号不匹配");
        }
        RepairOrderPayments orderPayment =
                repairOrderPaymentsService.getOne(
                        new LambdaQueryWrapper<RepairOrderPayments>()
                                .eq(RepairOrderPayments::getRepairOrderId, order.getId())
                                .eq(RepairOrderPayments::getIsDelete, 0)
                                .last("limit 1 for update"),
                        false);
        if (orderPayment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单支付信息不存在");
        }

        BigDecimal amount = normalizeMoney(payment.getPaymentAmount());
        boolean isTail = payment.getBizStage() != null && payment.getBizStage() == BIZ_STAGE_TAIL;
        BigDecimal paidBefore = normalizeMoney(orderPayment.getActualAmount());
        orderPayment.setActualAmount(paidBefore.add(amount));
        orderPayment.setPaymentMethod(payment.getPaymentMethod());
        orderPayment.setPaymentTime(now);
        orderPayment.setUpdatedTime(now);
        if (!repairOrderPaymentsService.updateById(orderPayment)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新订单支付信息失败");
        }

        order.setPaymentStatus(PAYMENT_STATUS_PAID);
        order.setUpdatedTime(now);
        if (!repairOrdersService.updateById(order)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新订单状态失败");
        }

        if (isTail) {
            repairOrderFundService.recordOrderTailPay(
                    order.getAccountId(),
                    order.getTechnicianAccountId(),
                    order.getId(),
                    order.getOrderNo(),
                    payment.getPaymentMethod(),
                    amount,
                    now);
            savePaymentProgress(order, "尾款", payment.getPaymentMethod(), amount, now);
        } else {
            repairOrderFundService.recordOrderPrepay(
                    order.getAccountId(),
                    order.getTechnicianAccountId(),
                    order.getId(),
                    order.getOrderNo(),
                    payment.getPaymentMethod(),
                    amount,
                    now);
            savePaymentProgress(order, "预付款", payment.getPaymentMethod(), amount, now);
        }
    }

    private void savePaymentProgress(
            RepairOrders order,
            String stageText,
            Integer paymentMethod,
            BigDecimal amount,
            long now) {
        OrderProgress progress = new OrderProgress();
        progress.setId(SnowflakeIdUtil.nextOrderProgressId());
        progress.setOrderId(order.getId());
        progress.setStatus(ORDER_PROGRESS_STATUS_PAY);
        progress.setStatusName("费用支付");
        progress.setDescription(
                "用户已支付"
                        + stageText
                        + "，支付方式："
                        + paymentMethodText(paymentMethod)
                        + "；支付金额："
                        + amount.toPlainString());
        progress.setOperatorId(order.getAccountId());
        progress.setOperatorType(OPERATOR_TYPE_USER);
        progress.setOperatorName("用户");
        progress.setCreatedTime(now);
        progress.setIsDelete(0);
        if (!orderProgressService.save(progress)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存订单进度失败");
        }
    }

    private String paymentMethodText(Integer paymentMethod) {
        int value = paymentMethod == null ? 0 : paymentMethod;
        if (value == 5) {
            return "钱包支付";
        }
        if (value == 2) {
            return "支付宝支付";
        }
        return "微信支付";
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }
}
