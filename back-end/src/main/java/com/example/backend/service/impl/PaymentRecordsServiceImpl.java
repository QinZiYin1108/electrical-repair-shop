package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.PaymentRecords;
import com.example.backend.service.PaymentRecordsService;
import com.example.backend.mapper.PaymentRecordsMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【payment_records(支付记录表)】的数据库操作Service实现
* @createDate 2026-03-03 11:26:16
*/
@Service
public class PaymentRecordsServiceImpl extends ServiceImpl<PaymentRecordsMapper, PaymentRecords>
    implements PaymentRecordsService{

}




