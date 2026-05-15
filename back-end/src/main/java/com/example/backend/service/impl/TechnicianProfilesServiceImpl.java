package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.TechnicianProfiles;
import com.example.backend.service.TechnicianProfilesService;
import com.example.backend.mapper.TechnicianProfilesMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【technician_profiles(师傅信息表)】的数据库操作Service实现
* @createDate 2026-03-03 11:26:16
*/
@Service
public class TechnicianProfilesServiceImpl extends ServiceImpl<TechnicianProfilesMapper, TechnicianProfiles>
    implements TechnicianProfilesService{

}




