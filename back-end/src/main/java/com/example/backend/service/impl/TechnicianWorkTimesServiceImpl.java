package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.TechnicianWorkTimes;
import com.example.backend.service.TechnicianWorkTimesService;
import com.example.backend.mapper.TechnicianWorkTimesMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【technician_work_times(师傅工作时间表)】的数据库操作Service实现
* @createDate 2026-03-03 11:26:16
*/
@Service
public class TechnicianWorkTimesServiceImpl extends ServiceImpl<TechnicianWorkTimesMapper, TechnicianWorkTimes>
    implements TechnicianWorkTimesService{

}




