package com.example.backend.controller.admin;

import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.OrderSlaEvents;
import com.example.backend.exception.BusinessException;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.OrderSlaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员端/订单 SLA")
@RestController
@RequestMapping("/admin/order-sla")
public class AdminOrderSlaController {

    private final OrderSlaService orderSlaService;

    public AdminOrderSlaController(OrderSlaService orderSlaService) {
        this.orderSlaService = orderSlaService;
    }

    private LoginUserInfo requireAdmin() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "需登录管理员");
        }
        return user;
    }

    @Operation(summary = "查询订单超时事件（可按订单/状态过滤）")
    @GetMapping("/list")
    public Result<List<OrderSlaEvents>> list(
            @RequestParam(value = "orderId", required = false) String orderId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        requireAdmin();
        return Result.success(orderSlaService.listForAdmin(orderId, status, limit));
    }
}
