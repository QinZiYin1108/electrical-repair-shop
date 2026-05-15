package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.OrderDoorQrCodes;
import com.example.backend.mapper.OrderDoorQrCodesMapper;
import com.example.backend.service.OrderDoorQrCodesService;
import org.springframework.stereotype.Service;

@Service
public class OrderDoorQrCodesServiceImpl extends ServiceImpl<OrderDoorQrCodesMapper, OrderDoorQrCodes>
    implements OrderDoorQrCodesService {
}
