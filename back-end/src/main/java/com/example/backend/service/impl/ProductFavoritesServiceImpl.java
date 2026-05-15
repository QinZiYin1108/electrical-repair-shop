package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.ProductFavorites;
import com.example.backend.service.ProductFavoritesService;
import com.example.backend.mapper.ProductFavoritesMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【product_favorites(商品收藏表)】的数据库操作Service实现
* @createDate 2026-03-03 11:26:16
*/
@Service
public class ProductFavoritesServiceImpl extends ServiceImpl<ProductFavoritesMapper, ProductFavorites>
    implements ProductFavoritesService{

}




