package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

/** 预约停业/请假时段。用于节假日、临时停业、请假不可预约。 @TableName appointment_closures */
@TableName("appointment_closures")
@Data
public class AppointmentClosures {
    /** 主键，AC+雪花ID */
    @TableId private String id;

    /** 范围：GLOBAL-全平台，TECHNICIAN-师傅，STORE-门店 */
    private String ownerType;

    /** 范围对象ID（GLOBAL 可为空） */
    private String ownerId;

    /** 开始时间戳 */
    private Long startTime;

    /** 结束时间戳 */
    private Long endTime;

    /** 原因 */
    private String reason;

    /** 操作管理员ID */
    private String operatorAdminId;

    /** 创建时间戳 */
    private Long createdTime;

    /** 更新时间戳 */
    private Long updatedTime;

    /** 乐观锁版本号 */
    @Version private Integer version;

    /** 逻辑删除：0-未删除，1-已删除 */
    @TableLogic private Integer isDelete;
}
