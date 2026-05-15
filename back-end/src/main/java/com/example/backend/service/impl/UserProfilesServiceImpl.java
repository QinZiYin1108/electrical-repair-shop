package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.UserProfiles;
import com.example.backend.service.UserProfilesService;
import com.example.backend.mapper.UserProfilesMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【user_profiles(用户信息表)】的数据库操作Service实现
* @createDate 2026-03-03 11:26:16
*/
@Service
public class UserProfilesServiceImpl extends ServiceImpl<UserProfilesMapper, UserProfiles>
    implements UserProfilesService{

}




