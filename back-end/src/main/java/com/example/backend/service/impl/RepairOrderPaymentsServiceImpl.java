package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.RepairOrderPayments;
import com.example.backend.service.RepairOrderPaymentsService;
import com.example.backend.mapper.RepairOrderPaymentsMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【repair_order_payments(维修订单支付表)】的数据库操作Service实现
* @createDate 2026-03-03 11:26:16
*/
@Service
public class RepairOrderPaymentsServiceImpl extends ServiceImpl<RepairOrderPaymentsMapper, RepairOrderPayments>
    implements RepairOrderPaymentsService{

}




