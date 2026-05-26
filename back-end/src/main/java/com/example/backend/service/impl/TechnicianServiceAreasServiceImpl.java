package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.TechnicianServiceAreas;
import com.example.backend.mapper.TechnicianServiceAreasMapper;
import com.example.backend.service.TechnicianServiceAreasService;
import org.springframework.stereotype.Service;

@Service
public class TechnicianServiceAreasServiceImpl
    extends ServiceImpl<TechnicianServiceAreasMapper, TechnicianServiceAreas>
    implements TechnicianServiceAreasService {
}
