package com.example.backend.controller.user;

import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.Invoices;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.finance.InvoiceApplyRequest;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户端/发票")
@RestController
@RequestMapping("/user/invoices")
public class UserInvoiceController {

    private final InvoiceService invoiceService;

    public UserInvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    private LoginUserInfo requireUser() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return user;
    }

    @Operation(summary = "申请开票")
    @PostMapping("/apply")
    public Result<Invoices> apply(@RequestBody(required = false) InvoiceApplyRequest request) {
        LoginUserInfo user = requireUser();
        return Result.success(invoiceService.apply(user.getAccountId(), request));
    }

    @Operation(summary = "我的发票申请")
    @GetMapping("/list")
    public Result<List<Invoices>> list(
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        LoginUserInfo user = requireUser();
        return Result.success(invoiceService.listForUser(user.getAccountId(), limit));
    }
}
