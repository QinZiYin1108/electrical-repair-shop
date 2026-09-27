package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.CancelReasons;
import com.example.backend.exception.BusinessException;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.CancelReasonsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** 订单取消原因管理 — 仅超级管理员可用。 提供取消原因统计和导出。 */
@Tag(name = "管理员端/取消原因", description = "订单取消原因统计与导出")
@RestController
@RequestMapping("/admin/cancel-reasons")
public class AdminCancelReasonController {

    private final CancelReasonsService cancelReasonsService;

    public AdminCancelReasonController(CancelReasonsService cancelReasonsService) {
        this.cancelReasonsService = cancelReasonsService;
    }

    private void requireSuperAdmin() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !user.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅超级管理员可操作");
        }
    }

    @Operation(summary = "取消原因列表（分页）")
    @GetMapping("/list")
    public Result<Page<CancelReasons>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "orderType", required = false) Integer orderType,
            @RequestParam(value = "startTime", required = false) Long startTime,
            @RequestParam(value = "endTime", required = false) Long endTime) {
        requireSuperAdmin();
        LambdaQueryWrapper<CancelReasons> wrapper = new LambdaQueryWrapper<>();
        if (orderType != null) {
            wrapper.eq(CancelReasons::getOrderType, orderType);
        }
        if (startTime != null) {
            wrapper.ge(CancelReasons::getCreatedTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(CancelReasons::getCreatedTime, endTime);
        }
        wrapper.orderByDesc(CancelReasons::getCreatedTime);
        Page<CancelReasons> result = cancelReasonsService.page(new Page<>(page, size), wrapper);
        return Result.success(result);
    }

    @Operation(summary = "取消原因统计（按原因编码分组）")
    @GetMapping("/stats")
    public Result<List<Map<String, Object>>> stats(
            @RequestParam(value = "orderType", required = false) Integer orderType,
            @RequestParam(value = "startTime", required = false) Long startTime,
            @RequestParam(value = "endTime", required = false) Long endTime) {
        requireSuperAdmin();
        return Result.success(cancelReasonsService.statsByReason(orderType, startTime, endTime));
    }
}
