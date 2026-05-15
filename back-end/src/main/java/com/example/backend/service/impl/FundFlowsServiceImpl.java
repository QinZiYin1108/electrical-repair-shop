package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.FundFlows;
import com.example.backend.service.FundFlowsService;
import com.example.backend.mapper.FundFlowsMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【fund_flows(资金流水表)】的数据库操作Service实现
* @createDate 2026-03-03 11:26:16
*/
@Service
public class FundFlowsServiceImpl extends ServiceImpl<FundFlowsMapper, FundFlows>
    implements FundFlowsService{

}




