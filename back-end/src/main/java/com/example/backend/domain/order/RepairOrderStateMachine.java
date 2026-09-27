package com.example.backend.domain.order;

import com.example.backend.common.ErrorCode;
import com.example.backend.exception.BusinessException;
import java.util.EnumSet;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RepairOrderStateMachine {

    private static final Map<RepairOrderStatus, EnumSet<RepairOrderStatus>> TRANSITIONS =
            Map.of(
                    RepairOrderStatus.WAITING_ACCEPT,
                    EnumSet.of(
                            RepairOrderStatus.WAITING_VISIT,
                            RepairOrderStatus.WAITING_INSPECTION,
                            RepairOrderStatus.CANCELED,
                            RepairOrderStatus.REFUNDED),
                    RepairOrderStatus.WAITING_VISIT,
                    EnumSet.of(
                            RepairOrderStatus.WAITING_INSPECTION,
                            RepairOrderStatus.IN_SERVICE,
                            RepairOrderStatus.CANCELED,
                            RepairOrderStatus.REFUNDED),
                    RepairOrderStatus.WAITING_INSPECTION,
                    EnumSet.of(
                            RepairOrderStatus.WAITING_PAYMENT,
                            RepairOrderStatus.CANCELED,
                            RepairOrderStatus.REFUNDED),
                    RepairOrderStatus.WAITING_PAYMENT,
                    EnumSet.of(
                            RepairOrderStatus.IN_SERVICE,
                            RepairOrderStatus.COMPLETED,
                            RepairOrderStatus.CANCELED,
                            RepairOrderStatus.REFUNDED),
                    RepairOrderStatus.IN_SERVICE,
                    EnumSet.of(RepairOrderStatus.IN_SERVICE, RepairOrderStatus.COMPLETED),
                    RepairOrderStatus.COMPLETED,
                    EnumSet.of(RepairOrderStatus.REFUNDED),
                    RepairOrderStatus.CANCELED,
                    EnumSet.of(RepairOrderStatus.REFUNDED));

    public RepairOrderStatus statusOf(Integer status) {
        return RepairOrderStatus.fromCode(status);
    }

    public String statusText(Integer status) {
        return statusOf(status).getText();
    }

    public String displayStatusText(Integer status, boolean waitingUserConfirmation) {
        if (statusOf(status) == RepairOrderStatus.IN_SERVICE && waitingUserConfirmation) {
            return "待用户确认";
        }
        return statusText(status);
    }

    public boolean canPerform(
            Integer status, RepairOrderAction action, RepairOrderStateContext context) {
        RepairOrderStatus current = statusOf(status);
        RepairOrderStateContext state =
                context == null ? RepairOrderStateContext.defaults() : context;
        return switch (action) {
            case ACCEPT -> current == RepairOrderStatus.WAITING_ACCEPT;
            case CONSUME_DOOR_QR -> current == RepairOrderStatus.WAITING_VISIT;
            case SUBMIT_INSPECTION ->
                    current == RepairOrderStatus.WAITING_INSPECTION
                            && (state.serviceMode() == 1 || state.serviceMode() == 3);
            case EDIT_INSPECTION_FEES ->
                    current == RepairOrderStatus.WAITING_PAYMENT
                            && (state.serviceMode() == 1 || state.serviceMode() == 3)
                            && !state.fullyPaid();
            case PAY_TAIL -> current == RepairOrderStatus.WAITING_PAYMENT && !state.fullyPaid();
            case START_SERVICE -> current == RepairOrderStatus.WAITING_PAYMENT && state.fullyPaid();
            case SUBMIT_COMPLETION ->
                    current == RepairOrderStatus.IN_SERVICE && !state.waitingUserConfirmation();
            case CONFIRM_COMPLETION ->
                    (current == RepairOrderStatus.IN_SERVICE && state.waitingUserConfirmation())
                            || (current == RepairOrderStatus.WAITING_PAYMENT
                                    && state.tailPaymentCompleted());
            case CANCEL ->
                    current.getCode() >= RepairOrderStatus.WAITING_ACCEPT.getCode()
                            && current.getCode() <= RepairOrderStatus.WAITING_PAYMENT.getCode()
                            && !state.tailPaymentCompleted();
            case MODIFY_ORDER ->
                    current.getCode() >= RepairOrderStatus.WAITING_ACCEPT.getCode()
                            && current.getCode() <= RepairOrderStatus.WAITING_PAYMENT.getCode()
                            && !state.tailPaymentCompleted();
            case MODIFY_APPOINTMENT ->
                    current.getCode() >= RepairOrderStatus.WAITING_ACCEPT.getCode()
                            && current.getCode() < RepairOrderStatus.COMPLETED.getCode()
                            && !state.technicianArrived();
            case APPLY_AFTER_SALES ->
                    current == RepairOrderStatus.COMPLETED
                            && !state.activeAfterSales()
                            && state.withinAfterSalesWindow()
                            && state.hasPaidAmount();
        };
    }

    public void requireAction(
            Integer status,
            RepairOrderAction action,
            RepairOrderStateContext context,
            String message) {
        if (!canPerform(status, action, context)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, message);
        }
    }

    public boolean canTransition(Integer fromStatus, Integer toStatus) {
        RepairOrderStatus from = statusOf(fromStatus);
        RepairOrderStatus to = statusOf(toStatus);
        return TRANSITIONS.getOrDefault(from, EnumSet.noneOf(RepairOrderStatus.class)).contains(to);
    }

    public void requireTransition(Integer fromStatus, Integer toStatus) {
        if (!canTransition(fromStatus, toStatus)) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR,
                    "订单状态不允许从" + statusText(fromStatus) + "变更为" + statusText(toStatus));
        }
    }
}
