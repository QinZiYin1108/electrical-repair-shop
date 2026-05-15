package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.Products;
import com.example.backend.service.ProductsService;
import com.example.backend.mapper.ProductsMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【products(商品表)】的数据库操作Service实现
* @createDate 2026-03-03 11:26:16
*/
@Service
public class ProductsServiceImpl extends ServiceImpl<ProductsMapper, Products>
    implements ProductsService{

}




