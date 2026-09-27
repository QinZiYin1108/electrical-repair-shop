package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.ReconciliationIssues;
import com.example.backend.mapper.ReconciliationIssuesMapper;
import com.example.backend.service.ReconciliationIssuesService;
import org.springframework.stereotype.Service;

@Service
public class ReconciliationIssuesServiceImpl
        extends ServiceImpl<ReconciliationIssuesMapper, ReconciliationIssues>
        implements ReconciliationIssuesService {}
