package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.ProductCategories;
import com.example.backend.service.ProductCategoriesService;
import com.example.backend.mapper.ProductCategoriesMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【product_categories(商品分类表)】的数据库操作Service实现
* @createDate 2026-03-03 11:26:16
*/
@Service
public class ProductCategoriesServiceImpl extends ServiceImpl<ProductCategoriesMapper, ProductCategories>
    implements ProductCategoriesService{

}




