package com.example.backend.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.CreditRecords;
import com.example.backend.exception.BusinessException;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.CreditRecordsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** 用户端信用积分接口 */
@Tag(name = "用户端/信用积分", description = "用户查看信用积分与变动历史")
@RestController
@RequestMapping("/user/credit")
public class UserCreditController {

    private final CreditRecordsService creditRecordsService;

    public UserCreditController(CreditRecordsService creditRecordsService) {
        this.creditRecordsService = creditRecordsService;
    }

    private String requireUserId() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || user.getRole() != AccountRole.USER) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return user.getAccountId();
    }

    @Operation(summary = "查询当前信用积分")
    @GetMapping("/score")
    public Result<Map<String, Object>> getScore() {
        String accountId = requireUserId();
        int score = creditRecordsService.getCreditScore(accountId, 1);
        Map<String, Object> result = new HashMap<>();
        result.put("creditScore", score);
        result.put("status", score >= 60 ? "normal" : score >= 40 ? "warning" : "danger");
        return Result.success(result);
    }

    @Operation(summary = "信用积分变动历史")
    @GetMapping("/history")
    public Result<Page<CreditRecords>> history(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        String accountId = requireUserId();
        return Result.success(creditRecordsService.getCreditHistory(accountId, 1, page, size));
    }
}
