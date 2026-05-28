package com.example.backend.service;

import com.example.backend.entity.TechnicianAccounts;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Administrator
* @description 针对表【technician_accounts(师傅账号表)】的数据库操作Service
* @createDate 2026-03-03 11:26:16
*/
public interface TechnicianAccountsService extends IService<TechnicianAccounts> {

    /**
     * 绑定师傅到门店
     */
    void bindStore(String technicianId, String storeId);

    /**
     * 解绑师傅与门店
     */
    void unbindStore(String technicianId);

    /**
     * 判断师傅是否可以接单（已绑定门店 + 门店营业中且审核通过）
     */
    boolean canAcceptOrder(String technicianId);
}
