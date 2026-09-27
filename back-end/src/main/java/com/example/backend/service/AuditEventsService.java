package com.example.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.entity.AuditEvents;
import com.example.backend.model.audit.AuditEventCommand;

/** 统一业务审计事件。 */
public interface AuditEventsService extends IService<AuditEvents> {

    /** 记录一条审计事件；操作者、IP、请求ID由服务从上下文补齐。 */
    void record(AuditEventCommand command);
}
