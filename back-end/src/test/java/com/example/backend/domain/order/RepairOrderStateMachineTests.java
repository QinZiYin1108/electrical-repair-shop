package com.example.backend.domain.order;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.backend.exception.BusinessException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RepairOrderStateMachineTests {

    private final RepairOrderStateMachine stateMachine = new RepairOrderStateMachine();

    @ParameterizedTest
    @MethodSource("legalActions")
    void allowsLegalActions(int status, RepairOrderAction action, RepairOrderStateContext context) {
        assertTrue(stateMachine.canPerform(status, action, context));
    }

    @ParameterizedTest
    @MethodSource("illegalActions")
    void rejectsIllegalActions(
            int status, RepairOrderAction action, RepairOrderStateContext context) {
        assertFalse(stateMachine.canPerform(status, action, context));
    }

    @Test
    void rejectsUnknownHistoricalStatusWithoutFailingToParse() {
        assertFalse(
                stateMachine.canPerform(
                        99, RepairOrderAction.ACCEPT, RepairOrderStateContext.defaults()));
        assertThrows(
                BusinessException.class,
                () -> stateMachine.requireTransition(99, RepairOrderStatus.COMPLETED.getCode()));
    }

    @Test
    void validatesLegalAndIllegalTransitions() {
        assertTrue(
                stateMachine.canTransition(
                        RepairOrderStatus.WAITING_ACCEPT.getCode(),
                        RepairOrderStatus.WAITING_VISIT.getCode()));
        assertFalse(
                stateMachine.canTransition(
                        RepairOrderStatus.WAITING_ACCEPT.getCode(),
                        RepairOrderStatus.COMPLETED.getCode()));
    }

    private static Stream<Arguments> legalActions() {
        RepairOrderStateContext defaults = RepairOrderStateContext.defaults();
        return Stream.of(
                Arguments.of(1, RepairOrderAction.ACCEPT, defaults),
                Arguments.of(2, RepairOrderAction.CONSUME_DOOR_QR, defaults),
                Arguments.of(
                        3, RepairOrderAction.SUBMIT_INSPECTION, context(1, false, false, false)),
                Arguments.of(
                        4, RepairOrderAction.EDIT_INSPECTION_FEES, context(3, false, false, false)),
                Arguments.of(4, RepairOrderAction.PAY_TAIL, context(1, false, false, false)),
                Arguments.of(4, RepairOrderAction.START_SERVICE, context(1, true, true, false)),
                Arguments.of(5, RepairOrderAction.SUBMIT_COMPLETION, context(1, true, true, false)),
                Arguments.of(5, RepairOrderAction.CONFIRM_COMPLETION, context(1, true, true, true)),
                Arguments.of(3, RepairOrderAction.CANCEL, defaults),
                Arguments.of(
                        6,
                        RepairOrderAction.APPLY_AFTER_SALES,
                        new RepairOrderStateContext(
                                1, true, true, false, true, false, true, true)));
    }

    private static Stream<Arguments> illegalActions() {
        RepairOrderStateContext defaults = RepairOrderStateContext.defaults();
        return Stream.of(
                Arguments.of(2, RepairOrderAction.ACCEPT, defaults),
                Arguments.of(1, RepairOrderAction.CONSUME_DOOR_QR, defaults),
                Arguments.of(
                        3, RepairOrderAction.SUBMIT_INSPECTION, context(2, false, false, false)),
                Arguments.of(
                        4, RepairOrderAction.EDIT_INSPECTION_FEES, context(1, true, true, false)),
                Arguments.of(4, RepairOrderAction.PAY_TAIL, context(1, true, true, false)),
                Arguments.of(4, RepairOrderAction.START_SERVICE, context(1, false, false, false)),
                Arguments.of(5, RepairOrderAction.SUBMIT_COMPLETION, context(1, true, true, true)),
                Arguments.of(
                        5, RepairOrderAction.CONFIRM_COMPLETION, context(1, true, true, false)),
                Arguments.of(4, RepairOrderAction.CANCEL, context(1, true, true, false)),
                Arguments.of(7, RepairOrderAction.CANCEL, defaults),
                Arguments.of(
                        6,
                        RepairOrderAction.APPLY_AFTER_SALES,
                        new RepairOrderStateContext(1, true, true, false, true, true, true, true)));
    }

    private static RepairOrderStateContext context(
            int serviceMode,
            boolean fullyPaid,
            boolean tailPaymentCompleted,
            boolean waitingUserConfirmation) {
        return new RepairOrderStateContext(
                serviceMode,
                fullyPaid,
                tailPaymentCompleted,
                waitingUserConfirmation,
                false,
                false,
                false,
                false);
    }
}
