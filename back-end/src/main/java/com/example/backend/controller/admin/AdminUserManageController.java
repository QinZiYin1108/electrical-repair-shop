package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.Images;
import com.example.backend.entity.UserAddresses;
import com.example.backend.entity.UserAccounts;
import com.example.backend.entity.UserProfiles;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.admin.AdminUserAddressItemResponse;
import com.example.backend.model.admin.AdminUserAddressUpdateRequest;
import com.example.backend.model.admin.AdminUserDetailResponse;
import com.example.backend.model.admin.AdminUserListItemResponse;
import com.example.backend.model.admin.AdminUserStatusUpdateRequest;
import com.example.backend.model.admin.AdminUserUpdateRequest;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.entity.OperationLogs;
import com.example.backend.entity.SystemMessages;
import com.example.backend.service.OperationLogsService;
import com.example.backend.service.SystemMessagesService;
import com.example.backend.service.ImagesService;
import com.example.backend.service.UserAddressesService;
import com.example.backend.service.UserAccountsService;
import com.example.backend.service.UserProfilesService;
import com.example.backend.utils.PasswordUtil;
import com.example.backend.utils.id.SnowflakeIdUtil;
import com.example.backend.utils.oss.OssUtil;
import com.example.backend.utils.upload.UploadLimitUtil;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/admin/users")
public class AdminUserManageController {

    private final UserAccountsService userAccountsService;
    private final UserProfilesService userProfilesService;
    private final ImagesService imagesService;
    private final UserAddressesService userAddressesService;
    private final OssUtil ossUtil;
    private final OperationLogsService operationLogsService;
    private final SystemMessagesService systemMessagesService;

    public AdminUserManageController(
        UserAccountsService userAccountsService,
        UserProfilesService userProfilesService,
        ImagesService imagesService,
        UserAddressesService userAddressesService,
        OssUtil ossUtil,
        OperationLogsService operationLogsService,
        SystemMessagesService systemMessagesService
    ) {
        this.userAccountsService = userAccountsService;
        this.userProfilesService = userProfilesService;
        this.imagesService = imagesService;
        this.userAddressesService = userAddressesService;
        this.ossUtil = ossUtil;
        this.operationLogsService = operationLogsService;
        this.systemMessagesService = systemMessagesService;
    }

    @GetMapping
    public Result<Page<AdminUserListItemResponse>> listUsers(
        @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
        @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
        @RequestParam(value = "keyword", required = false) String keyword
    ) {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问管理员接口");
        }
        if (pageNum <= 0) {
            pageNum = 1;
        }
        if (pageSize <= 0) {
            pageSize = 10;
        }
        LambdaQueryWrapper<UserAccounts> wrapper = new LambdaQueryWrapper<UserAccounts>()
            .eq(UserAccounts::getIsDelete, 0);
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(UserAccounts::getUsername, kw)
                .or().like(UserAccounts::getPhone, kw)
                .or().like(UserAccounts::getEmail, kw));
        }
        wrapper.orderByDesc(UserAccounts::getCreatedTime);
        Page<UserAccounts> page = userAccountsService.page(new Page<>(pageNum, pageSize), wrapper);
        List<UserAccounts> records = page.getRecords();
        List<AdminUserListItemResponse> items = new ArrayList<>();
        if (!records.isEmpty()) {
            Set<String> accountIds = new HashSet<>();
            for (UserAccounts account : records) {
                accountIds.add(account.getId());
            }
            Map<String, UserProfiles> profileMap = new HashMap<>();
            if (!accountIds.isEmpty()) {
                List<UserProfiles> profiles = userProfilesService.list(
                    new LambdaQueryWrapper<UserProfiles>()
                        .in(UserProfiles::getAccountId, accountIds)
                        .eq(UserProfiles::getIsDelete, 0)
                );
                for (UserProfiles profile : profiles) {
                    profileMap.put(profile.getAccountId(), profile);
                }
            }
            Map<String, String> avatarMap = new HashMap<>();
            if (!accountIds.isEmpty()) {
                List<Images> images = imagesService.list(
                    new LambdaQueryWrapper<Images>()
                        .eq(Images::getBusinessType, "AVATAR")
                        .in(Images::getBusinessId, accountIds)
                        .eq(Images::getIsDelete, 0)
                        .orderByDesc(Images::getCreatedTime)
                );
                for (Images image : images) {
                    String businessId = image.getBusinessId();
                    if (!avatarMap.containsKey(businessId) && StringUtils.hasText(image.getFileUrl())) {
                        avatarMap.put(businessId, image.getFileUrl());
                    }
                }
            }
            for (UserAccounts account : records) {
                AdminUserListItemResponse item = new AdminUserListItemResponse();
                item.setId(account.getId());
                item.setUsername(account.getUsername());
                item.setPhone(account.getPhone());
                item.setEmail(account.getEmail());
                item.setIsVerified(account.getIsVerified());
                item.setBalance(account.getBalance());
                item.setStatus(account.getStatus());
                item.setCreatedTime(account.getCreatedTime());
                UserProfiles profile = profileMap.get(account.getId());
                if (profile != null) {
                    item.setRealName(profile.getRealName());
                }
                String avatarUrl = avatarMap.get(account.getId());
                if (avatarUrl != null) {
                    item.setAvatarUrl(avatarUrl);
                }
                items.add(item);
            }
        }
        Page<AdminUserListItemResponse> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(items);
        return Result.success(resultPage);
    }

    @GetMapping("/{id}")
    public Result<AdminUserDetailResponse> getUserDetail(@PathVariable("id") String id) {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问管理员接口");
        }
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空");
        }
        UserAccounts account = userAccountsService.getById(id);
        if (account == null || account.getIsDelete() != null && account.getIsDelete() != 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户账号不存在");
        }
        UserProfiles profile = userProfilesService.getOne(
            new LambdaQueryWrapper<UserProfiles>()
                .eq(UserProfiles::getAccountId, id)
                .eq(UserProfiles::getIsDelete, 0),
            false
        );
        String avatarUrl = null;
        Images avatarImage = imagesService.getOne(
            new LambdaQueryWrapper<Images>()
                .eq(Images::getBusinessType, "AVATAR")
                .eq(Images::getBusinessId, id)
                .eq(Images::getIsDelete, 0)
                .orderByDesc(Images::getCreatedTime)
                .last("limit 1"),
            false
        );
        if (avatarImage != null && StringUtils.hasText(avatarImage.getFileUrl())) {
            avatarUrl = avatarImage.getFileUrl();
        }
        AdminUserDetailResponse resp = new AdminUserDetailResponse();
        resp.setId(account.getId());
        resp.setUsername(account.getUsername());
        resp.setPhone(account.getPhone());
        resp.setEmail(account.getEmail());
        resp.setStatus(account.getStatus());
        resp.setIsVerified(account.getIsVerified());
        resp.setBalance(account.getBalance());
        if (profile != null) {
            resp.setRealName(profile.getRealName());
            resp.setGender(profile.getGender());
            resp.setProfession(profile.getProfession());
            resp.setEmergencyContact(profile.getEmergencyContact());
            resp.setEmergencyPhone(profile.getEmergencyPhone());
            resp.setIdCard(profile.getIdCard());
            if (profile.getBirthday() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                resp.setBirthday(sdf.format(new Date(profile.getBirthday())));
            }
        }
        resp.setAvatarUrl(avatarUrl);
        return Result.success(resp);
    }

    @GetMapping("/{id}/addresses")
    public Result<List<AdminUserAddressItemResponse>> listUserAddresses(@PathVariable("id") String id) {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问用户地址");
        }
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空");
        }
        UserAccounts account = userAccountsService.getById(id);
        if (account == null || account.getIsDelete() != null && account.getIsDelete() != 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户不存在");
        }
        List<UserAddresses> addresses = userAddressesService.list(
            new LambdaQueryWrapper<UserAddresses>()
                .eq(UserAddresses::getAccountId, id)
                .eq(UserAddresses::getIsDelete, 0)
                .orderByDesc(UserAddresses::getIsDefault)
                .orderByDesc(UserAddresses::getUpdatedTime)
                .orderByDesc(UserAddresses::getCreatedTime)
        );
        List<AdminUserAddressItemResponse> items = new ArrayList<>();
        for (UserAddresses address : addresses) {
            items.add(buildUserAddressItem(address));
        }
        return Result.success(items);
    }

    @PostMapping("/{id}/addresses/{addressId}/set-default")
    public Result<Void> setUserDefaultAddress(
        @PathVariable("id") String id,
        @PathVariable("addressId") String addressId
    ) {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问用户地址");
        }
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空");
        }
        if (!StringUtils.hasText(addressId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "地址ID不能为空");
        }
        UserAccounts account = userAccountsService.getById(id);
        if (account == null || account.getIsDelete() != null && account.getIsDelete() != 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户不存在");
        }
        userAddressesService.adminSetUserDefaultAddress(id, addressId);
        long now = System.currentTimeMillis();

        OperationLogs log = new OperationLogs();
        log.setId("OL" + now + "UAD");
        log.setOperatorId(user.getAccountId());
        log.setOperatorType(3);
        log.setOperatorName(user.getAccountId());
        log.setOperationType("UPDATE");
        log.setOperationDesc("管理员设置用户默认地址");
        log.setModuleName("ADMIN_USER");
        log.setRequestMethod("POST");
        log.setRequestUrl("/admin/users/" + id + "/addresses/" + addressId + "/set-default");
        log.setRequestParams("{\"addressId\":\"" + addressId + "\"}");
        log.setStatus(1);
        log.setCreatedTime(now);
        log.setIpAddress("");
        log.setVersion(1);
        log.setIsDelete(0);
        operationLogsService.save(log);

        SystemMessages message = new SystemMessages();
        message.setId("SM" + now + "UAD");
        message.setReceiverId(id);
        message.setReceiverType(1);
        message.setTitle("默认地址已更新");
        message.setContent("管理员已为您更新默认地址，请留意后续订单服务地址。");
        message.setMessageType(3);
        message.setBusinessType("ADMIN_USER_ADDRESS_DEFAULT");
        message.setBusinessId(addressId);
        message.setPriority(2);
        message.setIsRead(0);
        message.setCreatedTime(now);
        message.setUpdatedTime(now);
        message.setVersion(1);
        message.setIsDelete(0);
        systemMessagesService.save(message);
        return Result.success();
    }

    @PostMapping("/{id}/addresses/{addressId}/delete")
    public Result<Void> deleteUserAddress(
        @PathVariable("id") String id,
        @PathVariable("addressId") String addressId
    ) {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问用户地址");
        }
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空");
        }
        if (!StringUtils.hasText(addressId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "地址ID不能为空");
        }
        UserAccounts account = userAccountsService.getById(id);
        if (account == null || account.getIsDelete() != null && account.getIsDelete() != 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户不存在");
        }
        userAddressesService.adminDeleteUserAddress(id, addressId);
        long now = System.currentTimeMillis();

        OperationLogs log = new OperationLogs();
        log.setId("OL" + now + "UAR");
        log.setOperatorId(user.getAccountId());
        log.setOperatorType(3);
        log.setOperatorName(user.getAccountId());
        log.setOperationType("DELETE");
        log.setOperationDesc("管理员删除用户地址");
        log.setModuleName("ADMIN_USER");
        log.setRequestMethod("POST");
        log.setRequestUrl("/admin/users/" + id + "/addresses/" + addressId + "/delete");
        log.setRequestParams("{\"addressId\":\"" + addressId + "\"}");
        log.setStatus(1);
        log.setCreatedTime(now);
        log.setIpAddress("");
        log.setVersion(1);
        log.setIsDelete(0);
        operationLogsService.save(log);

        SystemMessages message = new SystemMessages();
        message.setId("SM" + now + "UAR");
        message.setReceiverId(id);
        message.setReceiverType(1);
        message.setTitle("地址信息已删除");
        message.setContent("管理员已删除您的一个地址信息，如非本人操作请及时联系客服。");
        message.setMessageType(3);
        message.setBusinessType("ADMIN_USER_ADDRESS_DELETE");
        message.setBusinessId(addressId);
        message.setPriority(2);
        message.setIsRead(0);
        message.setCreatedTime(now);
        message.setUpdatedTime(now);
        message.setVersion(1);
        message.setIsDelete(0);
        systemMessagesService.save(message);
        return Result.success();
    }

    @PostMapping("/{id}/addresses/{addressId}/update")
    public Result<Void> updateUserAddress(
        @PathVariable("id") String id,
        @PathVariable("addressId") String addressId,
        @Valid @RequestBody AdminUserAddressUpdateRequest request
    ) {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问管理员接口");
        }
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空");
        }
        if (!StringUtils.hasText(addressId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "地址ID不能为空");
        }
        UserAccounts account = userAccountsService.getById(id);
        if (account == null || account.getIsDelete() != null && account.getIsDelete() != 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户账号不存在");
        }
        userAddressesService.adminUpdateUserAddress(id, addressId, request);
        long now = System.currentTimeMillis();

        OperationLogs log = new OperationLogs();
        log.setId("OL" + now + "UAU");
        log.setOperatorId(user.getAccountId());
        log.setOperatorType(3);
        log.setOperatorName(user.getAccountId());
        log.setOperationType("UPDATE");
        log.setOperationDesc("管理员修改用户地址");
        log.setModuleName("ADMIN_USER");
        log.setRequestMethod("POST");
        log.setRequestUrl("/admin/users/" + id + "/addresses/" + addressId + "/update");
        log.setRequestParams("{\"addressId\":\"" + addressId + "\"}");
        log.setStatus(1);
        log.setCreatedTime(now);
        log.setIpAddress("");
        log.setVersion(1);
        log.setIsDelete(0);
        operationLogsService.save(log);

        SystemMessages message = new SystemMessages();
        message.setId("SM" + now + "UAU");
        message.setReceiverId(id);
        message.setReceiverType(1);
        message.setTitle("地址信息已被管理员更新");
        message.setContent("您的一条地址信息已由管理员更新，如有疑问请联系平台客服。");
        message.setMessageType(3);
        message.setBusinessType("ADMIN_USER_ADDRESS_UPDATE");
        message.setBusinessId(addressId);
        message.setPriority(2);
        message.setIsRead(0);
        message.setCreatedTime(now);
        message.setUpdatedTime(now);
        message.setVersion(1);
        message.setIsDelete(0);
        systemMessagesService.save(message);
        return Result.success();
    }

    @PostMapping("/{id}/update")
    public Result<Void> updateUser(
        @PathVariable("id") String id,
        @Valid @RequestBody AdminUserUpdateRequest request
    ) {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问管理员接口");
        }
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空");
        }
        UserAccounts account = userAccountsService.getById(id);
        if (account == null || account.getIsDelete() != null && account.getIsDelete() != 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户账号不存在");
        }
        String oldUsername = account.getUsername();
        String oldPhone = account.getPhone();
        long now = System.currentTimeMillis();
        account.setUsername(request.getUsername());
        account.setPhone(request.getPhone());
        account.setUpdatedTime(now);
        userAccountsService.updateById(account);
        UserProfiles profile = userProfilesService.getOne(
            new LambdaQueryWrapper<UserProfiles>()
                .eq(UserProfiles::getAccountId, id)
                .eq(UserProfiles::getIsDelete, 0),
            false
        );
        boolean isNew = profile == null;
        if (isNew) {
            profile = new UserProfiles();
            profile.setId(SnowflakeIdUtil.nextUserProfileId());
            profile.setAccountId(id);
            profile.setCreatedTime(now);
            profile.setIsDelete(0);
        }
        profile.setProfession(request.getProfession());
        profile.setEmergencyContact(request.getEmergencyContact());
        profile.setEmergencyPhone(request.getEmergencyPhone());
        profile.setUpdatedTime(now);
        if (isNew) {
            userProfilesService.save(profile);
        } else {
            userProfilesService.updateById(profile);
        }
        OperationLogs log = new OperationLogs();
        log.setId("OL" + now + "U");
        log.setOperatorId(user.getAccountId());
        log.setOperatorType(3);
        log.setOperatorName(user.getAccountId());
        log.setOperationType("UPDATE");
        log.setOperationDesc("管理员修改用户基础信息");
        log.setModuleName("ADMIN_USER");
        log.setRequestMethod("POST");
        log.setRequestUrl("/admin/users/" + id + "/update");
        log.setRequestParams("{\"oldUsername\":\"" + oldUsername + "\",\"newUsername\":\"" + request.getUsername() + "\",\"oldPhone\":\"" + oldPhone + "\",\"newPhone\":\"" + request.getPhone() + "\"}");
        log.setStatus(1);
        log.setCreatedTime(now);
        log.setIpAddress("");
        log.setVersion(1);
        log.setIsDelete(0);
        operationLogsService.save(log);
        SystemMessages message = new SystemMessages();
        message.setId("SM" + now + "UB");
        message.setReceiverId(id);
        message.setReceiverType(1);
        message.setTitle("账号信息已被管理员更新");
        message.setContent("您的账户昵称、手机号或个人信息已由管理员更新，如有疑问请联系平台客服。");
        message.setMessageType(3);
        message.setBusinessType("ADMIN_USER_UPDATE");
        message.setBusinessId(id);
        message.setPriority(2);
        message.setIsRead(0);
        message.setCreatedTime(now);
        message.setUpdatedTime(now);
        message.setVersion(1);
        message.setIsDelete(0);
        systemMessagesService.save(message);
        return Result.success();
    }

    @PostMapping("/{id}/password/init")
    public Result<Void> initUserPassword(@PathVariable("id") String id) {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "\u672a\u767b\u5f55");
        }
        if (user.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "\u65e0\u6743\u8bbf\u95ee\u7ba1\u7406\u5458\u63a5\u53e3");
        }
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "\u7528\u6237ID\u4e0d\u80fd\u4e3a\u7a7a");
        }

        UserAccounts account = userAccountsService.getById(id);
        if (account == null || account.getIsDelete() != null && account.getIsDelete() != 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "\u7528\u6237\u8d26\u53f7\u4e0d\u5b58\u5728");
        }
        if (!StringUtils.hasText(account.getEmail())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "\u7528\u6237\u672a\u7ed1\u5b9a\u90ae\u7bb1\uff0c\u65e0\u6cd5\u521d\u59cb\u5316\u5bc6\u7801");
        }

        long now = System.currentTimeMillis();
        String salt = PasswordUtil.generateSalt(16);
        String password = "123456";
        account.setSalt(salt);
        account.setPassword(PasswordUtil.hashPassword(password, salt));
        account.setUpdatedTime(now);
        boolean updated = userAccountsService.updateById(account);
        if (!updated) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "\u521d\u59cb\u5316\u5bc6\u7801\u5931\u8d25");
        }

        OperationLogs log = new OperationLogs();
        log.setId("OL" + now + "UP");
        log.setOperatorId(user.getAccountId());
        log.setOperatorType(3);
        log.setOperatorName(user.getAccountId());
        log.setOperationType("UPDATE");
        log.setOperationDesc("\u7ba1\u7406\u5458\u521d\u59cb\u5316\u7528\u6237\u767b\u5f55\u5bc6\u7801");
        log.setModuleName("ADMIN_USER");
        log.setRequestMethod("POST");
        log.setRequestUrl("/admin/users/" + id + "/password/init");
        log.setRequestParams("{\"email\":\"" + account.getEmail() + "\",\"passwordInitialized\":true}");
        log.setStatus(1);
        log.setCreatedTime(now);
        log.setIpAddress("");
        log.setVersion(1);
        log.setIsDelete(0);
        operationLogsService.save(log);

        SystemMessages message = new SystemMessages();
        message.setId("SM" + now + "UP");
        message.setReceiverId(id);
        message.setReceiverType(1);
        message.setTitle("\u767b\u5f55\u5bc6\u7801\u5df2\u88ab\u7ba1\u7406\u5458\u91cd\u7f6e");
        message.setContent("\u60a8\u7684\u767b\u5f55\u5bc6\u7801\u5df2\u88ab\u7ba1\u7406\u5458\u91cd\u7f6e\uff0c\u5982\u975e\u672c\u4eba\u64cd\u4f5c\u8bf7\u5c3d\u5feb\u8054\u7cfb\u5e73\u53f0\u5ba2\u670d\u3002");
        message.setMessageType(3);
        message.setBusinessType("ADMIN_USER_PASSWORD_INIT");
        message.setBusinessId(id);
        message.setPriority(1);
        message.setIsRead(0);
        message.setCreatedTime(now);
        message.setUpdatedTime(now);
        message.setVersion(1);
        message.setIsDelete(0);
        systemMessagesService.save(message);
        return Result.success();
    }

    @PostMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadUserAvatar(
        @PathVariable("id") String id,
        @RequestPart("file") MultipartFile file
    ) {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问管理员接口");
        }
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }
        UploadLimitUtil.validateImageSize(file);
        UserAccounts account = userAccountsService.getById(id);
        if (account == null || account.getIsDelete() != null && account.getIsDelete() != 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户账号不存在");
        }
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null) {
            int idx = originalFilename.lastIndexOf('.');
            if (idx >= 0 && idx < originalFilename.length() - 1) {
                ext = originalFilename.substring(idx);
            }
        }
        String objectName = "avatars/" + id + "/" + java.util.UUID.randomUUID() + ext;
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
            BufferedImage bufferedImage = javax.imageio.ImageIO.read(file.getInputStream());
            if (bufferedImage != null) {
                image.setWidth(bufferedImage.getWidth());
                image.setHeight(bufferedImage.getHeight());
            }
        } catch (IOException ignored) {
        }
        String adminAccountId = user.getAccountId();
        image.setUploaderId(adminAccountId);
        image.setUploaderType(3);
        image.setBusinessType("AVATAR");
        image.setBusinessId(id);
        long now = System.currentTimeMillis();
        image.setCreatedTime(now);
        image.setIsDelete(0);
        imagesService.save(image);
        OperationLogs log = new OperationLogs();
        log.setId("OL" + now + "UA");
        log.setOperatorId(adminAccountId);
        log.setOperatorType(3);
        log.setOperatorName(adminAccountId);
        log.setOperationType("UPDATE");
        log.setOperationDesc("管理员修改用户头像");
        log.setModuleName("ADMIN_USER");
        log.setRequestMethod("POST");
        log.setRequestUrl("/admin/users/" + id + "/avatar");
        log.setRequestParams("{\"filename\":\"" + originalFilename + "\"}");
        log.setStatus(1);
        log.setCreatedTime(now);
        log.setIpAddress("");
        log.setVersion(1);
        log.setIsDelete(0);
        operationLogsService.save(log);
        SystemMessages message = new SystemMessages();
        message.setId("SM" + now + "UA");
        message.setReceiverId(id);
        message.setReceiverType(1);
        message.setTitle("头像已被管理员修改");
        message.setContent("您的账户头像已由管理员修改，如有疑问请联系平台客服。");
        message.setMessageType(3);
        message.setBusinessType("ADMIN_USER_AVATAR");
        message.setBusinessId(id);
        message.setPriority(2);
        message.setIsRead(0);
        message.setCreatedTime(now);
        message.setUpdatedTime(now);
        message.setVersion(1);
        message.setIsDelete(0);
        systemMessagesService.save(message);
        return Result.success(url);
    }

    private AdminUserAddressItemResponse buildUserAddressItem(UserAddresses address) {
        AdminUserAddressItemResponse item = new AdminUserAddressItemResponse();
        item.setId(address.getId());
        item.setContactName(address.getContactName());
        item.setContactPhone(address.getContactPhone());
        item.setProvince(address.getProvince());
        item.setCity(address.getCity());
        item.setDistrict(address.getDistrict());
        item.setStreet(address.getStreet());
        item.setDetailedAddress(address.getDetailedAddress());
        item.setPostalCode(address.getPostalCode());
        item.setLongitude(toPlainString(address.getLongitude()));
        item.setLatitude(toPlainString(address.getLatitude()));
        item.setIsDefault(address.getIsDefault());
        item.setAddressType(address.getAddressType());
        item.setAddressTypeName(mapAddressTypeName(address.getAddressType()));
        item.setFullAddress(buildFullAddress(address));
        item.setCreatedTime(address.getCreatedTime());
        item.setUpdatedTime(address.getUpdatedTime());
        return item;
    }

    private String mapAddressTypeName(Integer addressType) {
        if (addressType == null || addressType == 1) {
            return "家庭";
        }
        if (addressType == 2) {
            return "公司";
        }
        return "其他";
    }

    private String buildFullAddress(UserAddresses address) {
        return safe(address.getProvince())
            + safe(address.getCity())
            + safe(address.getDistrict())
            + safe(address.getStreet());
    }

    private String toPlainString(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @PostMapping("/{id}/status")
    public Result<Void> updateUserStatus(
        @PathVariable("id") String id,
        @Valid @RequestBody AdminUserStatusUpdateRequest request
    ) {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问管理员接口");
        }
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空");
        }
        Integer targetStatus = request.getStatus();
        if (targetStatus == null || (targetStatus != 1 && targetStatus != 2)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "状态仅支持正常或冻结");
        }
        UserAccounts account = userAccountsService.getById(id);
        if (account == null || account.getIsDelete() != null && account.getIsDelete() != 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户账号不存在");
        }
        Integer currentStatus = account.getStatus();
        if (currentStatus == null || (currentStatus != 1 && currentStatus != 2)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前状态不支持该操作");
        }
        if (currentStatus.equals(targetStatus)) {
            return Result.success();
        }
        account.setStatus(targetStatus);
        long now = System.currentTimeMillis();
        account.setUpdatedTime(now);
        userAccountsService.updateById(account);
        OperationLogs log = new OperationLogs();
        log.setId("OL" + now + "US");
        log.setOperatorId(user.getAccountId());
        log.setOperatorType(3);
        log.setOperatorName(user.getAccountId());
        log.setOperationType("UPDATE");
        log.setOperationDesc("管理员修改用户账号状态");
        log.setModuleName("ADMIN_USER");
        log.setRequestMethod("POST");
        log.setRequestUrl("/admin/users/" + id + "/status");
        log.setRequestParams("{\"oldStatus\":" + currentStatus + ",\"newStatus\":" + targetStatus + "}");
        log.setStatus(1);
        log.setCreatedTime(now);
        log.setIpAddress("");
        log.setVersion(1);
        log.setIsDelete(0);
        operationLogsService.save(log);
        String statusText;
        if (targetStatus == 1) {
            statusText = "正常";
        } else {
            statusText = "冻结";
        }
        SystemMessages message = new SystemMessages();
        message.setId("SM" + now + "US");
        message.setReceiverId(id);
        message.setReceiverType(1);
        message.setTitle("账号状态已被管理员调整");
        message.setContent("您的账户状态已被调整为：" + statusText + "，如有疑问请联系平台客服。");
        message.setMessageType(3);
        message.setBusinessType("ADMIN_USER_STATUS");
        message.setBusinessId(id);
        message.setPriority(1);
        message.setIsRead(0);
        message.setCreatedTime(now);
        message.setUpdatedTime(now);
        message.setVersion(1);
        message.setIsDelete(0);
        systemMessagesService.save(message);
        return Result.success();
    }
}
