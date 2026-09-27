package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.PenaltyRecords;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.audit.AuditEventCommand;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.AuditEventsService;
import com.example.backend.service.PenaltyRecordsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** 处罚管理 — 仅超级管理员可用。 */
@Tag(name = "管理员端/处罚管理", description = "处罚记录列表、执行处罚、申诉处理、解除处罚")
@RestController
@RequestMapping("/admin/penalties")
public class AdminPenaltyController {

    private final PenaltyRecordsService penaltyRecordsService;
    private final AuditEventsService auditEventsService;

    public AdminPenaltyController(
            PenaltyRecordsService penaltyRecordsService, AuditEventsService auditEventsService) {
        this.penaltyRecordsService = penaltyRecordsService;
        this.auditEventsService = auditEventsService;
    }

    private void requireSuperAdmin() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !user.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅超级管理员可操作");
        }
    }

    @Operation(summary = "处罚记录列表（分页）")
    @GetMapping("/list")
    public Result<Page<PenaltyRecords>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "violationLevel", required = false) Integer violationLevel,
            @RequestParam(value = "appealStatus", required = false) Integer appealStatus,
            @RequestParam(value = "startTime", required = false) Long startTime,
            @RequestParam(value = "endTime", required = false) Long endTime) {
        requireSuperAdmin();
        return Result.success(
                penaltyRecordsService.listPenalties(
                        page, size, status, violationLevel, appealStatus, startTime, endTime));
    }

    @Operation(summary = "处罚详情")
    @GetMapping("/{id}")
    public Result<PenaltyRecords> detail(@PathVariable String id) {
        requireSuperAdmin();
        return Result.success(penaltyRecordsService.getPenaltyDetail(id));
    }

    @Operation(summary = "手动执行处罚")
    @PostMapping("/execute")
    public Result<PenaltyRecords> execute(@RequestBody Map<String, Object> body) {
        requireSuperAdmin();
        LoginUserInfo user = AuthUserContext.get();

        String accountId = (String) body.get("accountId");
        Integer accountType = (Integer) body.get("accountType");
        String reportId = (String) body.get("reportId");
        Integer violationLevel = (Integer) body.get("violationLevel");
        Integer penaltyType = (Integer) body.get("penaltyType");
        Integer banDurationHours = (Integer) body.get("banDurationHours");
        String restrictedFunctions =
                body.get("restrictedFunctions") != null
                        ? body.get("restrictedFunctions").toString()
                        : null;
        String remark = (String) body.get("remark");

        PenaltyRecords penalty =
                penaltyRecordsService.executePenalty(
                        accountId,
                        accountType,
                        reportId,
                        violationLevel,
                        penaltyType,
                        banDurationHours,
                        restrictedFunctions,
                        user.getAccountId(),
                        remark);
        auditEventsService.record(
                new AuditEventCommand(
                        "BAN",
                        "ACCOUNT",
                        accountId,
                        null,
                        "BANNED",
                        null,
                        null,
                        remark,
                        penalty == null ? null : penalty.getId()));
        return Result.success(penalty);
    }

    @Operation(summary = "处理申诉")
    @PostMapping("/{id}/appeal/process")
    public Result<PenaltyRecords> processAppeal(
            @PathVariable String id, @RequestBody Map<String, Object> body) {
        requireSuperAdmin();
        LoginUserInfo user = AuthUserContext.get();
        Boolean approved = (Boolean) body.get("approved");
        String result = (String) body.get("result");
        return Result.success(
                penaltyRecordsService.processAppeal(
                        id, approved != null && approved, result, user.getAccountId()));
    }

    @Operation(summary = "解除处罚")
    @PostMapping("/{id}/lift")
    public Result<PenaltyRecords> lift(@PathVariable String id) {
        requireSuperAdmin();
        LoginUserInfo user = AuthUserContext.get();
        PenaltyRecords penalty = penaltyRecordsService.liftPenalty(id, user.getAccountId());
        auditEventsService.record(
                new AuditEventCommand(
                        "UNBAN", "PENALTY", id, "BANNED", "LIFTED", null, null, "管理员解除处罚", id));
        return Result.success(penalty);
    }
}
