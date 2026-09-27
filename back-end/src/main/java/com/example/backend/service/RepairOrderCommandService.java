package com.example.backend.service;

import com.example.backend.entity.RepairOrders;

public interface RepairOrderCommandService {

    void saveTransition(
            RepairOrders order,
            Integer fromStatus,
            Integer targetStatus,
            long updatedTime,
            String failureMessage);
}
