package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalTime;

@Data
@TableName("store_business_hours")
public class StoreBusinessHours {

    @TableId
    private String id;

    private String storeId;

    private Integer dayOfWeek;

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer isAvailable;

    private Long createdTime;

    private Long updatedTime;

    @Version
    private Integer version;

    @TableLogic
    private Integer isDelete;
}
