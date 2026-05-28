package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.AdminAccounts;
import com.example.backend.entity.AdminProfiles;
import com.example.backend.entity.Images;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.admin.AdminProfileDetailResponse;
import com.example.backend.model.admin.AdminProfileUpdateRequest;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.AdminAccountsService;
import com.example.backend.service.AdminProfilesService;
import com.example.backend.service.ImagesService;
import com.example.backend.utils.oss.OssUtil;
import com.example.backend.utils.upload.UploadLimitUtil;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.imageio.ImageIO;

@RestController
@RequestMapping("/admin/profile")
public class AdminProfileController {

    private final AdminAccountsService adminAccountsService;
    private final AdminProfilesService adminProfilesService;
    private final ImagesService imagesService;
    private final OssUtil ossUtil;

    public AdminProfileController(
        AdminAccountsService adminAccountsService,
        AdminProfilesService adminProfilesService,
        ImagesService imagesService,
        OssUtil ossUtil
    ) {
        this.adminAccountsService = adminAccountsService;
        this.adminProfilesService = adminProfilesService;
        this.imagesService = imagesService;
        this.ossUtil = ossUtil;
    }

    @GetMapping("/me")
    public Result<AdminProfileDetailResponse> getProfile() {
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
        AdminProfiles profile = adminProfilesService.getOne(
            new LambdaQueryWrapper<AdminProfiles>()
                .eq(AdminProfiles::getAccountId, accountId)
                .eq(AdminProfiles::getIsDelete, 0),
            false
        );
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
        AdminProfileDetailResponse resp = new AdminProfileDetailResponse();
        resp.setId(admin.getId());
        resp.setUsername(admin.getUsername());
        resp.setPhone(admin.getPhone());
        resp.setEmail(admin.getEmail());
        resp.setAdminType(admin.getAdminType());
        resp.setAdminRole(admin.getAdminRole());
        resp.setAccountStatus(admin.getAccountStatus());
        if (profile != null) {
            resp.setRealName(profile.getRealName());
            resp.setDepartment(profile.getDepartment());
            resp.setPosition(profile.getPosition());
        }
        resp.setAvatarUrl(avatarUrl);
        return Result.success(resp);
    }

    @PostMapping("/update")
    public Result<Void> updateProfile(@Valid @RequestBody AdminProfileUpdateRequest request) {
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
        long now = System.currentTimeMillis();
        admin.setUsername(request.getUsername());
        admin.setPhone(request.getPhone());
        admin.setUpdatedTime(now);
        adminAccountsService.updateById(admin);

        AdminProfiles profile = adminProfilesService.getOne(
            new LambdaQueryWrapper<AdminProfiles>()
                .eq(AdminProfiles::getAccountId, accountId)
                .eq(AdminProfiles::getIsDelete, 0),
            false
        );
        boolean isNew = profile == null;
        if (isNew) {
            profile = new AdminProfiles();
            profile.setId("AP" + now);
            profile.setAccountId(accountId);
            profile.setCreatedTime(now);
            profile.setIsDelete(0);
        }
        profile.setRealName(request.getRealName());
        profile.setDepartment(request.getDepartment());
        profile.setPosition(request.getPosition());
        profile.setUpdatedTime(now);
        if (isNew) {
            adminProfilesService.save(profile);
        } else {
            adminProfilesService.updateById(profile);
        }
        return Result.success();
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadAvatar(@RequestPart("file") MultipartFile file) {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问管理员信息");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }
        UploadLimitUtil.validateImageSize(file);
        String accountId = user.getAccountId();
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null) {
            int idx = originalFilename.lastIndexOf('.');
            if (idx >= 0 && idx < originalFilename.length() - 1) {
                ext = originalFilename.substring(idx);
            }
        }
        String objectName = "avatars/" + accountId + "/" + UUID.randomUUID() + ext;
        String url;
        try (InputStream in = file.getInputStream()) {
            url = ossUtil.upload(objectName, in);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取上传文件失败");
        }
        Images image = new Images();
        image.setId("IM" + System.currentTimeMillis());
        image.setOriginalName(originalFilename);
        image.setFileName(objectName);
        image.setFilePath(objectName);
        image.setFileUrl(url);
        image.setFileSize(file.getSize());
        image.setMimeType(file.getContentType());
        try {
            BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
            if (bufferedImage != null) {
                image.setWidth(bufferedImage.getWidth());
                image.setHeight(bufferedImage.getHeight());
            }
        } catch (IOException ignored) {
        }
        image.setUploaderId(accountId);
        image.setUploaderType(3);
        image.setBusinessType("AVATAR");
        image.setBusinessId(accountId);
        image.setCreatedTime(System.currentTimeMillis());
        image.setIsDelete(0);
        imagesService.save(image);
        return Result.success(url);
    }
}
