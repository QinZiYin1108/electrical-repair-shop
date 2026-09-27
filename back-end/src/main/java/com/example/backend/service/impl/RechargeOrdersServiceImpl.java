package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.RechargeOrders;
import com.example.backend.mapper.RechargeOrdersMapper;
import com.example.backend.service.RechargeOrdersService;
import org.springframework.stereotype.Service;

@Service
public class RechargeOrdersServiceImpl extends ServiceImpl<RechargeOrdersMapper, RechargeOrders>
        implements RechargeOrdersService {}
