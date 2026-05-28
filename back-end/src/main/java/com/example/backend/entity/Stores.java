package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("stores")
public class Stores {

    @TableId
    private String id;

    private String name;

    private String logoImageId;

    private String storeAdminId;

    private String contactPhone;

    private String address;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private Integer businessStatus;

    private BigDecimal rating;

    private Integer ratingCount;

    private String description;

    private String businessLicense;

    private Integer auditStatus;

    private String auditRemark;

    private Long auditTime;

    private Integer isOnline;

    private Long createdTime;

    private Long updatedTime;

    @Version
    private Integer version;

    @TableLogic
    private Integer isDelete;
}
