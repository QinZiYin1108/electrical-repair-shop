package com.example.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.backend.entity.FundFlows;
import com.example.backend.mapper.FundFlowsMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class FundFlowsServiceImplTests {

    @Test
    void generatesDeterministicIdempotencyKeyBeforeInsert() {
        FundFlowsMapper mapper = mock(FundFlowsMapper.class);
        when(mapper.insert(any(FundFlows.class))).thenReturn(1);
        FundFlowsServiceImpl service = new FundFlowsServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);

        FundFlows flow = new FundFlows();
        flow.setAccountType(1);
        flow.setAccountId("U1");
        flow.setBusinessType("REPAIR_ORDER_PREPAY");
        flow.setBusinessId("RO1");

        service.save(flow);

        assertEquals("1:U1:REPAIR_ORDER_PREPAY:RO1", flow.getIdempotencyKey());
    }
}
