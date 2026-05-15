package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.Coupons;
import com.example.backend.service.CouponsService;
import com.example.backend.mapper.CouponsMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【coupons(优惠券表)】的数据库操作Service实现
* @createDate 2026-03-03 11:26:16
*/
@Service
public class CouponsServiceImpl extends ServiceImpl<CouponsMapper, Coupons>
    implements CouponsService{

}




