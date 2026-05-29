package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.TechnicianBindings;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.TechnicianBindingsMapper;
import com.example.backend.service.TechnicianBindingsService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TechnicianBindingsServiceImpl extends ServiceImpl<TechnicianBindingsMapper, TechnicianBindings>
        implements TechnicianBindingsService {

    @Override
    @Transactional
    public TechnicianBindings invite(String storeId, String technicianId) {
        // 检查是否已有待确认或已绑定的记录
        TechnicianBindings existing = getOne(new LambdaQueryWrapper<TechnicianBindings>()
            .eq(TechnicianBindings::getTechnicianId, technicianId)
            .in(TechnicianBindings::getStatus, 1, 2)
            .eq(TechnicianBindings::getIsDelete, 0)
        );
        if (existing != null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该师傅已有绑定记录或待确认邀请");
        }
        long now = System.currentTimeMillis();
        TechnicianBindings binding = new TechnicianBindings();
        binding.setId(SnowflakeIdUtil.nextTechnicianBindingId());
        binding.setStoreId(storeId);
        binding.setTechnicianId(technicianId);
        binding.setStatus(1);
        binding.setInvitedTime(now);
        binding.setCreatedTime(now);
        binding.setUpdatedTime(now);
        save(binding);
        return binding;
    }

    @Override
    @Transactional
    public TechnicianBindings accept(String technicianId) {
        TechnicianBindings binding = getOne(new LambdaQueryWrapper<TechnicianBindings>()
            .eq(TechnicianBindings::getTechnicianId, technicianId)
            .eq(TechnicianBindings::getStatus, 1)
            .eq(TechnicianBindings::getIsDelete, 0)
        );
        if (binding == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "没有待确认的邀请");
        }
        binding.setStatus(2);
        binding.setConfirmedTime(System.currentTimeMillis());
        binding.setUpdatedTime(System.currentTimeMillis());
        updateById(binding);
        return binding;
    }

    @Override
    @Transactional
    public TechnicianBindings reject(String technicianId) {
        TechnicianBindings binding = getOne(new LambdaQueryWrapper<TechnicianBindings>()
            .eq(TechnicianBindings::getTechnicianId, technicianId)
            .eq(TechnicianBindings::getStatus, 1)
            .eq(TechnicianBindings::getIsDelete, 0)
        );
        if (binding == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "没有待确认的邀请");
        }
        binding.setStatus(4);
        binding.setUpdatedTime(System.currentTimeMillis());
        updateById(binding);
        return binding;
    }

    @Override
    @Transactional
    public TechnicianBindings requestUnbind(String technicianId) {
        TechnicianBindings binding = getOne(new LambdaQueryWrapper<TechnicianBindings>()
            .eq(TechnicianBindings::getTechnicianId, technicianId)
            .eq(TechnicianBindings::getStatus, 2)
            .eq(TechnicianBindings::getIsDelete, 0)
        );
        if (binding == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "没有已绑定的记录");
        }
        binding.setStatus(3);
        binding.setUnbindRequestedTime(System.currentTimeMillis());
        binding.setUpdatedTime(System.currentTimeMillis());
        updateById(binding);
        return binding;
    }

    @Override
    @Transactional
    public void directUnbind(String storeId, String technicianId) {
        TechnicianBindings binding = getOne(new LambdaQueryWrapper<TechnicianBindings>()
            .eq(TechnicianBindings::getStoreId, storeId)
            .eq(TechnicianBindings::getTechnicianId, technicianId)
            .in(TechnicianBindings::getStatus, 2, 3)
            .eq(TechnicianBindings::getIsDelete, 0)
        );
        if (binding == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "没有该师傅的绑定记录");
        }
        binding.setStatus(4);
        binding.setUpdatedTime(System.currentTimeMillis());
        updateById(binding);
    }

    @Override
    @Transactional
    public void approveUnbind(String storeId, String technicianId) {
        TechnicianBindings binding = getOne(new LambdaQueryWrapper<TechnicianBindings>()
            .eq(TechnicianBindings::getStoreId, storeId)
            .eq(TechnicianBindings::getTechnicianId, technicianId)
            .eq(TechnicianBindings::getStatus, 3)
            .eq(TechnicianBindings::getIsDelete, 0)
        );
        if (binding == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "没有待审批的解绑申请");
        }
        binding.setStatus(4);
        binding.setUpdatedTime(System.currentTimeMillis());
        updateById(binding);
    }

    @Override
    public List<TechnicianBindings> listByStore(String storeId, Integer status) {
        LambdaQueryWrapper<TechnicianBindings> wrapper = new LambdaQueryWrapper<TechnicianBindings>()
            .eq(TechnicianBindings::getStoreId, storeId)
            .eq(TechnicianBindings::getIsDelete, 0);
        if (status != null) {
            wrapper.eq(TechnicianBindings::getStatus, status);
        }
        wrapper.orderByDesc(TechnicianBindings::getCreatedTime);
        return list(wrapper);
    }
}
