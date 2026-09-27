package com.example.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.entity.SupportTicketLogs;
import com.example.backend.entity.SupportTickets;
import com.example.backend.model.support.SupportTicketCreateRequest;
import com.example.backend.model.support.SupportTicketDetailResponse;
import java.util.List;

/** 客服工单与人工介入。工单仅记录与追踪，不改核心状态与金额；金额操作按阈值触发双人审批。 */
public interface SupportTicketService extends IService<SupportTickets> {

    /** 创建工单；关联金额超过阈值时进入待审批并标记需双人审批。 */
    SupportTickets create(SupportTicketCreateRequest request, String creatorAdminId);

    /** 分派处理人。 */
    SupportTickets assign(
            String ticketId, String assigneeAdminId, String operatorAdminId, String remark);

    /** 调整优先级。 */
    SupportTickets changePriority(
            String ticketId, Integer priority, String operatorAdminId, String remark);

    /** 变更状态（按状态机校验；需审批而未经审批时不允许直接解决）。 */
    SupportTickets changeStatus(
            String ticketId, Integer targetStatus, String operatorAdminId, String remark);

    /** 追加内部备注（可带附件）。 */
    SupportTicketLogs addComment(
            String ticketId, String operatorAdminId, String content, String attachments);

    /** 审批通过（双人审批：审批人不能为创建人）；通过后回到处理中。 */
    SupportTickets approve(String ticketId, String approverAdminId, String remark);

    /** 审批驳回；驳回后回到处理中。 */
    SupportTickets reject(String ticketId, String approverAdminId, String remark);

    /** 管理端查询（status/assigneeAdminId/priority 为 null 时不过滤）。 */
    List<SupportTickets> listForAdmin(
            Integer status, String assigneeAdminId, Integer priority, int limit);

    /** 工单详情（含操作记录）。 */
    SupportTicketDetailResponse detail(String ticketId);
}
