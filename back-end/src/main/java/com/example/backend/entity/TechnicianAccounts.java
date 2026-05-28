package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("technician_accounts")
public class TechnicianAccounts {

    @TableId
    private String id;

    private String storeId;

    private String username;

    private String phone;

    private String email;

    private String wechatOpenid;

    private String wechatUnionid;

    private String passwordHash;

    private String salt;

    private Integer accountStatus;

    private Long lastLoginTime;

    private String lastLoginIp;

    private Integer workStatus;

    private BigDecimal rating;

    private Integer creditScore;

    private Integer orderCount;

    private BigDecimal completionRate;

    private Long createdTime;

    private Long updatedTime;

    @Version
    private Integer version;

    @TableLogic
    private Integer isDelete;
}
