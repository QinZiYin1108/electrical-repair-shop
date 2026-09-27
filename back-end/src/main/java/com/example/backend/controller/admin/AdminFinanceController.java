package com.example.backend.controller.admin;

import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.FinanceOrderSnapshots;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.finance.FinanceReportResponse;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.OrderFinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员端/财务核算")
@RestController
@RequestMapping("/admin/finance")
public class AdminFinanceController {

    private static final long DEFAULT_WINDOW_MILLIS = 30L * 24L * 60L * 60L * 1000L;

    private final OrderFinanceService orderFinanceService;

    public AdminFinanceController(OrderFinanceService orderFinanceService) {
        this.orderFinanceService = orderFinanceService;
    }

    private LoginUserInfo requireAdmin() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "需登录管理员");
        }
        return user;
    }

    @Operation(summary = "订单财务快照列表")
    @GetMapping("/snapshots")
    public Result<List<FinanceOrderSnapshots>> snapshots(
            @RequestParam(value = "orderType", required = false) Integer orderType,
            @RequestParam(value = "orderId", required = false) String orderId,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        requireAdmin();
        return Result.success(orderFinanceService.listSnapshots(orderType, orderId, limit));
    }

    @Operation(summary = "重算订单财务快照")
    @PostMapping("/snapshots/rebuild")
    public Result<FinanceOrderSnapshots> rebuild(
            @RequestParam("orderType") int orderType, @RequestParam("orderId") String orderId) {
        requireAdmin();
        return Result.success(orderFinanceService.rebuild(orderType, orderId));
    }

    @Operation(summary = "财务报表（按时间口径）")
    @GetMapping("/report")
    public Result<FinanceReportResponse> report(
            @RequestParam(value = "timeBasis", defaultValue = "PAY_TIME") String timeBasis,
            @RequestParam(value = "from", required = false) Long from,
            @RequestParam(value = "to", required = false) Long to) {
        requireAdmin();
        long toTime = to != null ? to : System.currentTimeMillis();
        long fromTime = from != null ? from : toTime - DEFAULT_WINDOW_MILLIS;
        return Result.success(orderFinanceService.report(fromTime, toTime, timeBasis));
    }
}
