package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.Stores;
import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.TechnicianAccountsMapper;
import com.example.backend.service.StoresService;
import com.example.backend.service.TechnicianAccountsService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class TechnicianAccountsServiceImpl extends ServiceImpl<TechnicianAccountsMapper, TechnicianAccounts>
    implements TechnicianAccountsService {

    @Resource
    private StoresService storesService;

    @Override
    public void bindStore(String technicianId, String storeId) {
        TechnicianAccounts tech = getById(technicianId);
        if (tech == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "师傅不存在");
        }
        Stores store = storesService.getById(storeId);
        if (store == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "门店不存在");
        }
        tech.setStoreId(storeId);
        updateById(tech);
    }

    @Override
    public void unbindStore(String technicianId) {
        TechnicianAccounts tech = getById(technicianId);
        if (tech == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "师傅不存在");
        }
        tech.setStoreId(null);
        updateById(tech);
    }

    @Override
    public boolean canAcceptOrder(String technicianId) {
        TechnicianAccounts tech = getById(technicianId);
        if (tech == null) {
            return false;
        }
        if (tech.getStoreId() == null) {
            return false;
        }
        return storesService.canAcceptOrder(tech.getStoreId());
    }
}




