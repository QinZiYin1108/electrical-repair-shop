package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.ServiceCategories;
import com.example.backend.service.ServiceCategoriesService;
import com.example.backend.mapper.ServiceCategoriesMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【service_categories(服务类型分类表)】的数据库操作Service实现
* @createDate 2026-03-03 11:26:16
*/
@Service
public class ServiceCategoriesServiceImpl extends ServiceImpl<ServiceCategoriesMapper, ServiceCategories>
    implements ServiceCategoriesService{

}




