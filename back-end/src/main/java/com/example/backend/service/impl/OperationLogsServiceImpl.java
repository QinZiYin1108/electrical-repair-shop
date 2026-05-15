package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.OperationLogs;
import com.example.backend.mapper.OperationLogsMapper;
import com.example.backend.service.OperationLogsService;
import org.springframework.stereotype.Service;

@Service
public class OperationLogsServiceImpl extends ServiceImpl<OperationLogsMapper, OperationLogs>
    implements OperationLogsService {
}

