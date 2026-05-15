package com.example.backend.model.worker;

import lombok.Data;

/**
 * 师傅端账号注销状态
 */
@Data
public class WorkerAccountCancelStatusResponse {

    /**
     * 是否处于注销申请（反悔期）中
     */
    private Boolean canceling;

    /**
     * 注销申请时间戳
     */
    private Long cancelApplyTime;

    /**
     * 注销生效时间戳（反悔期截止）
     */
    private Long cancelDeadlineTime;

    /**
     * 是否可申请注销
     */
    private Boolean canApply;

    /**
     * 是否可撤销注销
     */
    private Boolean canRevoke;

    /**
     * 提示文案
     */
    private String tip;
}

