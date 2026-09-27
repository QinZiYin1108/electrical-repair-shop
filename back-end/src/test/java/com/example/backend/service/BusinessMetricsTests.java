package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class BusinessMetricsTests {

    @Test
    void countersIncrementWithTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BusinessMetrics metrics = new BusinessMetrics(registry);

        metrics.paymentIntentCreated(3);
        metrics.paymentIntentCreated(3);
        metrics.paymentSucceeded(3);
        metrics.refundInitiated();
        metrics.refundSucceeded();
        metrics.reconciliationIssue(2);

        assertEquals(
                2.0,
                registry.get("business.payment.intent.created")
                        .tag("orderType", "3")
                        .counter()
                        .count());
        assertEquals(
                1.0,
                registry.get("business.payment.succeeded").tag("orderType", "3").counter().count());
        assertEquals(1.0, registry.get("business.refund.initiated").counter().count());
        assertEquals(1.0, registry.get("business.refund.succeeded").counter().count());
        assertEquals(2.0, registry.get("business.reconciliation.issues").counter().count());
    }

    @Test
    void nonPositiveReconciliationCountIsIgnored() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BusinessMetrics metrics = new BusinessMetrics(registry);

        metrics.reconciliationIssue(0);

        assertEquals(0, registry.find("business.reconciliation.issues").counters().size());
    }

    @Test
    void notificationOutboxCountersIncrementWithChannelTag() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BusinessMetrics metrics = new BusinessMetrics(registry);

        metrics.notificationOutboxEnqueued("IN_APP");
        metrics.notificationOutboxSent("IN_APP");
        metrics.notificationOutboxFailed("SMS");
        metrics.notificationOutboxDeadLetter("SMS");

        assertEquals(
                1.0,
                registry.get("business.notification.outbox.enqueued")
                        .tag("channel", "IN_APP")
                        .counter()
                        .count());
        assertEquals(
                1.0,
                registry.get("business.notification.outbox.sent")
                        .tag("channel", "IN_APP")
                        .counter()
                        .count());
        assertEquals(
                1.0,
                registry.get("business.notification.outbox.failed")
                        .tag("channel", "SMS")
                        .counter()
                        .count());
        assertEquals(
                1.0,
                registry.get("business.notification.outbox.deadletter")
                        .tag("channel", "SMS")
                        .counter()
                        .count());
    }

    @Test
    void orderSlaCountersIncrementWithTypeTag() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BusinessMetrics metrics = new BusinessMetrics(registry);

        metrics.orderSlaExceeded("ACCEPT");
        metrics.orderSlaEscalated("ACCEPT");
        metrics.orderSlaAutoCanceled("ACCEPT");
        metrics.orderSlaRecovered("VISIT");

        assertEquals(
                1.0,
                registry.get("business.order.sla.exceeded")
                        .tag("slaType", "ACCEPT")
                        .counter()
                        .count());
        assertEquals(
                1.0,
                registry.get("business.order.sla.escalated")
                        .tag("slaType", "ACCEPT")
                        .counter()
                        .count());
        assertEquals(
                1.0,
                registry.get("business.order.sla.auto.canceled")
                        .tag("slaType", "ACCEPT")
                        .counter()
                        .count());
        assertEquals(
                1.0,
                registry.get("business.order.sla.recovered")
                        .tag("slaType", "VISIT")
                        .counter()
                        .count());
    }

    @Test
    void supportTicketCountersIncrement() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BusinessMetrics metrics = new BusinessMetrics(registry);

        metrics.supportTicketCreated();
        metrics.supportTicketResolved();
        metrics.supportTicketApprovalRequired();
        metrics.supportTicketApproved();

        assertEquals(1.0, registry.get("business.support.ticket.created").counter().count());
        assertEquals(1.0, registry.get("business.support.ticket.resolved").counter().count());
        assertEquals(
                1.0, registry.get("business.support.ticket.approval.required").counter().count());
        assertEquals(1.0, registry.get("business.support.ticket.approved").counter().count());
    }

    @Test
    void appointmentCountersIncrement() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BusinessMetrics metrics = new BusinessMetrics(registry);

        metrics.appointmentReserved();
        metrics.appointmentReleased();
        metrics.appointmentCapacityRejected();

        assertEquals(1.0, registry.get("business.appointment.reserved").counter().count());
        assertEquals(1.0, registry.get("business.appointment.released").counter().count());
        assertEquals(1.0, registry.get("business.appointment.rejected").counter().count());
    }

    @Test
    void invoiceAndFinanceCountersIncrement() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BusinessMetrics metrics = new BusinessMetrics(registry);

        metrics.invoiceApplied();
        metrics.invoiceIssued();
        metrics.invoiceRed();
        metrics.financeSnapshotRebuilt();

        assertEquals(1.0, registry.get("business.invoice.applied").counter().count());
        assertEquals(1.0, registry.get("business.invoice.issued").counter().count());
        assertEquals(1.0, registry.get("business.invoice.red").counter().count());
        assertEquals(1.0, registry.get("business.finance.snapshot.rebuilt").counter().count());
    }
}
