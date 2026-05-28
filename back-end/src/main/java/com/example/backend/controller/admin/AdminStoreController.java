package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.*;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.admin.*;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.*;
import com.example.backend.utils.PasswordUtil;
import com.example.backend.utils.id.SnowflakeIdUtil;
import com.example.backend.utils.oss.OssUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/stores")
public class AdminStoreController {

    private static final int ADMIN_OPERATOR_TYPE = 3;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Value("${baidu.map.ak:}")
    private String baiduAk;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final StoresService storesService;
    private final StoreBusinessHoursService storeBusinessHoursService;
    private final TechnicianAccountsService technicianAccountsService;
    private final AdminAccountsService adminAccountsService;
    private final ImagesService imagesService;
    private final OperationLogsService operationLogsService;
    private final OssUtil ossUtil;

    public AdminStoreController(
            StoresService storesService,
            StoreBusinessHoursService storeBusinessHoursService,
            TechnicianAccountsService technicianAccountsService,
            AdminAccountsService adminAccountsService,
            ImagesService imagesService,
            OperationLogsService operationLogsService,
            OssUtil ossUtil
    ) {
        this.storesService = storesService;
        this.storeBusinessHoursService = storeBusinessHoursService;
        this.technicianAccountsService = technicianAccountsService;
        this.adminAccountsService = adminAccountsService;
        this.imagesService = imagesService;
        this.operationLogsService = operationLogsService;
        this.ossUtil = ossUtil;
    }

    // ==================== 门店 CRUD ====================

    @GetMapping
    public Result<Page<AdminStoreResponse>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) Integer businessStatus
    ) {
        LoginUserInfo user = AuthUserContext.get();
        LambdaQueryWrapper<Stores> wrapper = new LambdaQueryWrapper<>();

        // 门店管理员只能看到自己的门店
        if (user.isStoreAdmin() && user.getStoreId() != null) {
            wrapper.eq(Stores::getId, user.getStoreId());
        }

        if (StringUtils.hasText(keyword)) {
            wrapper.like(Stores::getName, keyword);
        }
        if (auditStatus != null) {
            wrapper.eq(Stores::getAuditStatus, auditStatus);
        }
        if (businessStatus != null) {
            wrapper.eq(Stores::getBusinessStatus, businessStatus);
        }
        wrapper.orderByDesc(Stores::getCreatedTime);

        Page<Stores> storePage = storesService.page(new Page<>(page, size), wrapper);
        Page<AdminStoreResponse> result = new Page<>(page, size, storePage.getTotal());
        result.setRecords(storePage.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<AdminStoreResponse> detail(@PathVariable String id) {
        LoginUserInfo user = AuthUserContext.get();
        Stores store = storesService.getById(id);
        if (store == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "门店不存在");
        }
        // 门店管理员只能查看自己的门店
        if (user.isStoreAdmin() && !id.equals(user.getStoreId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看其他门店");
        }
        return Result.success(toResponse(store));
    }

    @PostMapping("/create")
    public Result<AdminStoreResponse> create(@Valid @RequestBody AdminStoreCreateRequest request) {
        LoginUserInfo user = AuthUserContext.get();

        // 仅超级管理员可创建门店
        if (!user.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅超级管理员可创建门店");
        }

        // 1. 创建门店管理员账号
        AdminAccounts storeAdmin = new AdminAccounts();
        storeAdmin.setId(SnowflakeIdUtil.nextAdminId());
        storeAdmin.setUsername(request.getAdminName());
        storeAdmin.setPhone(request.getAdminPhone());
        storeAdmin.setEmail(request.getAdminEmail());
        storeAdmin.setAdminType(2);          // 普通管理员
        storeAdmin.setAdminRole(2);          // 门店管理员
        storeAdmin.setPermissions("[]");
        storeAdmin.setAccountStatus(1);
        storeAdmin.setIsFirstLogin(1);

        String salt = PasswordUtil.generateSalt(16);
        storeAdmin.setSalt(salt);
        storeAdmin.setPasswordHash(PasswordUtil.hashPassword(request.getAdminPassword(), salt));

        long now = System.currentTimeMillis();
        storeAdmin.setCreatedTime(now);
        storeAdmin.setUpdatedTime(now);
        storeAdmin.setVersion(1);
        storeAdmin.setIsDelete(0);
        adminAccountsService.save(storeAdmin);

        // 2. 创建门店
        Stores store = new Stores();
        store.setName(request.getName());
        store.setLogoImageId(request.getLogoImageId());
        store.setStoreAdminId(storeAdmin.getId());
        store.setContactPhone(request.getContactPhone());
        store.setAddress(request.getAddress());
        store.setLatitude(request.getLatitude());
        store.setLongitude(request.getLongitude());
        store.setDescription(request.getDescription());
        store.setBusinessLicense(request.getBusinessLicense());
        store.setAuditStatus(2);  // 超级管理员创建的门店自动审核通过
        store.setBusinessStatus(1);

        Stores created = storesService.createStore(store, user.getAccountId());
        saveLog(user, "CREATE", "创建门店：" + created.getName() + "，管理员：" + request.getAdminEmail(),
                "/admin/stores/create", "");
        return Result.success(toResponse(created));
    }

    @PostMapping("/{id}/update")
    public Result<AdminStoreResponse> update(@PathVariable String id, @Valid @RequestBody AdminStoreUpdateRequest request) {
        LoginUserInfo user = AuthUserContext.get();
        // 超管或门店管理员（仅能编辑自己的门店）
        boolean isStoreOwner = user.isStoreAdmin() && id.equals(user.getStoreId());
        if (!user.isSuperAdmin() && !isStoreOwner) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权编辑此门店");
        }
        Stores store = storesService.getById(id);
        if (store == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "门店不存在");
        }

        if (StringUtils.hasText(request.getName())) {
            store.setName(request.getName());
        }
        if (request.getLogoImageId() != null) {
            store.setLogoImageId(request.getLogoImageId());
        }
        if (StringUtils.hasText(request.getContactPhone())) {
            store.setContactPhone(request.getContactPhone());
        }
        if (StringUtils.hasText(request.getAddress())) {
            store.setAddress(request.getAddress());
        }
        if (request.getLatitude() != null) {
            store.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            store.setLongitude(request.getLongitude());
        }
        if (request.getDescription() != null) {
            store.setDescription(request.getDescription());
        }
        if (request.getBusinessLicense() != null) {
            store.setBusinessLicense(request.getBusinessLicense());
        }
        if (request.getStoreAdminId() != null) {
            store.setStoreAdminId(request.getStoreAdminId());
        }
        if (request.getIsOnline() != null) {
            store.setIsOnline(request.getIsOnline());
        }

        Stores updated = storesService.updateStore(store, user.getAccountId());
        saveLog(user, "UPDATE", "更新门店：" + updated.getName(), "/admin/stores/" + id + "/update", "");
        return Result.success(toResponse(updated));
    }

    // ==================== 门店审核 ====================

    @PostMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable String id, @Valid @RequestBody AdminStoreAuditRequest request) {
        LoginUserInfo user = AuthUserContext.get();
        if (!user.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅超级管理员可审核门店");
        }
        Stores store = storesService.getById(id);
        if (store == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "门店不存在");
        }
        storesService.auditStore(id, request.getAuditStatus(), request.getRemark(), user.getAccountId());
        String resultText = request.getAuditStatus() == 2 ? "通过" : "拒绝";
        saveLog(user, "AUDIT", "审核门店：" + store.getName() + " → " + resultText,
                "/admin/stores/" + id + "/audit",
                "{\"auditStatus\":" + request.getAuditStatus() + ",\"remark\":\"" + request.getRemark() + "\"}");
        return Result.success();
    }

    // ==================== 营业状态管理 ====================

    @PostMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable String id, @Valid @RequestBody AdminStoreStatusRequest request) {
        LoginUserInfo user = AuthUserContext.get();
        // 门店管理员只能操作自己的门店
        if (user.isStoreAdmin() && !id.equals(user.getStoreId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作其他门店");
        }
        Stores store = storesService.getById(id);
        if (store == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "门店不存在");
        }
        storesService.toggleBusinessStatus(id, request.getBusinessStatus(), user.getAccountId());
        String statusName = request.getBusinessStatus() == 1 ? "营业中" : request.getBusinessStatus() == 2 ? "休息中" : "已关闭";
        saveLog(user, "UPDATE", "切换门店营业状态：" + store.getName() + " → " + statusName,
                "/admin/stores/" + id + "/status",
                "{\"businessStatus\":" + request.getBusinessStatus() + "}");
        return Result.success();
    }

    // ==================== 营业时间管理 ====================

    @GetMapping("/{id}/business-hours")
    public Result<List<AdminStoreBusinessHourItem>> getBusinessHours(@PathVariable String id) {
        List<StoreBusinessHours> hours = storeBusinessHoursService.getByStoreId(id);
        List<AdminStoreBusinessHourItem> items = hours.stream().map(h -> {
            AdminStoreBusinessHourItem item = new AdminStoreBusinessHourItem();
            item.setDayOfWeek(h.getDayOfWeek());
            item.setStartTime(h.getStartTime() != null ? h.getStartTime().format(TIME_FMT) : null);
            item.setEndTime(h.getEndTime() != null ? h.getEndTime().format(TIME_FMT) : null);
            item.setIsAvailable(h.getIsAvailable());
            return item;
        }).collect(Collectors.toList());
        return Result.success(items);
    }

    @PostMapping("/{id}/business-hours")
    public Result<Void> saveBusinessHours(@PathVariable String id, @Valid @RequestBody AdminStoreBusinessHoursRequest request) {
        LoginUserInfo user = AuthUserContext.get();
        if (user.isStoreAdmin() && !id.equals(user.getStoreId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作其他门店");
        }
        Stores store = storesService.getById(id);
        if (store == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "门店不存在");
        }

        List<StoreBusinessHours> hoursList = new ArrayList<>();
        for (AdminStoreBusinessHourItem item : request.getHours()) {
            StoreBusinessHours hours = new StoreBusinessHours();
            hours.setStoreId(id);
            hours.setDayOfWeek(item.getDayOfWeek());
            hours.setStartTime(LocalTime.parse(item.getStartTime(), TIME_FMT));
            hours.setEndTime(LocalTime.parse(item.getEndTime(), TIME_FMT));
            hours.setIsAvailable(item.getIsAvailable() != null ? item.getIsAvailable() : 1);
            hoursList.add(hours);
        }
        storeBusinessHoursService.batchSave(id, hoursList);

        saveLog(user, "UPDATE", "更新门店营业时间：" + store.getName(),
                "/admin/stores/" + id + "/business-hours", "");
        return Result.success();
    }

    // ==================== Logo 上传 ====================

    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadLogo(@PathVariable String id, @RequestPart("file") MultipartFile file) {
        LoginUserInfo user = AuthUserContext.get();
        boolean isStoreOwner = user.isStoreAdmin() && id.equals(user.getStoreId());
        if (!user.isSuperAdmin() && !isStoreOwner) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此门店");
        }
        Stores store = storesService.getById(id);
        if (store == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "门店不存在");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择图片");
        }
        try {
            String ext = "";
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null) {
                int idx = originalFilename.lastIndexOf('.');
                if (idx >= 0 && idx < originalFilename.length() - 1) ext = originalFilename.substring(idx);
            }
            String objectName = "stores/" + id + "/logo" + ext;
            String url = ossUtil.upload(objectName, file.getInputStream());
            // 创建 Images 记录
            long now = System.currentTimeMillis();
            Images logoImage = new Images();
            logoImage.setId(SnowflakeIdUtil.nextImageId());
            logoImage.setOriginalName(file.getOriginalFilename());
            logoImage.setFileName(objectName);
            logoImage.setFilePath(objectName);
            logoImage.setFileUrl(url);
            logoImage.setFileSize(file.getSize());
            logoImage.setMimeType(file.getContentType());
            logoImage.setUploaderId(user.getAccountId());
            logoImage.setUploaderType(ADMIN_OPERATOR_TYPE);
            logoImage.setBusinessType("STORE_LOGO");
            logoImage.setBusinessId(id);
            logoImage.setCreatedTime(now);
            logoImage.setVersion(1);
            logoImage.setIsDelete(0);
            imagesService.save(logoImage);
            // 更新门店的 logo_image_id 为 Images 记录 ID
            Stores latest = storesService.getById(id);
            latest.setLogoImageId(logoImage.getId());
            latest.setUpdatedTime(now);
            storesService.updateById(latest);
            return Result.success(url);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传Logo失败: " + e.getMessage());
        }
    }

    // ==================== 逆地理编码 ====================

    @GetMapping("/reverse-geocode")
    public Result<java.util.Map<String, String>> reverseGeocode(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude) {
        if (!StringUtils.hasText(baiduAk)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "百度地图AK未配置");
        }
        String url = UriComponentsBuilder
                .fromHttpUrl("https://api.map.baidu.com/reverse_geocoding/v3/")
                .queryParam("ak", baiduAk)
                .queryParam("output", "json")
                .queryParam("coordtype", "gcj02ll")
                .queryParam("location", latitude + "," + longitude)
                .toUriString();
        String body;
        try {
            body = restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "逆地理编码请求失败");
        }
        try {
            java.util.Map<?, ?> root = objectMapper.readValue(body, java.util.Map.class);
            Object status = root.get("status");
            if (!(status instanceof Number) || ((Number) status).intValue() != 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "逆地理编码失败");
            }
            Object resultObj = root.get("result");
            String formattedAddress = null;
            if (resultObj instanceof java.util.Map) {
                java.util.Map<?, ?> result = (java.util.Map<?, ?>) resultObj;
                Object addr = result.get("formatted_address");
                if (addr != null) formattedAddress = addr.toString();
            }
            java.util.Map<String, String> ret = new java.util.HashMap<>();
            ret.put("address", formattedAddress != null ? formattedAddress : latitude + "," + longitude);
            return Result.success(ret);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "逆地理编码失败");
        }
    }

    // ==================== Helper ====================

    private AdminStoreResponse toResponse(Stores store) {
        AdminStoreResponse resp = new AdminStoreResponse();
        resp.setId(store.getId());
        resp.setName(store.getName());
        resp.setLogoImageId(store.getLogoImageId());
        resp.setStoreAdminId(store.getStoreAdminId());
        if (StringUtils.hasText(store.getStoreAdminId())) {
            AdminAccounts admin = adminAccountsService.getById(store.getStoreAdminId());
            if (admin != null) {
                resp.setStoreAdminName(admin.getUsername());
                resp.setStoreAdminPhone(admin.getPhone());
                resp.setStoreAdminEmail(admin.getEmail());
            }
        }
        resp.setContactPhone(store.getContactPhone());
        resp.setAddress(store.getAddress());
        resp.setLatitude(store.getLatitude());
        resp.setLongitude(store.getLongitude());
        resp.setBusinessStatus(store.getBusinessStatus());
        resp.setRating(store.getRating());
        resp.setRatingCount(store.getRatingCount());
        resp.setDescription(store.getDescription());
        resp.setBusinessLicense(store.getBusinessLicense());
        resp.setAuditStatus(store.getAuditStatus());
        resp.setAuditRemark(store.getAuditRemark());
        resp.setAuditTime(store.getAuditTime());
        resp.setIsOnline(store.getIsOnline());
        resp.setCreatedTime(store.getCreatedTime());
        resp.setUpdatedTime(store.getUpdatedTime());

        // 图片 URL
        if (StringUtils.hasText(store.getLogoImageId())) {
            Images img = imagesService.getById(store.getLogoImageId());
            if (img != null) {
                resp.setLogoImageUrl(img.getFileUrl());
            }
        }

        // 绑定师傅数量
        Long techCount = technicianAccountsService.count(
                new LambdaQueryWrapper<TechnicianAccounts>()
                        .eq(TechnicianAccounts::getStoreId, store.getId())
        );
        resp.setTechnicianCount(techCount != null ? techCount.intValue() : 0);

        // 营业时间
        List<StoreBusinessHours> hours = storeBusinessHoursService.getByStoreId(store.getId());
        resp.setBusinessHours(hours.stream().map(h -> {
            AdminStoreBusinessHourItem item = new AdminStoreBusinessHourItem();
            item.setDayOfWeek(h.getDayOfWeek());
            item.setStartTime(h.getStartTime() != null ? h.getStartTime().format(TIME_FMT) : null);
            item.setEndTime(h.getEndTime() != null ? h.getEndTime().format(TIME_FMT) : null);
            item.setIsAvailable(h.getIsAvailable());
            return item;
        }).collect(Collectors.toList()));

        return resp;
    }

    private void saveLog(LoginUserInfo user, String operationType, String operationDesc, String requestUrl, String requestParams) {
        long now = System.currentTimeMillis();
        OperationLogs log = new OperationLogs();
        log.setId(SnowflakeIdUtil.nextOperationLogId());
        log.setOperatorId(user.getAccountId());
        log.setOperatorType(ADMIN_OPERATOR_TYPE);
        log.setOperatorName(user.getAccountId());
        log.setOperationType(operationType);
        log.setOperationDesc(operationDesc);
        log.setModuleName("ADMIN_STORE");
        log.setRequestMethod("POST");
        log.setRequestUrl(requestUrl);
        log.setRequestParams(requestParams);
        log.setIpAddress("");
        log.setVersion(1);
        log.setIsDelete(0);
        log.setCreatedTime(now);
        operationLogsService.save(log);
    }
}
