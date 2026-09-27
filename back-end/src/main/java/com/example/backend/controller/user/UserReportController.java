package com.example.backend.controller.user;

import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.Reports;
import com.example.backend.exception.BusinessException;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.ReportsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/** 用户端举报接口 */
@Tag(name = "用户端/举报", description = "用户提交举报")
@RestController
@RequestMapping("/user/reports")
public class UserReportController {

    private final ReportsService reportsService;

    public UserReportController(ReportsService reportsService) {
        this.reportsService = reportsService;
    }

    private void requireUser() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || user.getRole() != AccountRole.USER) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
    }

    @Operation(summary = "提交举报")
    @PostMapping
    public Result<Reports> submit(@RequestBody Reports report) {
        LoginUserInfo user = AuthUserContext.get();
        requireUser();

        report.setReporterId(user.getAccountId());
        report.setReporterType(1); // 用户

        return Result.success(reportsService.submitReport(report));
    }

    @Operation(summary = "我的举报列表")
    @GetMapping("/my")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<Reports>> myReports(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        LoginUserInfo user = AuthUserContext.get();
        requireUser();

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Reports> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(Reports::getReporterId, user.getAccountId())
                .eq(Reports::getReporterType, 1)
                .orderByDesc(Reports::getCreatedTime);
        return Result.success(
                reportsService.page(
                        new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                                page, size),
                        wrapper));
    }

    @Operation(summary = "举报详情")
    @GetMapping("/{id}")
    public Result<Reports> detail(@PathVariable String id) {
        requireUser();
        return Result.success(reportsService.getReportDetail(id));
    }
}
