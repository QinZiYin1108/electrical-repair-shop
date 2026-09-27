package com.example.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.entity.FinanceOrderSnapshots;
import com.example.backend.model.finance.FinanceReportResponse;
import java.util.List;

/** 订单财务核算：生成订单财务快照，并按不同时间口径统计报表。 */
public interface OrderFinanceService extends IService<FinanceOrderSnapshots> {

    /** 重算/生成订单财务快照（幂等，按订单类型+订单ID upsert）。 */
    FinanceOrderSnapshots rebuild(int orderType, String orderId);

    /** 查询财务快照。 */
    List<FinanceOrderSnapshots> listSnapshots(Integer orderType, String orderId, int limit);

    /** 按时间口径统计报表。 */
    FinanceReportResponse report(long from, long to, String timeBasis);
}
