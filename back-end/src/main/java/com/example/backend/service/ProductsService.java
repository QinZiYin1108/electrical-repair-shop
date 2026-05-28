package com.example.backend.service;

import com.example.backend.entity.Products;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Administrator
* @description 针对表【products(商品表)】的数据库操作Service
* @createDate 2026-03-03 11:26:16
*/
public interface ProductsService extends IService<Products> {

    /**
     * 冻结商品
     */
    void freezeProduct(String productId, String operatorId);

    /**
     * 解冻商品
     */
    void unfreezeProduct(String productId);

    /**
     * 审核商品
     */
    void auditProduct(String productId, Integer auditStatus, String remark);

    /**
     * 判断商品是否可购买（未冻结 + 审核通过 + 上架）
     */
    boolean isPurchasable(String productId);
}
