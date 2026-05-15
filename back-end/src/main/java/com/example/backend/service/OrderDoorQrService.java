package com.example.backend.service;

import com.example.backend.entity.OrderDoorQrCodes;
import com.example.backend.entity.RepairOrders;
import com.example.backend.entity.ServiceTypes;
import com.example.backend.model.user.UserOrderDoorQrResponse;
import com.example.backend.model.worker.WorkerDoorQrConsumeResult;

import java.util.List;
import java.util.Map;

public interface OrderDoorQrService {

    void generateForAcceptedOrder(RepairOrders order, ServiceTypes serviceType);

    void invalidateCurrentCodes(String orderId);

    Map<String, OrderDoorQrCodes> getActiveCodeMap(List<String> orderIds);

    UserOrderDoorQrResponse getUserDoorQr(String orderId, String accountId);

    UserOrderDoorQrResponse getDoorQrByToken(String token);

    /**
     * Consume (verify/use) door QR token by technician and advance order status from "waiting visit".
     */
    WorkerDoorQrConsumeResult consumeForTechnician(String token, String technicianAccountId);
}
