package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

/** 订单 SLA 超时事件。记录超时触发状态、责任方、通知次数、升级级别与最终处理结果。 @TableName order_sla_events */
@TableName("order_sla_events")
@Data
public class OrderSlaEvents {
    /** 主键，SL+雪花ID */
    @TableId private String id;

    /** 订单ID */
    private String orderId;

    /** 订单类型：1-维修订单，2-商品订单 */
    private Integer orderType;

    /** SLA类型：ACCEPT/VISIT/INSPECTION/PAYMENT/COMPLETION */
    private String slaType;

    /** 触发时订单状态 */
    private Integer orderStatus;

    /** 责任方：PLATFORM/TECHNICIAN/USER */
    private String responsibleParty;

    /** 责任方账号ID */
    private String responsibleId;

    /** 状态：1-处理中，2-已恢复，3-已自动处理，4-已忽略 */
    private Integer status;

    /** 升级级别：0-无，1-已提醒，2-已升级 */
    private Integer escalationLevel;

    /** 通知次数 */
    private Integer notifyCount;

    /** 已执行动作：REMIND/ESCALATE/AUTO_CANCEL */
    private String actionTaken;

    /** 最近一次超时时长（分钟） */
    private Integer overdueMinutes;

    /** 首次超时时间戳 */
    private Long firstExceededTime;

    /** 最近通知时间戳 */
    private Long lastNotifiedTime;

    /** 恢复/处理完成时间戳 */
    private Long resolvedTime;

    /** 创建时间戳 */
    private Long createdTime;

    /** 更新时间戳 */
    private Long updatedTime;

    /** 乐观锁版本号 */
    @Version private Integer version;

    /** 逻辑删除：0-未删除，1-已删除 */
    @TableLogic private Integer isDelete;
}
