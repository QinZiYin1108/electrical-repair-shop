package com.example.backend.controller.admin;

import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.Invoices;
import com.example.backend.exception.BusinessException;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.InvoiceService;
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

@Tag(name = "管理员端/发票")
@RestController
@RequestMapping("/admin/invoices")
public class AdminInvoiceController {

    private final InvoiceService invoiceService;

    public AdminInvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    private LoginUserInfo requireAdmin() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "需登录管理员");
        }
        return user;
    }

    @Operation(summary = "发票列表")
    @GetMapping("/list")
    public Result<List<Invoices>> list(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "orderType", required = false) Integer orderType,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        requireAdmin();
        return Result.success(invoiceService.listForAdmin(status, orderType, limit));
    }

    @Operation(summary = "开票")
    @PostMapping("/{id}/issue")
    public Result<Invoices> issue(
            @PathVariable("id") String id,
            @RequestBody(required = false) Map<String, Object> body) {
        LoginUserInfo admin = requireAdmin();
        return Result.success(
                invoiceService.issue(id, admin.getAccountId(), str(body, "invoiceUrl")));
    }

    @Operation(summary = "驳回申请")
    @PostMapping("/{id}/reject")
    public Result<Invoices> reject(
            @PathVariable("id") String id,
            @RequestBody(required = false) Map<String, Object> body) {
        LoginUserInfo admin = requireAdmin();
        return Result.success(invoiceService.reject(id, admin.getAccountId(), str(body, "reason")));
    }

    @Operation(summary = "红冲")
    @PostMapping("/{id}/red")
    public Result<Invoices> red(
            @PathVariable("id") String id,
            @RequestBody(required = false) Map<String, Object> body) {
        LoginUserInfo admin = requireAdmin();
        return Result.success(invoiceService.red(id, admin.getAccountId(), str(body, "reason")));
    }

    private String str(Map<String, Object> body, String key) {
        if (body == null) {
            return null;
        }
        Object value = body.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
