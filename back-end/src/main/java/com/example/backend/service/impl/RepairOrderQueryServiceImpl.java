package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.RepairOrderPayments;
import com.example.backend.entity.RepairOrders;
import com.example.backend.entity.ServiceTypes;
import com.example.backend.exception.BusinessException;
import com.example.backend.service.RepairOrderPaymentsService;
import com.example.backend.service.RepairOrderQueryService;
import com.example.backend.service.RepairOrdersService;
import com.example.backend.service.ServiceTypesService;
import org.springframework.stereotype.Service;

@Service
public class RepairOrderQueryServiceImpl implements RepairOrderQueryService {

    private final RepairOrdersService repairOrdersService;
    private final RepairOrderPaymentsService repairOrderPaymentsService;
    private final ServiceTypesService serviceTypesService;

    public RepairOrderQueryServiceImpl(
            RepairOrdersService repairOrdersService,
            RepairOrderPaymentsService repairOrderPaymentsService,
            ServiceTypesService serviceTypesService) {
        this.repairOrdersService = repairOrdersService;
        this.repairOrderPaymentsService = repairOrderPaymentsService;
        this.serviceTypesService = serviceTypesService;
    }

    @Override
    public RepairOrders requireOrder(String orderId) {
        RepairOrders order =
                repairOrdersService.getOne(
                        new LambdaQueryWrapper<RepairOrders>()
                                .eq(RepairOrders::getId, orderId)
                                .eq(RepairOrders::getIsDelete, 0)
                                .last("limit 1"),
                        false);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    @Override
    public RepairOrders requireUserOrder(String orderId, String accountId) {
        RepairOrders order = requireOrder(orderId);
        if (accountId == null || !accountId.equals(order.getAccountId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    @Override
    public RepairOrders requireTechnicianOrder(String orderId, String technicianAccountId) {
        RepairOrders order = requireOrder(orderId);
        if (technicianAccountId == null
                || !technicianAccountId.equals(order.getTechnicianAccountId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    @Override
    public RepairOrderPayments requirePayment(String orderId) {
        RepairOrderPayments payment =
                repairOrderPaymentsService.getOne(
                        new LambdaQueryWrapper<RepairOrderPayments>()
                                .eq(RepairOrderPayments::getRepairOrderId, orderId)
                                .eq(RepairOrderPayments::getIsDelete, 0)
                                .orderByDesc(RepairOrderPayments::getCreatedTime)
                                .last("limit 1"),
                        false);
        if (payment == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "订单支付信息不存在");
        }
        return payment;
    }

    @Override
    public ServiceTypes requireServiceType(String serviceTypeId) {
        ServiceTypes serviceType =
                serviceTypesService.getOne(
                        new LambdaQueryWrapper<ServiceTypes>()
                                .eq(ServiceTypes::getId, serviceTypeId)
                                .eq(ServiceTypes::getIsDelete, 0)
                                .last("limit 1"),
                        false);
        if (serviceType == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "服务类型不存在或已停用");
        }
        return serviceType;
    }
}
