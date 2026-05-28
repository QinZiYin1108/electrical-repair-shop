package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.StoreBusinessHours;
import com.example.backend.mapper.StoreBusinessHoursMapper;
import com.example.backend.service.StoreBusinessHoursService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StoreBusinessHoursServiceImpl extends ServiceImpl<StoreBusinessHoursMapper, StoreBusinessHours>
        implements StoreBusinessHoursService {

    @Override
    @Transactional
    public void batchSave(String storeId, List<StoreBusinessHours> hoursList) {
        // 先删除旧数据
        baseMapper.delete(new LambdaQueryWrapper<StoreBusinessHours>()
                .eq(StoreBusinessHours::getStoreId, storeId));

        // 批量插入新数据
        long now = System.currentTimeMillis();
        for (StoreBusinessHours hours : hoursList) {
            hours.setStoreId(storeId);
            hours.setCreatedTime(now);
            hours.setUpdatedTime(now);
        }
        saveBatch(hoursList);
    }

    @Override
    public List<StoreBusinessHours> getByStoreId(String storeId) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<StoreBusinessHours>()
                        .eq(StoreBusinessHours::getStoreId, storeId)
                        .orderByAsc(StoreBusinessHours::getDayOfWeek)
        );
    }
}
