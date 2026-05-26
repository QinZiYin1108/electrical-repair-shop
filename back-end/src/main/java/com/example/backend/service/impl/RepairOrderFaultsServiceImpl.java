package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.RepairOrderFaults;
import com.example.backend.service.RepairOrderFaultsService;
import com.example.backend.mapper.RepairOrderFaultsMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【repair_order_faults(维修订单故障记录表)】的数据库操作Service实现
* @createDate 2026-03-03 11:26:16
*/
@Service
public class RepairOrderFaultsServiceImpl extends ServiceImpl<RepairOrderFaultsMapper, RepairOrderFaults>
    implements RepairOrderFaultsService{

}




