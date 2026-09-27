package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.AuditEvents;
import com.example.backend.mapper.AuditEventsMapper;
import com.example.backend.model.audit.AuditEventCommand;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.context.RequestContext;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.AuditEventsService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import org.springframework.stereotype.Service;

@Service
public class AuditEventsServiceImpl extends ServiceImpl<AuditEventsMapper, AuditEvents>
        implements AuditEventsService {

    @Override
    public void record(AuditEventCommand command) {
        if (command == null) {
            return;
        }
        long now = System.currentTimeMillis();
        AuditEvents event = new AuditEvents();
        event.setId(SnowflakeIdUtil.nextAuditEventId());
        event.setEventType(command.eventType());
        event.setBizType(command.bizType());
        event.setBizId(command.bizId());
        event.setBeforeState(command.beforeState());
        event.setAfterState(command.afterState());
        event.setAmountBefore(command.amountBefore());
        event.setAmountAfter(command.amountAfter());
        event.setReason(command.reason());
        event.setRelatedId(command.relatedId());

        LoginUserInfo user = AuthUserContext.get();
        if (user != null) {
            event.setOperatorId(user.getAccountId());
            event.setOperatorRole(user.getRole() == null ? null : user.getRole().name());
            event.setOperatorName(user.getAccountId());
        }
        event.setSourceIp(RequestContext.getClientIp());
        event.setRequestId(RequestContext.getRequestId());
        event.setCreatedTime(now);
        event.setVersion(0);
        event.setIsDelete(0);
        save(event);
    }
}
