package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.Stores;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.StoresMapper;
import com.example.backend.service.StoresService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoresServiceImpl extends ServiceImpl<StoresMapper, Stores>
        implements StoresService {

    @Override
    @Transactional
    public Stores createStore(Stores store, String operatorId) {
        Long count = baseMapper.selectCount(
                new LambdaQueryWrapper<Stores>()
                        .eq(Stores::getName, store.getName())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_KEY, "门店名称已存在");
        }

        store.setId(SnowflakeIdUtil.nextStoreId());
        long now = System.currentTimeMillis();
        store.setCreatedTime(now);
        store.setUpdatedTime(now);
        store.setAuditStatus(1);
        store.setBusinessStatus(1);
        store.setRatingCount(0);
        store.setIsOnline(1);

        save(store);
        return store;
    }

    @Override
    @Transactional
    public Stores updateStore(Stores store, String operatorId) {
        Stores existing = getById(store.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "门店不存在");
        }

        Long count = baseMapper.selectCount(
                new LambdaQueryWrapper<Stores>()
                        .eq(Stores::getName, store.getName())
                        .ne(Stores::getId, store.getId())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_KEY, "门店名称已存在");
        }

        store.setUpdatedTime(System.currentTimeMillis());
        updateById(store);

        return getById(store.getId());
    }

    @Override
    @Transactional
    public void auditStore(String storeId, Integer auditStatus, String remark, String operatorId) {
        Stores store = getById(storeId);
        if (store == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "门店不存在");
        }
        if (store.getAuditStatus() != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该门店已审核，不能重复审核");
        }

        store.setAuditStatus(auditStatus);
        store.setAuditRemark(remark);
        store.setAuditTime(System.currentTimeMillis());
        store.setUpdatedTime(System.currentTimeMillis());
        updateById(store);
    }

    @Override
    @Transactional
    public void toggleBusinessStatus(String storeId, Integer businessStatus, String operatorId) {
        Stores store = getById(storeId);
        if (store == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "门店不存在");
        }

        store.setBusinessStatus(businessStatus);
        store.setUpdatedTime(System.currentTimeMillis());
        updateById(store);
    }

    @Override
    public boolean canAcceptOrder(String storeId) {
        if (storeId == null) {
            return false;
        }
        Stores store = getById(storeId);
        if (store == null) {
            return false;
        }
        return store.getBusinessStatus() == 1 && store.getAuditStatus() == 2;
    }
}
