package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("technician_bindings")
public class TechnicianBindings {

    @TableId
    private String id;

    private String storeId;

    private String technicianId;

    /** 状态：1-待确认，2-已绑定，3-申请解绑，4-已解绑 */
    private Integer status;

    private Long invitedTime;

    private Long confirmedTime;

    private Long unbindRequestedTime;

    private Long createdTime;

    private Long updatedTime;

    @Version
    private Integer version;

    @TableLogic
    private Integer isDelete;
}
