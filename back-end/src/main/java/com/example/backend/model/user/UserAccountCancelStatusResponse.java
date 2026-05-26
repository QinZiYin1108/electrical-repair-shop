package com.example.backend.model.user;

import lombok.Data;

@Data
public class UserAccountCancelStatusResponse {

    /**
     * 账号状态：1-正常，2-冻结，3-注销申请中，4-已注销
     */
    private Integer status;

    private Long cancelApplyTime;
    private Long cancelDeadlineTime;

    /**
     * 说明文案
     */
    private String tip;

    /**
     * 是否可以申请注销
     */
    private Boolean canApply;

    /**
     * 是否可以撤销注销申请
     */
    private Boolean canRevoke;
}

