package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

@Data
@TableName("technician_profiles")
public class TechnicianProfiles {

    @TableId
    private String id;

    private String technicianAccountId;

    private String realName;

    private String idCard;

    private Integer gender;

    private Long birthday;

    private Integer workYears;

    private String education;

    private String certificates;

    private String specialties;

    private String introduction;

    private Integer responseTime;

    private Long locationUpdateTime;

    private Long createdTime;

    private Long updatedTime;

    @Version
    private Integer version;

    @TableLogic
    private Integer isDelete;
}
