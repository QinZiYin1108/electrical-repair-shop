package com.example.backend.model.finance;

import lombok.Data;

/** 管理端处理对账异常的请求。 */
@Data
public class ReconciliationHandleRequest {
    /** 处理状态：2-已处理，3-已忽略 */
    private Integer status;

    /** 处理备注 */
    private String remark;
}
