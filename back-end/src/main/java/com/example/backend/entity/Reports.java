package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/** 举报表 @TableName reports */
@TableName(value = "reports")
@Data
public class Reports {

    /** 主键，RP+雪花ID */
    @TableId private String id;

    /** 举报人账号ID */
    private String reporterId;

    /** 举报人类型：1-用户，2-师傅，3-门店管理员 */
    private Integer reporterType;

    /** 举报对象类型：1-账号，2-门店，3-订单，4-商品 */
    private Integer targetType;

    /** 举报对象ID */
    private String targetId;

    /** 举报字段或节点 */
    private String targetField;

    /** 举报原因分类 */
    private String reasonCategory;

    /** 补充说明 */
    private String description;

    /** 截图JSON数组 */
    private String evidenceImages;

    /** 状态：1-待处理，2-处理中，3-已成立，4-已驳回 */
    private Integer status;

    /** 处理结果说明 */
    private String result;

    /** 处理人ID */
    private String handlerId;

    /** 处理时间戳 */
    private Long handleTime;

    /** 创建时间戳 */
    private Long createdTime;

    /** 更新时间戳 */
    private Long updatedTime;

    /** 乐观锁版本号 */
    @Version private Integer version;

    /** 逻辑删除：0-未删除，1-已删除 */
    @TableLogic private Integer isDelete;
}
