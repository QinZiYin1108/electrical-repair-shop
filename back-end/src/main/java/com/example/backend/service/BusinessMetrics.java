package com.example.backend.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/** 业务指标记录（Micrometer/Prometheus 暴露）。仅做计数，不影响业务逻辑。 */
@Component
public class BusinessMetrics {
    private final MeterRegistry registry;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void paymentIntentCreated(int orderType) {
        Counter.builder("business.payment.intent.created")
                .tag("orderType", String.valueOf(orderType))
                .register(registry)
                .increment();
    }

    public void paymentSucceeded(int orderType) {
        Counter.builder("business.payment.succeeded")
                .tag("orderType", String.valueOf(orderType))
                .register(registry)
                .increment();
    }

    public void paymentCallbackFailed(int provider) {
        Counter.builder("business.payment.callback.failed")
                .tag("provider", String.valueOf(provider))
                .register(registry)
                .increment();
    }

    public void refundInitiated() {
        Counter.builder("business.refund.initiated").register(registry).increment();
    }

    public void refundSucceeded() {
        Counter.builder("business.refund.succeeded").register(registry).increment();
    }

    public void reconciliationIssue(int count) {
        if (count <= 0) {
            return;
        }
        Counter.builder("business.reconciliation.issues").register(registry).increment(count);
    }

    public void notificationOutboxEnqueued(String channel) {
        outboxCounter("business.notification.outbox.enqueued", channel).increment();
    }

    public void notificationOutboxSent(String channel) {
        outboxCounter("business.notification.outbox.sent", channel).increment();
    }

    public void notificationOutboxFailed(String channel) {
        outboxCounter("business.notification.outbox.failed", channel).increment();
    }

    public void notificationOutboxDeadLetter(String channel) {
        outboxCounter("business.notification.outbox.deadletter", channel).increment();
    }

    public void orderSlaExceeded(String slaType) {
        slaCounter("business.order.sla.exceeded", slaType).increment();
    }

    public void orderSlaEscalated(String slaType) {
        slaCounter("business.order.sla.escalated", slaType).increment();
    }

    public void orderSlaAutoCanceled(String slaType) {
        slaCounter("business.order.sla.auto.canceled", slaType).increment();
    }

    public void orderSlaRecovered(String slaType) {
        slaCounter("business.order.sla.recovered", slaType).increment();
    }

    private Counter slaCounter(String name, String slaType) {
        return Counter.builder(name)
                .tag("slaType", slaType == null ? "unknown" : slaType)
                .register(registry);
    }

    public void supportTicketCreated() {
        Counter.builder("business.support.ticket.created").register(registry).increment();
    }

    public void supportTicketResolved() {
        Counter.builder("business.support.ticket.resolved").register(registry).increment();
    }

    public void supportTicketApprovalRequired() {
        Counter.builder("business.support.ticket.approval.required").register(registry).increment();
    }

    public void supportTicketApproved() {
        Counter.builder("business.support.ticket.approved").register(registry).increment();
    }

    public void appointmentReserved() {
        Counter.builder("business.appointment.reserved").register(registry).increment();
    }

    public void appointmentReleased() {
        Counter.builder("business.appointment.released").register(registry).increment();
    }

    public void appointmentCapacityRejected() {
        Counter.builder("business.appointment.rejected").register(registry).increment();
    }

    public void invoiceApplied() {
        Counter.builder("business.invoice.applied").register(registry).increment();
    }

    public void invoiceIssued() {
        Counter.builder("business.invoice.issued").register(registry).increment();
    }

    public void invoiceRed() {
        Counter.builder("business.invoice.red").register(registry).increment();
    }

    public void financeSnapshotRebuilt() {
        Counter.builder("business.finance.snapshot.rebuilt").register(registry).increment();
    }

    private Counter outboxCounter(String name, String channel) {
        return Counter.builder(name)
                .tag("channel", channel == null ? "unknown" : channel)
                .register(registry);
    }
}
