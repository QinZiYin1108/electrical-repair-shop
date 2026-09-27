package com.example.backend.controller.admin;

import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.payment.PaymentRefundResponse;
import com.example.backend.model.payment.RefundApplyRequest;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.PaymentRefundsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员端/支付退款")
@RestController
@RequestMapping("/admin/payments")
public class AdminPaymentController {

    private final PaymentRefundsService paymentRefundsService;

    public AdminPaymentController(PaymentRefundsService paymentRefundsService) {
        this.paymentRefundsService = paymentRefundsService;
    }

    private LoginUserInfo requireSuperAdmin() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !user.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅超级管理员可操作退款");
        }
        return user;
    }

    @Operation(summary = "对支付单发起渠道退款")
    @PostMapping("/{paymentNo}/refund")
    public Result<PaymentRefundResponse> refund(
            @PathVariable("paymentNo") String paymentNo,
            @Valid @RequestBody RefundApplyRequest request) {
        LoginUserInfo admin = requireSuperAdmin();
        return Result.success(
                paymentRefundsService.applyRefund(paymentNo, request, admin.getAccountId()));
    }

    @Operation(summary = "查询支付单的退款记录")
    @GetMapping("/{paymentNo}/refunds")
    public Result<List<PaymentRefundResponse>> listRefunds(
            @PathVariable("paymentNo") String paymentNo) {
        requireSuperAdmin();
        return Result.success(paymentRefundsService.listRefunds(paymentNo));
    }
}
