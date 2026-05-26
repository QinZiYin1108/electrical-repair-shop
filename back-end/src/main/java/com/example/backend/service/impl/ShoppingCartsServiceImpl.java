package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.ShoppingCarts;
import com.example.backend.service.ShoppingCartsService;
import com.example.backend.mapper.ShoppingCartsMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【shopping_carts(购物车表)】的数据库操作Service实现
* @createDate 2026-03-03 11:26:16
*/
@Service
public class ShoppingCartsServiceImpl extends ServiceImpl<ShoppingCartsMapper, ShoppingCarts>
    implements ShoppingCartsService{

}




