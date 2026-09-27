package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.SupportTicketLogs;
import com.example.backend.mapper.SupportTicketLogsMapper;
import com.example.backend.service.SupportTicketLogsService;
import org.springframework.stereotype.Service;

@Service
public class SupportTicketLogsServiceImpl
        extends ServiceImpl<SupportTicketLogsMapper, SupportTicketLogs>
        implements SupportTicketLogsService {}
