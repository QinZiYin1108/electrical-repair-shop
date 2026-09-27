package com.example.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.entity.Stores;

public interface StoresService extends IService<Stores> {

    /** 创建门店 */
    Stores createStore(Stores store, String operatorId);

    /** 更新门店信息 */
    Stores updateStore(Stores store, String operatorId);

    /** 审核门店 */
    void auditStore(String storeId, Integer auditStatus, String remark, String operatorId);

    /** 切换营业状态 */
    void toggleBusinessStatus(String storeId, Integer businessStatus, String operatorId);

    /** 判断门店是否可以接单（营业中 + 审核通过） */
    boolean canAcceptOrder(String storeId);

    /** 重新计算门店评分（统计已完成且已评价的门店订单评分的平均值） */
    void recalculateStoreRating(String storeId);
}
