package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

/** 资金对账异常明细。 @TableName reconciliation_issues */
@TableName("reconciliation_issues")
@Data
public class ReconciliationIssues {
    /** 主键，RI+雪花ID */
    @TableId private String id;

    /** 所属批次ID */
    private String batchId;

    /** 异常分类 */
    private String category;

    /** 业务对象类型 */
    private String bizType;

    /** 业务对象ID */
    private String bizId;

    /** 异常描述 */
    private String message;

    /** 处理状态：1-待处理，2-已处理，3-已忽略 */
    private Integer status;

    /** 处理人账号ID */
    private String handledBy;

    /** 处理时间戳 */
    private Long handledTime;

    /** 处理备注 */
    private String handleRemark;

    /** 创建时间戳 */
    private Long createdTime;

    /** 更新时间戳 */
    private Long updatedTime;

    /** 乐观锁版本号 */
    @Version private Integer version;

    /** 逻辑删除：0-未删除，1-已删除 */
    @TableLogic private Integer isDelete;
}
