package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.common.ErrorCode;
import com.example.backend.domain.support.SupportTicketAction;
import com.example.backend.domain.support.SupportTicketPriority;
import com.example.backend.domain.support.SupportTicketStatus;
import com.example.backend.entity.SupportTicketLogs;
import com.example.backend.entity.SupportTickets;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.SupportTicketsMapper;
import com.example.backend.model.audit.AuditEventCommand;
import com.example.backend.model.support.SupportTicketCreateRequest;
import com.example.backend.model.support.SupportTicketDetailResponse;
import com.example.backend.service.AuditEventsService;
import com.example.backend.service.BusinessMetrics;
import com.example.backend.service.SupportTicketLogsService;
import com.example.backend.service.SupportTicketService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 客服工单实现。操作均通过专用命令方法，禁止通用更新直接改状态/金额；关键操作写操作记录与审计。 */
@Service
public class SupportTicketServiceImpl extends ServiceImpl<SupportTicketsMapper, SupportTickets>
        implements SupportTicketService {

    private static final int APPROVAL_YES = 1;
    private static final int APPROVAL_NO = 0;
    private static final int SOURCE_ADMIN = 1;
    private static final String BIZ_TYPE_TICKET = "SUPPORT_TICKET";

    private static final Map<SupportTicketStatus, Set<SupportTicketStatus>> TRANSITIONS =
            new EnumMap<>(SupportTicketStatus.class);

    static {
        TRANSITIONS.put(
                SupportTicketStatus.PENDING,
                EnumSet.of(
                        SupportTicketStatus.PROCESSING,
                        SupportTicketStatus.PENDING_APPROVAL,
                        SupportTicketStatus.CLOSED));
        TRANSITIONS.put(
                SupportTicketStatus.PROCESSING,
                EnumSet.of(
                        SupportTicketStatus.PENDING_APPROVAL,
                        SupportTicketStatus.RESOLVED,
                        SupportTicketStatus.CLOSED));
        TRANSITIONS.put(
                SupportTicketStatus.PENDING_APPROVAL,
                EnumSet.of(SupportTicketStatus.PROCESSING, SupportTicketStatus.RESOLVED));
        TRANSITIONS.put(SupportTicketStatus.RESOLVED, EnumSet.of(SupportTicketStatus.CLOSED));
        TRANSITIONS.put(SupportTicketStatus.CLOSED, EnumSet.noneOf(SupportTicketStatus.class));
    }

    private final SupportTicketLogsService supportTicketLogsService;
    private final AuditEventsService auditEventsService;
    private final BusinessMetrics businessMetrics;

    @Value("${support.ticket.approval-amount-threshold:1000}")
    private BigDecimal approvalAmountThreshold = new BigDecimal("1000");

    @Value("${support.ticket.default-due-hours:24}")
    private int defaultDueHours = 24;

    public SupportTicketServiceImpl(
            SupportTicketLogsService supportTicketLogsService,
            AuditEventsService auditEventsService,
            BusinessMetrics businessMetrics) {
        this.supportTicketLogsService = supportTicketLogsService;
        this.auditEventsService = auditEventsService;
        this.businessMetrics = businessMetrics;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupportTickets create(SupportTicketCreateRequest request, String creatorAdminId) {
        if (request == null || !StringUtils.hasText(request.getTitle())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "工单标题不能为空");
        }
        long now = System.currentTimeMillis();
        BigDecimal amount = request.getAmount();
        boolean requiresApproval = requiresApproval(amount);
        SupportTickets ticket = new SupportTickets();
        ticket.setId(SnowflakeIdUtil.nextSupportTicketId());
        ticket.setTicketNo("TK" + ticket.getId().substring(2));
        ticket.setCategory(trimToNull(request.getCategory()));
        ticket.setTitle(request.getTitle().trim());
        ticket.setContent(trimToNull(request.getContent()));
        ticket.setBizType(trimToNull(request.getBizType()));
        ticket.setBizId(trimToNull(request.getBizId()));
        ticket.setRelatedUserAccountId(trimToNull(request.getRelatedUserAccountId()));
        ticket.setRelatedTechnicianAccountId(trimToNull(request.getRelatedTechnicianAccountId()));
        ticket.setPriority(
                SupportTicketPriority.isValid(request.getPriority())
                        ? request.getPriority()
                        : SupportTicketPriority.MEDIUM.getCode());
        ticket.setStatus(
                requiresApproval
                        ? SupportTicketStatus.PENDING_APPROVAL.getCode()
                        : SupportTicketStatus.PENDING.getCode());
        ticket.setSource(request.getSource() == null ? SOURCE_ADMIN : request.getSource());
        ticket.setCreatorAdminId(trimToNull(creatorAdminId));
        ticket.setAmount(amount);
        ticket.setRequiresApproval(requiresApproval ? APPROVAL_YES : APPROVAL_NO);
        ticket.setDueTime(
                request.getDueTime() != null
                        ? request.getDueTime()
                        : now + Math.max(defaultDueHours, 1) * 3600_000L);
        ticket.setCreatedTime(now);
        ticket.setUpdatedTime(now);
        ticket.setVersion(0);
        ticket.setIsDelete(0);
        if (!save(ticket)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建工单失败");
        }
        writeLog(
                ticket.getId(),
                SupportTicketAction.CREATE,
                creatorAdminId,
                ticket.getContent(),
                null,
                ticket.getStatus(),
                null,
                now);
        businessMetrics.supportTicketCreated();
        if (requiresApproval) {
            businessMetrics.supportTicketApprovalRequired();
        }
        audit(
                "SUPPORT_TICKET_CREATE",
                ticket,
                null,
                String.valueOf(ticket.getStatus()),
                amount,
                "创建工单");
        return ticket;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupportTickets assign(
            String ticketId, String assigneeAdminId, String operatorAdminId, String remark) {
        if (!StringUtils.hasText(assigneeAdminId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "处理人不能为空");
        }
        SupportTickets ticket = requireTicket(ticketId);
        long now = System.currentTimeMillis();
        ticket.setAssigneeAdminId(assigneeAdminId.trim());
        if (SupportTicketStatus.fromCode(ticket.getStatus()) == SupportTicketStatus.PENDING) {
            ticket.setStatus(SupportTicketStatus.PROCESSING.getCode());
        }
        ticket.setUpdatedTime(now);
        updateTicket(ticket);
        writeLog(
                ticket.getId(),
                SupportTicketAction.ASSIGN,
                operatorAdminId,
                remark,
                null,
                ticket.getStatus(),
                null,
                now);
        audit(
                "SUPPORT_TICKET_ASSIGN",
                ticket,
                null,
                String.valueOf(ticket.getStatus()),
                null,
                remark);
        return ticket;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupportTickets changePriority(
            String ticketId, Integer priority, String operatorAdminId, String remark) {
        if (!SupportTicketPriority.isValid(priority)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "优先级仅支持 1-高 / 2-中 / 3-低");
        }
        SupportTickets ticket = requireTicket(ticketId);
        long now = System.currentTimeMillis();
        ticket.setPriority(priority);
        ticket.setUpdatedTime(now);
        updateTicket(ticket);
        writeLog(
                ticket.getId(),
                SupportTicketAction.PRIORITY,
                operatorAdminId,
                remark,
                null,
                null,
                null,
                now);
        audit("SUPPORT_TICKET_PRIORITY", ticket, null, null, null, remark);
        return ticket;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupportTickets changeStatus(
            String ticketId, Integer targetStatus, String operatorAdminId, String remark) {
        SupportTickets ticket = requireTicket(ticketId);
        SupportTicketStatus current = SupportTicketStatus.fromCode(ticket.getStatus());
        SupportTicketStatus target = SupportTicketStatus.fromCode(targetStatus);
        if (target == SupportTicketStatus.UNKNOWN) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的目标状态");
        }
        if (!TRANSITIONS
                .getOrDefault(current, EnumSet.noneOf(SupportTicketStatus.class))
                .contains(target)) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR,
                    "工单状态不允许从" + current.getText() + "变更为" + target.getText());
        }
        if (target == SupportTicketStatus.RESOLVED
                && Integer.valueOf(APPROVAL_YES).equals(ticket.getRequiresApproval())
                && !StringUtils.hasText(ticket.getApproverAdminId())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该工单需双人审批后方可解决");
        }
        long now = System.currentTimeMillis();
        int from = ticket.getStatus();
        ticket.setStatus(target.getCode());
        if (target == SupportTicketStatus.RESOLVED) {
            ticket.setResolvedTime(now);
        } else if (target == SupportTicketStatus.CLOSED) {
            ticket.setClosedTime(now);
        }
        ticket.setUpdatedTime(now);
        updateTicket(ticket);
        writeLog(
                ticket.getId(),
                SupportTicketAction.STATUS,
                operatorAdminId,
                remark,
                from,
                target.getCode(),
                null,
                now);
        audit(
                "SUPPORT_TICKET_STATUS",
                ticket,
                String.valueOf(from),
                String.valueOf(target.getCode()),
                null,
                remark);
        if (target == SupportTicketStatus.RESOLVED) {
            businessMetrics.supportTicketResolved();
        }
        return ticket;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupportTicketLogs addComment(
            String ticketId, String operatorAdminId, String content, String attachments) {
        if (!StringUtils.hasText(content) && !StringUtils.hasText(attachments)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "备注或附件至少填写一项");
        }
        requireTicket(ticketId);
        long now = System.currentTimeMillis();
        return writeLog(
                ticketId,
                SupportTicketAction.COMMENT,
                operatorAdminId,
                content,
                null,
                null,
                attachments,
                now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupportTickets approve(String ticketId, String approverAdminId, String remark) {
        return resolveApproval(ticketId, approverAdminId, remark, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupportTickets reject(String ticketId, String approverAdminId, String remark) {
        return resolveApproval(ticketId, approverAdminId, remark, false);
    }

    private SupportTickets resolveApproval(
            String ticketId, String approverAdminId, String remark, boolean approved) {
        SupportTickets ticket = requireTicket(ticketId);
        if (SupportTicketStatus.fromCode(ticket.getStatus())
                != SupportTicketStatus.PENDING_APPROVAL) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "仅待审批的工单可审批");
        }
        if (!StringUtils.hasText(approverAdminId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (approverAdminId.equals(ticket.getCreatorAdminId())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "审批人不能为创建人（需双人审批）");
        }
        long now = System.currentTimeMillis();
        int from = ticket.getStatus();
        ticket.setApproverAdminId(approverAdminId.trim());
        ticket.setApproveTime(now);
        ticket.setStatus(SupportTicketStatus.PROCESSING.getCode());
        ticket.setUpdatedTime(now);
        updateTicket(ticket);
        writeLog(
                ticket.getId(),
                approved ? SupportTicketAction.APPROVE : SupportTicketAction.REJECT,
                approverAdminId,
                remark,
                from,
                ticket.getStatus(),
                null,
                now);
        audit(
                approved ? "SUPPORT_TICKET_APPROVE" : "SUPPORT_TICKET_REJECT",
                ticket,
                String.valueOf(from),
                String.valueOf(ticket.getStatus()),
                null,
                remark);
        if (approved) {
            businessMetrics.supportTicketApproved();
        }
        return ticket;
    }

    @Override
    public List<SupportTickets> listForAdmin(
            Integer status, String assigneeAdminId, Integer priority, int limit) {
        LambdaQueryWrapper<SupportTickets> wrapper =
                new LambdaQueryWrapper<SupportTickets>()
                        .orderByDesc(SupportTickets::getCreatedTime)
                        .last("limit " + Math.min(Math.max(limit, 1), 200));
        if (status != null) {
            wrapper.eq(SupportTickets::getStatus, status);
        }
        if (StringUtils.hasText(assigneeAdminId)) {
            wrapper.eq(SupportTickets::getAssigneeAdminId, assigneeAdminId);
        }
        if (priority != null) {
            wrapper.eq(SupportTickets::getPriority, priority);
        }
        return list(wrapper);
    }

    @Override
    public SupportTicketDetailResponse detail(String ticketId) {
        SupportTickets ticket = requireTicket(ticketId);
        List<SupportTicketLogs> logs =
                supportTicketLogsService.list(
                        new LambdaQueryWrapper<SupportTicketLogs>()
                                .eq(SupportTicketLogs::getTicketId, ticketId)
                                .orderByAsc(SupportTicketLogs::getCreatedTime));
        return new SupportTicketDetailResponse(ticket, logs);
    }

    private boolean requiresApproval(BigDecimal amount) {
        return amount != null
                && approvalAmountThreshold != null
                && amount.compareTo(approvalAmountThreshold) > 0;
    }

    private SupportTicketLogs writeLog(
            String ticketId,
            String action,
            String operatorAdminId,
            String content,
            Integer fromStatus,
            Integer toStatus,
            String attachments,
            long now) {
        SupportTicketLogs log = new SupportTicketLogs();
        log.setId(SnowflakeIdUtil.nextSupportTicketLogId());
        log.setTicketId(ticketId);
        log.setAction(action);
        log.setOperatorAdminId(trimToNull(operatorAdminId));
        log.setOperatorName(trimToNull(operatorAdminId));
        log.setContent(truncate(content, 2000));
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setAttachments(truncate(attachments, 2000));
        log.setCreatedTime(now);
        log.setVersion(0);
        log.setIsDelete(0);
        supportTicketLogsService.save(log);
        return log;
    }

    private SupportTickets requireTicket(String ticketId) {
        if (!StringUtils.hasText(ticketId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "工单ID不能为空");
        }
        SupportTickets ticket = getById(ticketId);
        if (ticket == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工单不存在");
        }
        return ticket;
    }

    private void updateTicket(SupportTickets ticket) {
        if (!updateById(ticket)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新工单失败");
        }
    }

    private void audit(
            String eventType,
            SupportTickets ticket,
            String beforeState,
            String afterState,
            BigDecimal amount,
            String reason) {
        auditEventsService.record(
                new AuditEventCommand(
                        eventType,
                        BIZ_TYPE_TICKET,
                        ticket.getId(),
                        beforeState,
                        afterState,
                        null,
                        amount,
                        reason,
                        ticket.getTicketNo()));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}
