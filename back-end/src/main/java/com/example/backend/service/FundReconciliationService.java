package com.example.backend.service;

import com.example.backend.entity.ReconciliationBatches;
import com.example.backend.model.finance.FundReconciliationReport;

public interface FundReconciliationService {

    int BATCH_TYPE_INCREMENTAL = 1;
    int BATCH_TYPE_FULL = 2;

    /** 内存态核对（保留兼容）。 */
    FundReconciliationReport reconcile(long since, long now);

    /** 执行一次对账并持久化批次与异常明细。 */
    ReconciliationBatches runBatch(int batchType, long since, long now, String triggeredBy);

    /** 最近一次对账批次的窗口结束时间，用作增量检查点；无历史返回 null。 */
    Long lastBatchWindowEnd();
}
