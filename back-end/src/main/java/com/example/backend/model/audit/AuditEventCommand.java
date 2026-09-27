package com.example.backend.model.audit;

import java.math.BigDecimal;

/** 记录一条业务审计事件所需的数据。操作者、IP、请求ID由服务从上下文补齐。 */
public record AuditEventCommand(
        String eventType,
        String bizType,
        String bizId,
        String beforeState,
        String afterState,
        BigDecimal amountBefore,
        BigDecimal amountAfter,
        String reason,
        String relatedId) {}
