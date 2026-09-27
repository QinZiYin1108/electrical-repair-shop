package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

/** 预约时段占用。同一师傅同一时段同一时刻仅一条有效占用（active_slot 唯一）。 @TableName appointment_reservations */
@TableName("appointment_reservations")
@Data
public class AppointmentReservations {
    /** 主键，AR+雪花ID */
    @TableId private String id;

    /** 师傅账号ID */
    private String technicianAccountId;

    /** 预约时间戳 */
    private Long appointmentTime;

    /** 占用订单ID */
    private String orderId;

    /** 有效占用键（师傅:时间戳），释放后置 NULL */
    private String activeSlot;

    /** 状态：1-占用中，2-已释放 */
    private Integer status;

    /** 释放原因 */
    private String releaseReason;

    /** 创建时间戳 */
    private Long createdTime;

    /** 更新时间戳 */
    private Long updatedTime;

    /** 乐观锁版本号 */
    @Version private Integer version;

    /** 逻辑删除：0-未删除，1-已删除 */
    @TableLogic private Integer isDelete;
}
