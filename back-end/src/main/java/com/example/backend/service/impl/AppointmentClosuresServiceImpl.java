package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.AppointmentClosures;
import com.example.backend.mapper.AppointmentClosuresMapper;
import com.example.backend.service.AppointmentClosuresService;
import org.springframework.stereotype.Service;

@Service
public class AppointmentClosuresServiceImpl
        extends ServiceImpl<AppointmentClosuresMapper, AppointmentClosures>
        implements AppointmentClosuresService {}
