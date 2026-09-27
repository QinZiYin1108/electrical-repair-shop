package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/** 处罚记录表 @TableName penalty_records */
@TableName(value = "penalty_records")
@Data
public class PenaltyRecords {

    /** 主键，PN+雪花ID */
    @TableId private String id;

    /** 被处罚账号ID */
    private String accountId;

    /** 账号类型：1-用户，2-师傅，3-门店管理员 */
    private Integer accountType;

    /** 关联举报ID（可为空，手动处罚时无关联举报） */
    private String reportId;

    /** 违规等级：1-一级，2-二级，3-三级，4-四级 */
    private Integer violationLevel;

    /** 扣分分值 */
    private Integer scoreDeducted;

    /** 处罚类型：1-警告，2-功能限制，3-封禁 */
    private Integer penaltyType;

    /** 被限制的功能JSON */
    private String restrictedFunctions;

    /** 封禁时长（小时），NULL为永久 */
    private Integer banDurationHours;

    /** 封禁开始时间戳 */
    private Long banStartTime;

    /** 封禁结束时间戳 */
    private Long banEndTime;

    /** 首次因处罚冻结前的账号状态；原本已冻结时为空，防止误解封 */
    private Integer previousAccountStatus;

    /** 状态：1-执行中，2-已解除，3-已过期 */
    private Integer status;

    /** 操作人ID */
    private String operatorId;

    /** 备注 */
    private String remark;

    /** 申诉状态：0-未申诉，1-申诉中，2-申诉通过，3-申诉驳回 */
    private Integer appealStatus;

    /** 申诉原因 */
    private String appealReason;

    /** 申诉时间戳 */
    private Long appealTime;

    /** 申诉结果 */
    private String appealResult;

    /** 创建时间戳 */
    private Long createdTime;

    /** 更新时间戳 */
    private Long updatedTime;

    /** 乐观锁版本号 */
    @Version private Integer version;

    /** 逻辑删除：0-未删除，1-已删除 */
    @TableLogic private Integer isDelete;
}
