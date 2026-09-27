package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.ReconciliationBatches;
import com.example.backend.entity.ReconciliationIssues;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.finance.ReconciliationHandleRequest;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.FundReconciliationService;
import com.example.backend.service.ReconciliationBatchesService;
import com.example.backend.service.ReconciliationIssuesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员端/资金对账")
@RestController
@RequestMapping("/admin/reconciliation")
public class AdminFundReconciliationController {
    private static final long DEFAULT_LOOKBACK_MILLIS = 24L * 60L * 60L * 1000L;
    private static final int ISSUE_STATUS_HANDLED = 2;
    private static final int ISSUE_STATUS_IGNORED = 3;

    private final FundReconciliationService fundReconciliationService;
    private final ReconciliationBatchesService reconciliationBatchesService;
    private final ReconciliationIssuesService reconciliationIssuesService;

    public AdminFundReconciliationController(
            FundReconciliationService fundReconciliationService,
            ReconciliationBatchesService reconciliationBatchesService,
            ReconciliationIssuesService reconciliationIssuesService) {
        this.fundReconciliationService = fundReconciliationService;
        this.reconciliationBatchesService = reconciliationBatchesService;
        this.reconciliationIssuesService = reconciliationIssuesService;
    }

    private LoginUserInfo requireSuperAdmin() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !user.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅超级管理员可访问资金对账");
        }
        return user;
    }

    @Operation(summary = "查询对账批次")
    @GetMapping("/batches")
    public Result<List<ReconciliationBatches>> listBatches(
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        requireSuperAdmin();
        int size = Math.min(Math.max(limit, 1), 100);
        return Result.success(
                reconciliationBatchesService.list(
                        new LambdaQueryWrapper<ReconciliationBatches>()
                                .eq(ReconciliationBatches::getIsDelete, 0)
                                .orderByDesc(ReconciliationBatches::getCreatedTime)
                                .last("limit " + size)));
    }

    @Operation(summary = "查询批次异常明细")
    @GetMapping("/batches/{batchId}/issues")
    public Result<List<ReconciliationIssues>> listIssues(@PathVariable("batchId") String batchId) {
        requireSuperAdmin();
        return Result.success(
                reconciliationIssuesService.list(
                        new LambdaQueryWrapper<ReconciliationIssues>()
                                .eq(ReconciliationIssues::getBatchId, batchId)
                                .eq(ReconciliationIssues::getIsDelete, 0)
                                .orderByAsc(ReconciliationIssues::getCreatedTime)));
    }

    @Operation(summary = "处理对账异常")
    @PostMapping("/issues/{issueId}/handle")
    public Result<ReconciliationIssues> handle(
            @PathVariable("issueId") String issueId,
            @RequestBody(required = false) ReconciliationHandleRequest request) {
        LoginUserInfo admin = requireSuperAdmin();
        ReconciliationIssues issue = reconciliationIssuesService.getById(issueId);
        if (issue == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "对账异常不存在");
        }
        int status =
                request == null || request.getStatus() == null
                        ? ISSUE_STATUS_HANDLED
                        : request.getStatus();
        if (status != ISSUE_STATUS_HANDLED && status != ISSUE_STATUS_IGNORED) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "处理状态仅支持 2-已处理 或 3-已忽略");
        }
        long now = System.currentTimeMillis();
        issue.setStatus(status);
        issue.setHandledBy(admin.getAccountId());
        issue.setHandledTime(now);
        issue.setHandleRemark(request == null ? null : request.getRemark());
        issue.setUpdatedTime(now);
        if (!reconciliationIssuesService.updateById(issue)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新对账异常失败");
        }
        return Result.success(issue);
    }

    @Operation(summary = "手动触发对账")
    @PostMapping("/run")
    public Result<ReconciliationBatches> run(
            @RequestParam(value = "batchType", defaultValue = "1") int batchType) {
        requireSuperAdmin();
        long now = System.currentTimeMillis();
        Long last = fundReconciliationService.lastBatchWindowEnd();
        long since = last != null ? last : now - DEFAULT_LOOKBACK_MILLIS;
        return Result.success(fundReconciliationService.runBatch(batchType, since, now, "admin"));
    }
}
