package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

/** 资金对账批次。 @TableName reconciliation_batches */
@TableName("reconciliation_batches")
@Data
public class ReconciliationBatches {
    /** 主键，RB+雪花ID */
    @TableId private String id;

    /** 批次号 */
    private String batchNo;

    /** 批次类型：1-增量，2-全量 */
    private Integer batchType;

    /** 状态：1-正常，2-存在异常，3-执行失败 */
    private Integer status;

    /** 扫描窗口开始时间戳 */
    private Long windowStart;

    /** 扫描窗口结束时间戳 */
    private Long windowEnd;

    /** 扫描对象数 */
    private Integer scannedCount;

    /** 异常数 */
    private Integer issueCount;

    /** 错误摘要 */
    private String errorSummary;

    /** 创建时间戳 */
    private Long createdTime;

    /** 更新时间戳 */
    private Long updatedTime;

    /** 乐观锁版本号 */
    @Version private Integer version;

    /** 逻辑删除：0-未删除，1-已删除 */
    @TableLogic private Integer isDelete;
}
