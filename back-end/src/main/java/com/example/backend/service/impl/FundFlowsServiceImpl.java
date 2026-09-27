package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.FundFlows;
import com.example.backend.mapper.FundFlowsMapper;
import com.example.backend.service.FundFlowsService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @author Administrator
 * @description 针对表【fund_flows(资金流水表)】的数据库操作Service实现
 * @createDate 2026-03-03 11:26:16
 */
@Service
public class FundFlowsServiceImpl extends ServiceImpl<FundFlowsMapper, FundFlows>
        implements FundFlowsService {

    @Override
    public boolean save(FundFlows entity) {
        if (entity != null && !StringUtils.hasText(entity.getIdempotencyKey())) {
            entity.setIdempotencyKey(buildIdempotencyKey(entity));
        }
        return super.save(entity);
    }

    private String buildIdempotencyKey(FundFlows flow) {
        return String.join(
                ":",
                safe(flow.getAccountType()),
                safe(flow.getAccountId()),
                safe(flow.getBusinessType()),
                safe(flow.getBusinessId()));
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
