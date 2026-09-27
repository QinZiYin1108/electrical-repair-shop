package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/** 内容审核日志 @TableName content_check_logs */
@TableName(value = "content_check_logs")
@Data
public class ContentCheckLogs {

    /** 主键，CCL+雪花ID */
    @TableId private String id;

    /** 提交人账号ID */
    private String accountId;

    /** 提交人类型：1-用户，2-师傅，3-门店管理员 */
    private Integer accountType;

    /** 内容类型：1-文字，2-图片URL */
    private Integer contentType;

    /** 原始内容（文字或图片URL） */
    private String content;

    /** 审核结果：1-通过，2-拦截（文字），3-待审（图片） */
    private Integer checkResult;

    /** 命中规则ID */
    private String hitRuleId;

    /** 命中关键词 */
    private String hitKeyword;

    /** 检测来源：1-自建库，2-阿里云 */
    private Integer source;

    /** 创建时间戳 */
    private Long createdTime;

    /** 逻辑删除 */
    @TableLogic private Integer isDelete;
}
