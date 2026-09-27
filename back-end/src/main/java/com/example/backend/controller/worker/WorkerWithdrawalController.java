package com.example.backend.controller.worker;

import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.TechnicianWithdrawals;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.worker.TechnicianWithdrawalApplyRequest;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.TechnicianWithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "师傅端/提现")
@RequestMapping("/worker/withdrawals")
public class WorkerWithdrawalController {

    private final TechnicianWithdrawalService technicianWithdrawalService;

    public WorkerWithdrawalController(TechnicianWithdrawalService technicianWithdrawalService) {
        this.technicianWithdrawalService = technicianWithdrawalService;
    }

    @Operation(summary = "提交提现申请")
    @PostMapping("/apply")
    public Result<TechnicianWithdrawals> apply(
            @Valid @RequestBody TechnicianWithdrawalApplyRequest request) {
        LoginUserInfo worker = requireWorker();
        return Result.success(technicianWithdrawalService.apply(worker.getAccountId(), request));
    }

    @Operation(summary = "查询提现记录")
    @GetMapping("/list")
    public Result<List<TechnicianWithdrawals>> list(
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        LoginUserInfo worker = requireWorker();
        return Result.success(
                technicianWithdrawalService.listForTechnician(worker.getAccountId(), limit));
    }

    private LoginUserInfo requireWorker() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.WORKER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问师傅提现");
        }
        return user;
    }
}
