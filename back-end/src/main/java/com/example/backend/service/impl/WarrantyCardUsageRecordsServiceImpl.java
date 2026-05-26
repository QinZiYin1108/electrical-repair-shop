package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.WarrantyCardUsageRecords;
import com.example.backend.mapper.WarrantyCardUsageRecordsMapper;
import com.example.backend.service.WarrantyCardUsageRecordsService;
import org.springframework.stereotype.Service;

@Service
public class WarrantyCardUsageRecordsServiceImpl extends ServiceImpl<WarrantyCardUsageRecordsMapper, WarrantyCardUsageRecords>
    implements WarrantyCardUsageRecordsService {
}
