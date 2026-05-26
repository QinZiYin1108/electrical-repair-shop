package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("technician_service_areas")
public class TechnicianServiceAreas {

    @TableId
    private String id;

    private String technicianAccountId;

    private BigDecimal centerLatitude;

    private BigDecimal centerLongitude;

    private String centerAddress;

    private String areaName;

    private Integer isDefault;

    private Integer isActive;

    private Long createdTime;

    private Long updatedTime;

    @Version
    private Integer version;

    @TableLogic
    private Integer isDelete;
}
