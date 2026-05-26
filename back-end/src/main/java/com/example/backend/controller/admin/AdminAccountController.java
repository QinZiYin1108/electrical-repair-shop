package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.AdminAccounts;
import com.example.backend.entity.Images;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.admin.AdminAccountInfoResponse;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.AdminAccountsService;
import com.example.backend.service.ImagesService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/account")
public class AdminAccountController {

    private final AdminAccountsService adminAccountsService;
    private final ImagesService imagesService;

    public AdminAccountController(AdminAccountsService adminAccountsService, ImagesService imagesService) {
        this.adminAccountsService = adminAccountsService;
        this.imagesService = imagesService;
    }

    @GetMapping("/me")
    public Result<AdminAccountInfoResponse> getCurrentAdmin() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问管理员信息");
        }
        String accountId = user.getAccountId();
        AdminAccounts admin = adminAccountsService.getById(accountId);
        if (admin == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "管理员账号不存在");
        }
        String avatarUrl = null;
        Images avatarImage = imagesService.getOne(
            new LambdaQueryWrapper<Images>()
                .eq(Images::getBusinessType, "AVATAR")
                .eq(Images::getBusinessId, accountId)
                .eq(Images::getIsDelete, 0)
                .orderByDesc(Images::getCreatedTime)
                .last("limit 1"),
            false
        );
        if (avatarImage != null) {
            avatarUrl = avatarImage.getFileUrl();
        }
        AdminAccountInfoResponse resp = new AdminAccountInfoResponse();
        resp.setId(admin.getId());
        resp.setUsername(admin.getUsername());
        resp.setEmail(admin.getEmail());
        resp.setAdminType(admin.getAdminType());
        resp.setAccountStatus(admin.getAccountStatus());
        resp.setAvatarUrl(avatarUrl);
        return Result.success(resp);
    }
}

