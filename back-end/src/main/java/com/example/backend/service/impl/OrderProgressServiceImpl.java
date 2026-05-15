package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.OrderProgress;
import com.example.backend.service.OrderProgressService;
import com.example.backend.mapper.OrderProgressMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【order_progress(订单进度表)】的数据库操作Service实现
* @createDate 2026-03-03 11:26:16
*/
@Service
public class OrderProgressServiceImpl extends ServiceImpl<OrderProgressMapper, OrderProgress>
    implements OrderProgressService{

}




