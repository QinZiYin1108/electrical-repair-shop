package com.example.backend.service.impl;

import com.example.backend.common.ErrorCode;
import com.example.backend.domain.order.RepairOrderStateMachine;
import com.example.backend.entity.RepairOrders;
import com.example.backend.exception.BusinessException;
import com.example.backend.service.RepairOrderCommandService;
import com.example.backend.service.RepairOrdersService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepairOrderCommandServiceImpl implements RepairOrderCommandService {

    private final RepairOrdersService repairOrdersService;
    private final RepairOrderStateMachine stateMachine;

    public RepairOrderCommandServiceImpl(
            RepairOrdersService repairOrdersService, RepairOrderStateMachine stateMachine) {
        this.repairOrdersService = repairOrdersService;
        this.stateMachine = stateMachine;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTransition(
            RepairOrders order,
            Integer fromStatus,
            Integer targetStatus,
            long updatedTime,
            String failureMessage) {
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        stateMachine.requireTransition(fromStatus, targetStatus);
        order.setStatus(targetStatus);
        order.setUpdatedTime(updatedTime);
        if (!repairOrdersService.updateById(order)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, failureMessage);
        }
    }
}
