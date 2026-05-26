package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.TechnicianSkills;
import com.example.backend.service.TechnicianSkillsService;
import com.example.backend.mapper.TechnicianSkillsMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【technician_skills(师傅技能表)】的数据库操作Service实现
* @createDate 2026-03-03 11:26:16
*/
@Service
public class TechnicianSkillsServiceImpl extends ServiceImpl<TechnicianSkillsMapper, TechnicianSkills>
    implements TechnicianSkillsService{

}




