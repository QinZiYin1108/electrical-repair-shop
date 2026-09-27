package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 订单取消原因表 @TableName cancel_reasons */
@TableName(value = "cancel_reasons")
@Data
public class CancelReasons {
    /** 主键，CLR+雪花ID */
    @TableId private String id;

    /** 订单ID */
    private String orderId;

    /** 订单类型：1-维修，2-商品 */
    private Integer orderType;

    /** 原因编码 */
    private String reasonCode;

    /** 原因标签 */
    private String reasonLabel;

    /** 用户手动备注 */
    private String userRemark;

    /** 创建时间戳 */
    private Long createdTime;

    /** 逻辑删除：0-未删除，1-已删除 */
    @TableLogic private Integer isDelete;
}
