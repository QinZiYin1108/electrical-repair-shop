package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.UserCoupons;
import com.example.backend.service.UserCouponsService;
import com.example.backend.mapper.UserCouponsMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【user_coupons(用户优惠券表)】的数据库操作Service实现
* @createDate 2026-03-03 11:26:16
*/
@Service
public class UserCouponsServiceImpl extends ServiceImpl<UserCouponsMapper, UserCoupons>
    implements UserCouponsService{

}




