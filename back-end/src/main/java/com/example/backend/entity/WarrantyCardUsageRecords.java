package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

@Data
@TableName("warranty_card_usage_records")
public class WarrantyCardUsageRecords {

    @TableId
    private String id;

    private String warrantyCardId;

    private String cardNo;

    private String userId;

    private String productId;

    private String productName;

    private String productModel;

    private String issueDescription;

    private String contactName;

    private String contactPhone;

    private Integer status;

    private String processRemark;

    private Long applyTime;

    private Long processTime;

    private Long createdTime;

    private Long updatedTime;

    @Version
    private Integer version;

    @TableLogic
    private Integer isDelete;
}
