package com.example.backend.controller.admin;

import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.TechnicianWithdrawals;
import com.example.backend.exception.BusinessException;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.TechnicianWithdrawalService;
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

@Tag(name = "管理员端/师傅提现")
@RestController
@RequestMapping("/admin/worker-withdrawals")
public class AdminWorkerWithdrawalController {

    private final TechnicianWithdrawalService technicianWithdrawalService;

    public AdminWorkerWithdrawalController(
            TechnicianWithdrawalService technicianWithdrawalService) {
        this.technicianWithdrawalService = technicianWithdrawalService;
    }

    private LoginUserInfo requireSuperAdmin() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !user.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅超级管理员可处理提现");
        }
        return user;
    }

    @Operation(summary = "提现单列表")
    @GetMapping("/list")
    public Result<List<TechnicianWithdrawals>> list(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        requireSuperAdmin();
        return Result.success(technicianWithdrawalService.listForAdmin(status, limit));
    }

    @Operation(summary = "审核提现单")
    @PostMapping("/{id}/review")
    public Result<TechnicianWithdrawals> review(
            @PathVariable("id") String id,
            @RequestBody(required = false) Map<String, Object> body) {
        LoginUserInfo admin = requireSuperAdmin();
        boolean approve = body != null && Boolean.TRUE.equals(body.get("approve"));
        String remark = body == null ? null : (String) body.get("remark");
        return Result.success(
                technicianWithdrawalService.review(id, admin.getAccountId(), approve, remark));
    }

    @Operation(summary = "标记打款成功")
    @PostMapping("/{id}/paid")
    public Result<TechnicianWithdrawals> markPaid(
            @PathVariable("id") String id,
            @RequestBody(required = false) Map<String, Object> body) {
        LoginUserInfo admin = requireSuperAdmin();
        String providerNo = body == null ? null : (String) body.get("providerNo");
        return Result.success(
                technicianWithdrawalService.markPaid(id, admin.getAccountId(), providerNo));
    }

    @Operation(summary = "标记打款失败（退回）")
    @PostMapping("/{id}/failed")
    public Result<TechnicianWithdrawals> markFailed(
            @PathVariable("id") String id,
            @RequestBody(required = false) Map<String, Object> body) {
        LoginUserInfo admin = requireSuperAdmin();
        String reason = body == null ? null : (String) body.get("reason");
        return Result.success(
                technicianWithdrawalService.markFailed(id, admin.getAccountId(), reason));
    }

    @Operation(summary = "高金额复核确认")
    @PostMapping("/{id}/confirm-review")
    public Result<TechnicianWithdrawals> confirmReview(@PathVariable("id") String id) {
        LoginUserInfo admin = requireSuperAdmin();
        return Result.success(technicianWithdrawalService.confirmReview(id, admin.getAccountId()));
    }

    @Operation(summary = "人工拦截（阻止打款）")
    @PostMapping("/{id}/intercept")
    public Result<TechnicianWithdrawals> intercept(
            @PathVariable("id") String id,
            @RequestBody(required = false) Map<String, Object> body) {
        LoginUserInfo admin = requireSuperAdmin();
        String reason = body == null ? null : (String) body.get("reason");
        return Result.success(
                technicianWithdrawalService.intercept(id, admin.getAccountId(), reason));
    }

    @Operation(summary = "解除人工拦截")
    @PostMapping("/{id}/unintercept")
    public Result<TechnicianWithdrawals> unintercept(@PathVariable("id") String id) {
        LoginUserInfo admin = requireSuperAdmin();
        return Result.success(
                technicianWithdrawalService.releaseIntercept(id, admin.getAccountId()));
    }
}
