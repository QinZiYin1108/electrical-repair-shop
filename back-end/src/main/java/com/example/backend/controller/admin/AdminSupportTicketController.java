package com.example.backend.controller.admin;

import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.SupportTicketLogs;
import com.example.backend.entity.SupportTickets;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.support.SupportTicketCreateRequest;
import com.example.backend.model.support.SupportTicketDetailResponse;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.SupportTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员端/客服工单")
@RestController
@RequestMapping("/admin/support-tickets")
public class AdminSupportTicketController {

    private final SupportTicketService supportTicketService;

    public AdminSupportTicketController(SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    private LoginUserInfo requireAdmin() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "需登录管理员");
        }
        return user;
    }

    @Operation(summary = "创建工单")
    @PostMapping
    public Result<SupportTickets> create(@RequestBody SupportTicketCreateRequest request) {
        LoginUserInfo admin = requireAdmin();
        return Result.success(supportTicketService.create(request, admin.getAccountId()));
    }

    @Operation(summary = "工单列表")
    @GetMapping("/list")
    public Result<List<SupportTickets>> list(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "assigneeAdminId", required = false) String assigneeAdminId,
            @RequestParam(value = "priority", required = false) Integer priority,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        requireAdmin();
        return Result.success(
                supportTicketService.listForAdmin(status, assigneeAdminId, priority, limit));
    }

    @Operation(summary = "工单详情")
    @GetMapping("/{id}")
    public Result<SupportTicketDetailResponse> detail(@PathVariable("id") String id) {
        requireAdmin();
        return Result.success(supportTicketService.detail(id));
    }

    @Operation(summary = "分派处理人")
    @PostMapping("/{id}/assign")
    public Result<SupportTickets> assign(
            @PathVariable("id") String id,
            @RequestBody(required = false) Map<String, Object> body) {
        LoginUserInfo admin = requireAdmin();
        return Result.success(
                supportTicketService.assign(
                        id,
                        str(body, "assigneeAdminId"),
                        admin.getAccountId(),
                        str(body, "remark")));
    }

    @Operation(summary = "调整优先级")
    @PostMapping("/{id}/priority")
    public Result<SupportTickets> priority(
            @PathVariable("id") String id,
            @RequestBody(required = false) Map<String, Object> body) {
        LoginUserInfo admin = requireAdmin();
        return Result.success(
                supportTicketService.changePriority(
                        id, integer(body, "priority"), admin.getAccountId(), str(body, "remark")));
    }

    @Operation(summary = "变更状态")
    @PostMapping("/{id}/status")
    public Result<SupportTickets> status(
            @PathVariable("id") String id,
            @RequestBody(required = false) Map<String, Object> body) {
        LoginUserInfo admin = requireAdmin();
        return Result.success(
                supportTicketService.changeStatus(
                        id, integer(body, "status"), admin.getAccountId(), str(body, "remark")));
    }

    @Operation(summary = "追加内部备注")
    @PostMapping("/{id}/comment")
    public Result<SupportTicketLogs> comment(
            @PathVariable("id") String id,
            @RequestBody(required = false) Map<String, Object> body) {
        LoginUserInfo admin = requireAdmin();
        return Result.success(
                supportTicketService.addComment(
                        id, admin.getAccountId(), str(body, "content"), str(body, "attachments")));
    }

    @Operation(summary = "审批通过（双人审批）")
    @PostMapping("/{id}/approve")
    public Result<SupportTickets> approve(
            @PathVariable("id") String id,
            @RequestBody(required = false) Map<String, Object> body) {
        LoginUserInfo admin = requireAdmin();
        return Result.success(
                supportTicketService.approve(id, admin.getAccountId(), str(body, "remark")));
    }

    @Operation(summary = "审批驳回")
    @PostMapping("/{id}/reject")
    public Result<SupportTickets> reject(
            @PathVariable("id") String id,
            @RequestBody(required = false) Map<String, Object> body) {
        LoginUserInfo admin = requireAdmin();
        return Result.success(
                supportTicketService.reject(id, admin.getAccountId(), str(body, "remark")));
    }

    private String str(Map<String, Object> body, String key) {
        if (body == null) {
            return null;
        }
        Object value = body.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Integer integer(Map<String, Object> body, String key) {
        if (body == null) {
            return null;
        }
        Object value = body.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, key + " 必须为整数");
        }
    }
}
