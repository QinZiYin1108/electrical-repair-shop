package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.CreditRecords;
import com.example.backend.exception.BusinessException;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.CreditRecordsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** 信用积分管理 — 超级管理员/客服可用。 */
@Tag(name = "管理员端/信用积分", description = "信用积分查询与历史记录")
@RestController
@RequestMapping("/admin/credits")
public class AdminCreditController {

    private final CreditRecordsService creditRecordsService;

    public AdminCreditController(CreditRecordsService creditRecordsService) {
        this.creditRecordsService = creditRecordsService;
    }

    private void requirePrivileged() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null
                || (!user.isSuperAdmin()
                        && (user.getAdminRole() == null || user.getAdminRole() != 3))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅超级管理员或客服可操作");
        }
    }

    @Operation(summary = "查询账号信用积分")
    @GetMapping("/{accountId}")
    public Result<Map<String, Object>> getScore(
            @PathVariable String accountId,
            @RequestParam(value = "accountType", defaultValue = "1") Integer accountType) {
        requirePrivileged();
        int score = creditRecordsService.getCreditScore(accountId, accountType);
        Map<String, Object> result = new HashMap<>();
        result.put("accountId", accountId);
        result.put("accountType", accountType);
        result.put("creditScore", score);
        result.put("status", score >= 60 ? "normal" : score >= 40 ? "warning" : "danger");
        return Result.success(result);
    }

    @Operation(summary = "信用积分变动历史")
    @GetMapping("/{accountId}/history")
    public Result<Page<CreditRecords>> history(
            @PathVariable String accountId,
            @RequestParam(value = "accountType", defaultValue = "1") Integer accountType,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        requirePrivileged();
        return Result.success(
                creditRecordsService.getCreditHistory(accountId, accountType, page, size));
    }

    @Operation(summary = "手动恢复积分（仅超级管理员）")
    @PostMapping("/{accountId}/recover")
    public Result<Map<String, Object>> manuallyRecover(
            @PathVariable String accountId, @RequestBody Map<String, Object> body) {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !user.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅超级管理员可操作");
        }

        Integer accountType = (Integer) body.get("accountType");
        Integer score = (Integer) body.get("score");
        String reason = (String) body.get("reason");

        if (accountType == null || score == null || score <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请提供有效的 accountType 和 score（正数）");
        }

        int newScore =
                creditRecordsService.changeScore(
                        accountId, accountType, score, 2, "管理员手动恢复：" + reason, null);

        Map<String, Object> result = new HashMap<>();
        result.put("accountId", accountId);
        result.put("newScore", newScore);
        return Result.success(result);
    }
}
