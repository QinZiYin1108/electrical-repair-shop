package com.example.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.entity.CancelReasons;
import java.util.List;
import java.util.Map;

/**
 * @description 针对表【cancel_reasons(订单取消原因表)】的数据库操作Service
 */
public interface CancelReasonsService extends IService<CancelReasons> {

    /**
     * 统计各取消原因数量（按原因编码分组）
     *
     * @param orderType 订单类型：1-维修，2-商品，null-全部
     * @param startTime 开始时间戳，null-不限
     * @param endTime 结束时间戳，null-不限
     */
    List<Map<String, Object>> statsByReason(Integer orderType, Long startTime, Long endTime);
}
