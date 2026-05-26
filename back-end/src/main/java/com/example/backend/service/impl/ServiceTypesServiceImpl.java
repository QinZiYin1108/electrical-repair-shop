package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.ServiceTypes;
import com.example.backend.service.ServiceTypesService;
import com.example.backend.mapper.ServiceTypesMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【service_types(服务类型表)】的数据库操作Service实现
* @createDate 2026-03-03 11:26:16
*/
@Service
public class ServiceTypesServiceImpl extends ServiceImpl<ServiceTypesMapper, ServiceTypes>
    implements ServiceTypesService{

}




