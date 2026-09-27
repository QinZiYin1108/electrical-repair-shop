package com.example.backend.model.support;

import com.example.backend.entity.SupportTicketLogs;
import com.example.backend.entity.SupportTickets;
import java.util.List;

/** 客服工单详情（含操作记录）。 */
public record SupportTicketDetailResponse(SupportTickets ticket, List<SupportTicketLogs> logs) {}
