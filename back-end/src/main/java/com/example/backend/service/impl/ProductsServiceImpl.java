package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.Products;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.ProductsMapper;
import com.example.backend.service.ProductsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductsServiceImpl extends ServiceImpl<ProductsMapper, Products>
    implements ProductsService {

    @Override
    @Transactional
    public void freezeProduct(String productId, String operatorId) {
        Products product = getById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在");
        }
        if (product.getIsFrozen() != null && product.getIsFrozen() == 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "商品已冻结");
        }
        product.setIsFrozen(1);
        product.setFrozenTime(System.currentTimeMillis());
        product.setFrozenBy(operatorId);
        product.setUpdatedTime(System.currentTimeMillis());
        updateById(product);
    }

    @Override
    @Transactional
    public void unfreezeProduct(String productId) {
        Products product = getById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在");
        }
        if (product.getIsFrozen() == null || product.getIsFrozen() == 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "商品未被冻结");
        }
        product.setIsFrozen(0);
        product.setFrozenTime(null);
        product.setFrozenBy(null);
        product.setUpdatedTime(System.currentTimeMillis());
        updateById(product);
    }

    @Override
    @Transactional
    public void auditProduct(String productId, Integer auditStatus, String remark) {
        Products product = getById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在");
        }
        if (product.getAuditStatus() != null && product.getAuditStatus() != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该商品已审核");
        }
        product.setAuditStatus(auditStatus);
        product.setUpdatedTime(System.currentTimeMillis());
        updateById(product);
    }

    @Override
    public boolean isPurchasable(String productId) {
        Products product = getById(productId);
        if (product == null) {
            return false;
        }
        boolean notFrozen = product.getIsFrozen() == null || product.getIsFrozen() == 0;
        boolean auditPassed = product.getAuditStatus() != null && product.getAuditStatus() == 2;
        boolean onShelf = product.getStatus() != null && product.getStatus() == 1;
        return notFrozen && auditPassed && onShelf;
    }
}




