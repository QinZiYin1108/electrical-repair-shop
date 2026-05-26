package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.AdminProfiles;
import com.example.backend.service.AdminProfilesService;
import com.example.backend.mapper.AdminProfilesMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【admin_profiles(管理员信息表)】的数据库操作Service实现
* @createDate 2026-03-03 11:26:15
*/
@Service
public class AdminProfilesServiceImpl extends ServiceImpl<AdminProfilesMapper, AdminProfiles>
    implements AdminProfilesService{

}




