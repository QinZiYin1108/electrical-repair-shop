package com.example.backend.service;

import com.example.backend.entity.RepairOrderPayments;
import com.example.backend.entity.RepairOrders;

import java.math.BigDecimal;

public interface RepairOrderFundService {

    /**
     * Record user prepay expense and freeze technician pending income for the order.
     * This method should be idempotent (safe to call multiple times for same order).
     */
    void recordOrderPrepay(
        String userAccountId,
        String technicianAccountId,
        String orderId,
        String orderNo,
        Integer paymentMethod,
        BigDecimal amount,
        long now
    );

    /**
     * Record user tail payment expense and freeze additional technician pending income for the order.
     * This method should be idempotent (safe to call multiple times for same order).
     */
    void recordOrderTailPay(
        String userAccountId,
        String technicianAccountId,
        String orderId,
        String orderNo,
        Integer paymentMethod,
        BigDecimal amount,
        long now
    );

    /**
     * Freeze paid amount for technician when order is completed.
     * The amount becomes withdrawable after the 7-day after-sales protection period.
     * This method should be idempotent (safe to call multiple times for same order).
     */
    void settleOnOrderCompleted(RepairOrders order, RepairOrderPayments payment, long now);

    /**
     * Release eligible frozen technician income into available balance after the protection period.
     * This method should be idempotent.
     */
    void releaseEligibleTechnicianFunds(String technicianAccountId, long now);

    /**
     * Settle retained prepaid amount to technician when an on-site order is cancelled after arrival.
     * This method should be idempotent.
     */
    void settleRetainedAmountOnCancel(RepairOrders order, BigDecimal amount, long now);

    /**
     * Refund prepay amount back to user and release escrow when order is cancelled/refunded before settlement.
     * This method should be idempotent.
     */
    void refundOnOrderClosed(RepairOrders order, RepairOrderPayments payment, String reason, long now);
}
