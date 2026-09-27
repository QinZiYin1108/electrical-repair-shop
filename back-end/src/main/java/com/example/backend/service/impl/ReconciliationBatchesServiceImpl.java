package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.ReconciliationBatches;
import com.example.backend.mapper.ReconciliationBatchesMapper;
import com.example.backend.service.ReconciliationBatchesService;
import org.springframework.stereotype.Service;

@Service
public class ReconciliationBatchesServiceImpl
        extends ServiceImpl<ReconciliationBatchesMapper, ReconciliationBatches>
        implements ReconciliationBatchesService {}
