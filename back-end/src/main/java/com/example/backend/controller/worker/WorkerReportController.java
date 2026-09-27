package com.example.backend.controller.worker;

import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.PenaltyRecords;
import com.example.backend.entity.Reports;
import com.example.backend.exception.BusinessException;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.PenaltyRecordsService;
import com.example.backend.service.ReportsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** 师傅端举报与申诉接口 */
@Tag(name = "师傅端/举报与申诉", description = "师傅提交举报、提交申诉")
@RestController
@RequestMapping("/worker/reports")
public class WorkerReportController {

    private final ReportsService reportsService;
    private final PenaltyRecordsService penaltyRecordsService;

    public WorkerReportController(
            ReportsService reportsService, PenaltyRecordsService penaltyRecordsService) {
        this.reportsService = reportsService;
        this.penaltyRecordsService = penaltyRecordsService;
    }

    private void requireWorker() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || user.getRole() != AccountRole.WORKER) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
    }

    @Operation(summary = "提交举报")
    @PostMapping
    public Result<Reports> submit(@RequestBody Reports report) {
        LoginUserInfo user = AuthUserContext.get();
        requireWorker();

        report.setReporterId(user.getAccountId());
        report.setReporterType(2); // 师傅

        return Result.success(reportsService.submitReport(report));
    }

    @Operation(summary = "我的举报列表")
    @GetMapping("/my")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<Reports>> myReports(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        LoginUserInfo user = AuthUserContext.get();
        requireWorker();

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Reports> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(Reports::getReporterId, user.getAccountId())
                .eq(Reports::getReporterType, 2)
                .orderByDesc(Reports::getCreatedTime);
        return Result.success(
                reportsService.page(
                        new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                                page, size),
                        wrapper));
    }

    @Operation(summary = "我的处罚记录（用于申诉）")
    @GetMapping("/my-penalties")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<PenaltyRecords>>
            myPenalties(
                    @RequestParam(value = "page", defaultValue = "1") int page,
                    @RequestParam(value = "size", defaultValue = "20") int size) {
        LoginUserInfo user = AuthUserContext.get();
        requireWorker();

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PenaltyRecords> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(PenaltyRecords::getAccountId, user.getAccountId())
                .eq(PenaltyRecords::getAccountType, 2)
                .orderByDesc(PenaltyRecords::getCreatedTime);
        return Result.success(
                penaltyRecordsService.page(
                        new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                                page, size),
                        wrapper));
    }

    @Operation(summary = "提交申诉")
    @PostMapping("/appeal")
    public Result<PenaltyRecords> submitAppeal(@RequestBody Map<String, Object> body) {
        requireWorker();
        String penaltyId = (String) body.get("penaltyId");
        String appealReason = (String) body.get("appealReason");
        return Result.success(penaltyRecordsService.submitAppeal(penaltyId, appealReason));
    }
}
