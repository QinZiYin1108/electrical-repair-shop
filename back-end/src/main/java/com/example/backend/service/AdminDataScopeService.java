package com.example.backend.service;

import com.example.backend.entity.AfterSalesApplications;
import com.example.backend.entity.RepairOrders;
import com.example.backend.entity.Reviews;
import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.security.model.LoginUserInfo;

public interface AdminDataScopeService {

    void requireStoreAccess(LoginUserInfo admin, String storeId);

    void requireTechnicianAccess(LoginUserInfo admin, TechnicianAccounts technician);

    void requireRepairOrderAccess(LoginUserInfo admin, RepairOrders order);

    void requireReviewAccess(LoginUserInfo admin, Reviews review);

    void requireAfterSalesAccess(LoginUserInfo admin, AfterSalesApplications application);
}
