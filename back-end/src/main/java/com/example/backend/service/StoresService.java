package com.example.backend.service;

import com.example.backend.entity.Stores;
import com.baomidou.mybatisplus.extension.service.IService;

public interface StoresService extends IService<Stores> {

    /**
     * 创建门店
     */
    Stores createStore(Stores store, String operatorId);

    /**
     * 更新门店信息
     */
    Stores updateStore(Stores store, String operatorId);

    /**
     * 审核门店
     */
    void auditStore(String storeId, Integer auditStatus, String remark, String operatorId);

    /**
     * 切换营业状态
     */
    void toggleBusinessStatus(String storeId, Integer businessStatus, String operatorId);

    /**
     * 判断门店是否可以接单（营业中 + 审核通过）
     */
    boolean canAcceptOrder(String storeId);
}
