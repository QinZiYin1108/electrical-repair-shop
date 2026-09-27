package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.CancelReasons;
import com.example.backend.mapper.CancelReasonsMapper;
import com.example.backend.service.CancelReasonsService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * @description 针对表【cancel_reasons(订单取消原因表)】的数据库操作Service实现
 */
@Service
public class CancelReasonsServiceImpl extends ServiceImpl<CancelReasonsMapper, CancelReasons>
        implements CancelReasonsService {

    @Override
    public List<Map<String, Object>> statsByReason(
            Integer orderType, Long startTime, Long endTime) {
        LambdaQueryWrapper<CancelReasons> wrapper = new LambdaQueryWrapper<>();
        if (orderType != null) {
            wrapper.eq(CancelReasons::getOrderType, orderType);
        }
        if (startTime != null) {
            wrapper.ge(CancelReasons::getCreatedTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(CancelReasons::getCreatedTime, endTime);
        }
        wrapper.select(CancelReasons::getReasonCode, CancelReasons::getReasonLabel);

        List<CancelReasons> records = list(wrapper);

        // 手动分组统计（避免 XML 依赖）
        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (CancelReasons r : records) {
            String code = r.getReasonCode() != null ? r.getReasonCode() : "other";
            grouped.computeIfAbsent(
                    code,
                    k -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("reasonCode", code);
                        m.put("reasonLabel", r.getReasonLabel());
                        m.put("count", 0L);
                        return m;
                    });
            @SuppressWarnings("unchecked")
            Map<String, Object> entry = (Map<String, Object>) grouped.get(code);
            entry.put("count", ((Long) entry.get("count")) + 1);
        }
        return new ArrayList<>(grouped.values());
    }
}
