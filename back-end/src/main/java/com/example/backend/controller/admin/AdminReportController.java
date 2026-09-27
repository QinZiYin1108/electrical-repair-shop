package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.PenaltyRecords;
import com.example.backend.entity.Reports;
import com.example.backend.exception.BusinessException;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.PenaltyRecordsService;
import com.example.backend.service.ReportsService;
import com.example.backend.service.SystemMessagesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** 举报管理 — 仅超级管理员/客服可用。 */
@Tag(name = "管理员端/举报管理", description = "举报列表、审核处理、统计")
@RestController
@RequestMapping("/admin/reports")
public class AdminReportController {

    private final ReportsService reportsService;
    private final PenaltyRecordsService penaltyRecordsService;
    private final SystemMessagesService systemMessagesService;

    public AdminReportController(
            ReportsService reportsService,
            PenaltyRecordsService penaltyRecordsService,
            SystemMessagesService systemMessagesService) {
        this.reportsService = reportsService;
        this.penaltyRecordsService = penaltyRecordsService;
        this.systemMessagesService = systemMessagesService;
    }

    private void requireSuperAdmin() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !user.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅超级管理员可操作");
        }
    }

    @Operation(summary = "举报列表（分页）")
    @GetMapping("/list")
    public Result<Page<Reports>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "targetType", required = false) Integer targetType,
            @RequestParam(value = "startTime", required = false) Long startTime,
            @RequestParam(value = "endTime", required = false) Long endTime) {
        requireSuperAdmin();
        return Result.success(
                reportsService.listReports(page, size, status, targetType, startTime, endTime));
    }

    @Operation(summary = "举报详情")
    @GetMapping("/{id}")
    public Result<Reports> detail(@PathVariable String id) {
        requireSuperAdmin();
        return Result.success(reportsService.getReportDetail(id));
    }

    @Operation(summary = "处理举报")
    @PostMapping("/{id}/process")
    public Result<Reports> process(@PathVariable String id, @RequestBody Map<String, Object> body) {
        requireSuperAdmin();
        LoginUserInfo user = AuthUserContext.get();
        Integer status = (Integer) body.get("status");
        String result = (String) body.get("result");
        Integer violationLevel = (Integer) body.get("violationLevel");
        Integer penaltyType = (Integer) body.get("penaltyType");
        Integer banDurationHours = (Integer) body.get("banDurationHours");

        // 1. 处理举报状态
        Reports report = reportsService.processReport(id, status, result, user.getAccountId());

        // 2. 举报成立时，触发处罚
        if (status != null && status == 3 && violationLevel != null && penaltyType != null) {
            String restrictedFunctions = null;
            if (body.get("restrictedFunctions") != null) {
                restrictedFunctions = body.get("restrictedFunctions").toString();
            }
            PenaltyRecords penalty =
                    penaltyRecordsService.executePenalty(
                            getReportTargetAccountId(report),
                            getReportTargetAccountType(report),
                            report.getId(),
                            violationLevel,
                            penaltyType,
                            banDurationHours,
                            restrictedFunctions,
                            user.getAccountId(),
                            "举报成立，违规等级" + violationLevel);

            // 3. 通知举报人
            systemMessagesService.createSystemMessage(
                    report.getReporterId(),
                    report.getReporterType(),
                    "举报结果",
                    "您对" + getTargetTypeName(report.getTargetType()) + "的举报已成立，感谢您的监督",
                    3,
                    "report",
                    report.getId(),
                    1);
        }

        return Result.success(report);
    }

    @Operation(summary = "举报统计（按原因分类）")
    @GetMapping("/stats")
    public Result<List<Map<String, Object>>> stats(
            @RequestParam(value = "startTime", required = false) Long startTime,
            @RequestParam(value = "endTime", required = false) Long endTime) {
        requireSuperAdmin();
        return Result.success(reportsService.statsByCategory(startTime, endTime));
    }

    /** 从举报中推断被举报对象的账号ID和类型 */
    private String getReportTargetAccountId(Reports report) {
        // targetType=1(账号)时 targetId 就是账号ID；其他类型暂时直接返回 targetId
        return report.getTargetId();
    }

    private Integer getReportTargetAccountType(Reports report) {
        // 根据 targetType 映射：1→根据具体账号表判断，简化处理
        return report.getTargetType();
    }

    private String getTargetTypeName(Integer targetType) {
        if (targetType == null) return "未知";
        switch (targetType) {
            case 1:
                return "账号";
            case 2:
                return "门店";
            case 3:
                return "订单";
            case 4:
                return "商品";
            default:
                return "未知";
        }
    }
}
