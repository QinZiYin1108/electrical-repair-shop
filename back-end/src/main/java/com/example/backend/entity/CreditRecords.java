package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/** 信用积分记录表 @TableName credit_records */
@TableName(value = "credit_records")
@Data
public class CreditRecords {

    /** 主键，CDR+雪花ID */
    @TableId private String id;

    /** 账号ID */
    private String accountId;

    /** 账号类型：1-用户，2-师傅，3-门店管理员 */
    private Integer accountType;

    /** 变动类型：1-违规扣分，2-自动恢复，3-举报奖励，4-完单恢复，5-申诉恢复 */
    private Integer changeType;

    /** 积分变动（负数为扣分） */
    private Integer scoreChange;

    /** 变动前积分 */
    private Integer scoreBefore;

    /** 变动后积分 */
    private Integer scoreAfter;

    /** 变动原因 */
    private String reason;

    /** 关联记录ID（处罚/举报/申诉ID） */
    private String relatedRecordId;

    /** 创建时间戳 */
    private Long createdTime;

    /** 逻辑删除：0-未删除，1-已删除 */
    @TableLogic private Integer isDelete;
}
