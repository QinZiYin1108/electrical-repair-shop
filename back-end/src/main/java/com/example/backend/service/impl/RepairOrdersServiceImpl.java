package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.RepairOrders;
import com.example.backend.service.RepairOrdersService;
import com.example.backend.mapper.RepairOrdersMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【repair_orders(维修订单表)】的数据库操作Service实现
* @createDate 2026-03-03 11:26:16
*/
@Service
public class RepairOrdersServiceImpl extends ServiceImpl<RepairOrdersMapper, RepairOrders>
    implements RepairOrdersService{

}




