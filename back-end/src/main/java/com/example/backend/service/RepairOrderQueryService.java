package com.example.backend.service;

import com.example.backend.entity.RepairOrderPayments;
import com.example.backend.entity.RepairOrders;
import com.example.backend.entity.ServiceTypes;

public interface RepairOrderQueryService {

    RepairOrders requireOrder(String orderId);

    RepairOrders requireUserOrder(String orderId, String accountId);

    RepairOrders requireTechnicianOrder(String orderId, String technicianAccountId);

    RepairOrderPayments requirePayment(String orderId);

    ServiceTypes requireServiceType(String serviceTypeId);
}
