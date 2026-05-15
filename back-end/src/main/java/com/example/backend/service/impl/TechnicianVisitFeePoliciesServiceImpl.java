package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.TechnicianVisitFeePolicies;
import com.example.backend.mapper.TechnicianVisitFeePoliciesMapper;
import com.example.backend.service.TechnicianVisitFeePoliciesService;
import org.springframework.stereotype.Service;

@Service
public class TechnicianVisitFeePoliciesServiceImpl
    extends ServiceImpl<TechnicianVisitFeePoliciesMapper, TechnicianVisitFeePolicies>
    implements TechnicianVisitFeePoliciesService {
}
