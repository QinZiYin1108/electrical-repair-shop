package com.example.backend.controller.worker;

import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.TechnicianBindings;
import com.example.backend.exception.BusinessException;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.TechnicianBindingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "师傅端/门店绑定")
@RequestMapping("/worker/binding")
public class WorkerBindingController {

    private final TechnicianBindingsService technicianBindingsService;

    public WorkerBindingController(TechnicianBindingsService technicianBindingsService) {
        this.technicianBindingsService = technicianBindingsService;
    }

    @Operation(summary = "查询Status")
    @GetMapping("/status")
    public Result<TechnicianBindings> getStatus() {
        LoginUserInfo user = requireWorker();
        TechnicianBindings binding =
                technicianBindingsService.getOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                                        TechnicianBindings>()
                                .eq(TechnicianBindings::getTechnicianId, user.getAccountId())
                                .in(TechnicianBindings::getStatus, 1, 2, 3)
                                .eq(TechnicianBindings::getIsDelete, 0)
                                .orderByDesc(TechnicianBindings::getCreatedTime)
                                .last("LIMIT 1"));
        return Result.success(binding);
    }

    @Operation(summary = "提交accept")
    @PostMapping("/accept")
    public Result<TechnicianBindings> accept() {
        LoginUserInfo user = requireWorker();
        return Result.success(technicianBindingsService.accept(user.getAccountId()));
    }

    @Operation(summary = "审核拒绝")
    @PostMapping("/reject")
    public Result<TechnicianBindings> reject() {
        LoginUserInfo user = requireWorker();
        return Result.success(technicianBindingsService.reject(user.getAccountId()));
    }

    @Operation(summary = "提交request解绑")
    @PostMapping("/request-unbind")
    public Result<TechnicianBindings> requestUnbind() {
        LoginUserInfo user = requireWorker();
        return Result.success(technicianBindingsService.requestUnbind(user.getAccountId()));
    }

    private LoginUserInfo requireWorker() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.WORKER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问师傅接口");
        }
        return user;
    }
}
