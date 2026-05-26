package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.UserFollowTechnicians;
import com.example.backend.mapper.UserFollowTechniciansMapper;
import com.example.backend.service.UserFollowTechniciansService;
import org.springframework.stereotype.Service;

@Service
public class UserFollowTechniciansServiceImpl extends ServiceImpl<UserFollowTechniciansMapper, UserFollowTechnicians>
    implements UserFollowTechniciansService {
}

