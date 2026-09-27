package com.example.backend.controller.user;

import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.payment.CreatePaymentIntentRequest;
import com.example.backend.model.payment.PaymentIntentResponse;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.PaymentApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "用户端/支付")
@RequestMapping("/user/payments")
public class UserPaymentController {
    private final PaymentApplicationService paymentApplicationService;

    public UserPaymentController(PaymentApplicationService paymentApplicationService) {
        this.paymentApplicationService = paymentApplicationService;
    }

    @Operation(summary = "创建支付意图")
    @PostMapping("/intents")
    public Result<PaymentIntentResponse> createIntent(
            @Valid @RequestBody CreatePaymentIntentRequest request) {
        LoginUserInfo user = requireUser();
        return Result.success(paymentApplicationService.createIntent(user.getAccountId(), request));
    }

    @Operation(summary = "查询支付状态")
    @GetMapping("/{paymentNo}")
    public Result<PaymentIntentResponse> getPayment(@PathVariable String paymentNo) {
        LoginUserInfo user = requireUser();
        return Result.success(paymentApplicationService.getIntent(user.getAccountId(), paymentNo));
    }

    private LoginUserInfo requireUser() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.USER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问用户支付信息");
        }
        return user;
    }
}
