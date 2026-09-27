package com.example.backend.service.impl;

import com.example.backend.common.ErrorCode;
import com.example.backend.entity.AfterSalesApplications;
import com.example.backend.entity.Products;
import com.example.backend.entity.RepairOrders;
import com.example.backend.entity.Reviews;
import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.exception.BusinessException;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.AdminDataScopeService;
import com.example.backend.service.ProductsService;
import com.example.backend.service.RepairOrdersService;
import com.example.backend.service.TechnicianAccountsService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminDataScopeServiceImpl implements AdminDataScopeService {

    private final TechnicianAccountsService technicianAccountsService;
    private final RepairOrdersService repairOrdersService;
    private final ProductsService productsService;

    public AdminDataScopeServiceImpl(
            TechnicianAccountsService technicianAccountsService,
            RepairOrdersService repairOrdersService,
            ProductsService productsService) {
        this.technicianAccountsService = technicianAccountsService;
        this.repairOrdersService = repairOrdersService;
        this.productsService = productsService;
    }

    @Override
    public void requireStoreAccess(LoginUserInfo admin, String storeId) {
        requireAdmin(admin);
        if (admin.isSuperAdmin()) {
            return;
        }
        if (!admin.isStoreAdmin()
                || !StringUtils.hasText(admin.getStoreId())
                || !admin.getStoreId().equals(storeId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问其他门店的数据");
        }
    }

    @Override
    public void requireTechnicianAccess(LoginUserInfo admin, TechnicianAccounts technician) {
        if (technician == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "师傅不存在");
        }
        requireStoreAccess(admin, technician.getStoreId());
    }

    @Override
    public void requireRepairOrderAccess(LoginUserInfo admin, RepairOrders order) {
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        TechnicianAccounts technician =
                technicianAccountsService.getById(order.getTechnicianAccountId());
        requireTechnicianAccess(admin, technician);
    }

    @Override
    public void requireReviewAccess(LoginUserInfo admin, Reviews review) {
        if (review == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评价不存在");
        }
        if (Integer.valueOf(1).equals(review.getTargetType())) {
            requireTechnicianAccess(admin, technicianAccountsService.getById(review.getTargetId()));
            return;
        }
        if (Integer.valueOf(2).equals(review.getTargetType())) {
            Products product = productsService.getById(review.getTargetId());
            if (product == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在");
            }
            requireStoreAccess(admin, product.getStoreId());
            return;
        }
        if (Integer.valueOf(3).equals(review.getTargetType())) {
            requireStoreAccess(admin, review.getTargetId());
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "无法确认评价的数据归属");
    }

    @Override
    public void requireAfterSalesAccess(LoginUserInfo admin, AfterSalesApplications application) {
        if (application == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "售后申请不存在");
        }
        if (!Integer.valueOf(1).equals(application.getOrderType())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无法确认售后申请的数据归属");
        }
        requireRepairOrderAccess(admin, repairOrdersService.getById(application.getOrderId()));
    }

    private void requireAdmin(LoginUserInfo admin) {
        if (admin == null || admin.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问管理员资源");
        }
    }
}
