package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

@Data
@TableName("order_door_qr_codes")
public class OrderDoorQrCodes {

    @TableId
    private String id;

    private String repairOrderId;

    private String technicianAccountId;

    private String accountId;

    private String token;

    private Integer status;

    private Long expireTime;

    private Long usedTime;

    private String usedBy;

    private String imageId;

    private Long createdTime;

    private Long updatedTime;

    @Version
    private Integer version;

    @TableLogic
    private Integer isDelete;
}
