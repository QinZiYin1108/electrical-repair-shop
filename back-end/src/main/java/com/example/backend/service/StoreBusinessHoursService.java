package com.example.backend.service;

import com.example.backend.entity.StoreBusinessHours;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface StoreBusinessHoursService extends IService<StoreBusinessHours> {

    /**
     * 批量保存门店营业时间
     */
    void batchSave(String storeId, List<StoreBusinessHours> hoursList);

    /**
     * 查询门店营业时间
     */
    List<StoreBusinessHours> getByStoreId(String storeId);
}
