package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.Stores;
import com.example.backend.entity.TechnicianBindings;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.StoresMapper;
import com.example.backend.mapper.TechnicianBindingsMapper;
import com.example.backend.service.SystemMessagesService;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.service.TechnicianBindingsService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TechnicianBindingsServiceImpl
        extends ServiceImpl<TechnicianBindingsMapper, TechnicianBindings>
        implements TechnicianBindingsService {

    private final SystemMessagesService systemMessagesService;
    private final StoresMapper storesMapper;
    private final TechnicianAccountsService technicianAccountsService;

    public TechnicianBindingsServiceImpl(
            SystemMessagesService systemMessagesService,
            StoresMapper storesMapper,
            TechnicianAccountsService technicianAccountsService) {
        this.systemMessagesService = systemMessagesService;
        this.storesMapper = storesMapper;
        this.technicianAccountsService = technicianAccountsService;
    }

    @Override
    @Transactional
    public TechnicianBindings invite(String storeId, String technicianId) {
        // 检查是否已有待确认或已绑定的记录
        TechnicianBindings existing =
                getOne(
                        new LambdaQueryWrapper<TechnicianBindings>()
                                .eq(TechnicianBindings::getTechnicianId, technicianId)
                                .in(TechnicianBindings::getStatus, 1, 2, 3)
                                .eq(TechnicianBindings::getIsDelete, 0));
        if (existing != null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该师傅已有绑定记录、待确认邀请或解绑申请中");
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

        // 发送系统消息通知师傅
        Stores store = storesMapper.selectById(storeId);
        String storeName = store != null ? store.getName() : "未知门店";
        systemMessagesService.createSystemMessage(
                technicianId,
                2,
                "门店邀请通知",
                storeName + "邀请您加入门店，请前往确认",
                4,
                "STORE_INVITE_TECHNICIAN",
                binding.getId(),
                2);

        return binding;
    }

    @Override
    @Transactional
    public TechnicianBindings accept(String technicianId) {
        TechnicianBindings binding =
                getOne(
                        new LambdaQueryWrapper<TechnicianBindings>()
                                .eq(TechnicianBindings::getTechnicianId, technicianId)
                                .eq(TechnicianBindings::getStatus, 1)
                                .eq(TechnicianBindings::getIsDelete, 0));
        if (binding == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "没有待确认的邀请");
        }
        binding.setStatus(2);
        binding.setConfirmedTime(System.currentTimeMillis());
        binding.setUpdatedTime(System.currentTimeMillis());
        updateById(binding);

        // 将师傅账号绑定到门店
        technicianAccountsService.bindStore(technicianId, binding.getStoreId());

        return binding;
    }

    @Override
    @Transactional
    public TechnicianBindings reject(String technicianId) {
        TechnicianBindings binding =
                getOne(
                        new LambdaQueryWrapper<TechnicianBindings>()
                                .eq(TechnicianBindings::getTechnicianId, technicianId)
                                .eq(TechnicianBindings::getStatus, 1)
                                .eq(TechnicianBindings::getIsDelete, 0));
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
        TechnicianBindings binding =
                getOne(
                        new LambdaQueryWrapper<TechnicianBindings>()
                                .eq(TechnicianBindings::getTechnicianId, technicianId)
                                .eq(TechnicianBindings::getStatus, 2)
                                .eq(TechnicianBindings::getIsDelete, 0));
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
        TechnicianBindings binding =
                getOne(
                        new LambdaQueryWrapper<TechnicianBindings>()
                                .eq(TechnicianBindings::getStoreId, storeId)
                                .eq(TechnicianBindings::getTechnicianId, technicianId)
                                .in(TechnicianBindings::getStatus, 2, 3)
                                .eq(TechnicianBindings::getIsDelete, 0));
        if (binding == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "没有该师傅的绑定记录");
        }
        binding.setStatus(4);
        binding.setUpdatedTime(System.currentTimeMillis());
        updateById(binding);

        // 清除师傅账号的门店绑定
        technicianAccountsService.unbindStore(technicianId);
    }

    @Override
    @Transactional
    public void approveUnbind(String storeId, String technicianId) {
        TechnicianBindings binding =
                getOne(
                        new LambdaQueryWrapper<TechnicianBindings>()
                                .eq(TechnicianBindings::getStoreId, storeId)
                                .eq(TechnicianBindings::getTechnicianId, technicianId)
                                .eq(TechnicianBindings::getStatus, 3)
                                .eq(TechnicianBindings::getIsDelete, 0));
        if (binding == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "没有待审批的解绑申请");
        }
        binding.setStatus(4);
        binding.setUpdatedTime(System.currentTimeMillis());
        updateById(binding);

        // 清除师傅账号的门店绑定
        technicianAccountsService.unbindStore(technicianId);
    }

    @Override
    public List<TechnicianBindings> listByStore(String storeId, Integer status) {
        LambdaQueryWrapper<TechnicianBindings> wrapper =
                new LambdaQueryWrapper<TechnicianBindings>()
                        .eq(TechnicianBindings::getStoreId, storeId)
                        .eq(TechnicianBindings::getIsDelete, 0);
        if (status != null) {
            wrapper.eq(TechnicianBindings::getStatus, status);
        }
        wrapper.orderByDesc(TechnicianBindings::getCreatedTime);
        return list(wrapper);
    }
}
