package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.ProductOrders;
import com.example.backend.service.ProductOrdersService;
import com.example.backend.mapper.ProductOrdersMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【product_orders(商品订单表)】的数据库操作Service实现
* @createDate 2026-03-03 11:26:16
*/
@Service
public class ProductOrdersServiceImpl extends ServiceImpl<ProductOrdersMapper, ProductOrders>
    implements ProductOrdersService{

}




