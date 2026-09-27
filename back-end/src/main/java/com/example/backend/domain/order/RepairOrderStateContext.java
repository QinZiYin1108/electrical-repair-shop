package com.example.backend.domain.order;

public record RepairOrderStateContext(
        int serviceMode,
        boolean fullyPaid,
        boolean tailPaymentCompleted,
        boolean waitingUserConfirmation,
        boolean technicianArrived,
        boolean activeAfterSales,
        boolean withinAfterSalesWindow,
        boolean hasPaidAmount) {

    public static RepairOrderStateContext defaults() {
        return new RepairOrderStateContext(0, false, false, false, false, false, false, false);
    }
}
