package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.Reviews;
import com.example.backend.entity.Stores;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.ReviewsMapper;
import com.example.backend.mapper.StoresMapper;
import com.example.backend.service.StoresService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoresServiceImpl extends ServiceImpl<StoresMapper, Stores> implements StoresService {

    private final ReviewsMapper reviewsMapper;

    public StoresServiceImpl(ReviewsMapper reviewsMapper) {
        this.reviewsMapper = reviewsMapper;
    }

    @Override
    @Transactional
    public Stores createStore(Stores store, String operatorId) {
        Long count =
                baseMapper.selectCount(
                        new LambdaQueryWrapper<Stores>().eq(Stores::getName, store.getName()));
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

        Long count =
                baseMapper.selectCount(
                        new LambdaQueryWrapper<Stores>()
                                .eq(Stores::getName, store.getName())
                                .ne(Stores::getId, store.getId()));
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

    @Override
    @Transactional
    public void recalculateStoreRating(String storeId) {
        Stores store = getById(storeId);
        if (store == null) {
            return;
        }

        // 查询该门店下所有正常状态的门店评价（targetType=3）
        List<Reviews> reviews =
                reviewsMapper.selectList(
                        new LambdaQueryWrapper<Reviews>()
                                .eq(Reviews::getTargetId, storeId)
                                .eq(Reviews::getTargetType, 3)
                                .eq(Reviews::getStatus, 1)
                                .eq(Reviews::getIsDelete, 0));

        store.setRatingCount(reviews == null ? 0 : reviews.size());
        if (reviews != null && !reviews.isEmpty()) {
            BigDecimal total = BigDecimal.ZERO;
            for (Reviews review : reviews) {
                if (review.getRating() != null) {
                    total = total.add(BigDecimal.valueOf(review.getRating()));
                }
            }
            BigDecimal avg =
                    total.divide(BigDecimal.valueOf(reviews.size()), 1, RoundingMode.HALF_UP);
            store.setRating(avg);
        } else {
            store.setRating(null);
        }
        store.setUpdatedTime(System.currentTimeMillis());
        updateById(store);
    }
}
