package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.FaultPhenomena;
import com.example.backend.service.FaultPhenomenaService;
import com.example.backend.mapper.FaultPhenomenaMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【fault_phenomena(故障现象表)】的数据库操作Service实现
* @createDate 2026-03-03 11:26:16
*/
@Service
public class FaultPhenomenaServiceImpl extends ServiceImpl<FaultPhenomenaMapper, FaultPhenomena>
    implements FaultPhenomenaService{

}




