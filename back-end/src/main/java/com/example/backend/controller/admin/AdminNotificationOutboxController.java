package com.example.backend.controller.admin;

import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.NotificationOutbox;
import com.example.backend.exception.BusinessException;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.NotificationOutboxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员端/通知 outbox")
@RestController
@RequestMapping("/admin/notification-outbox")
public class AdminNotificationOutboxController {

    private final NotificationOutboxService notificationOutboxService;

    public AdminNotificationOutboxController(NotificationOutboxService notificationOutboxService) {
        this.notificationOutboxService = notificationOutboxService;
    }

    private LoginUserInfo requireAdmin() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "需登录管理员");
        }
        return user;
    }

    @Operation(summary = "查询通知 outbox（可按状态过滤，默认按时间倒序）")
    @GetMapping("/list")
    public Result<List<NotificationOutbox>> list(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        requireAdmin();
        return Result.success(notificationOutboxService.listForAdmin(status, limit));
    }

    @Operation(summary = "人工重试失败/死信的通知")
    @PostMapping("/{id}/retry")
    public Result<NotificationOutbox> retry(@PathVariable("id") String id) {
        requireAdmin();
        return Result.success(notificationOutboxService.manualRetry(id));
    }
}
