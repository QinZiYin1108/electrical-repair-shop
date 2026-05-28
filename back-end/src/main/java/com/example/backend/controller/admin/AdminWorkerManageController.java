package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.Images;
import com.example.backend.entity.OperationLogs;
import com.example.backend.entity.RepairOrders;
import com.example.backend.entity.ServiceCategories;
import com.example.backend.entity.ServiceTypes;
import com.example.backend.entity.SystemMessages;
import com.example.backend.entity.Stores;
import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.entity.TechnicianProfiles;
import com.example.backend.entity.TechnicianServiceAreas;
import com.example.backend.entity.TechnicianSkills;
import com.example.backend.entity.TechnicianVisitFeePolicies;
import com.example.backend.entity.TechnicianWorkTimes;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.admin.AdminWorkerDetailResponse;
import com.example.backend.model.admin.AdminWorkerListItemResponse;
import com.example.backend.model.admin.AdminWorkerOrderStatsResponse;
import com.example.backend.model.admin.AdminWorkerServiceAreaCenterResponse;
import com.example.backend.model.admin.AdminWorkerSkillBatchUpdateRequest;
import com.example.backend.model.admin.AdminWorkerSkillDeleteRequest;
import com.example.backend.model.admin.AdminWorkerSkillItemResponse;
import com.example.backend.model.admin.AdminWorkerStatusUpdateRequest;
import com.example.backend.model.admin.AdminWorkerUpdateRequest;
import com.example.backend.model.admin.AdminWorkerVisitFeePoliciesUpdateRequest;
import com.example.backend.model.admin.AdminWorkerVisitFeePolicyResponse;
import com.example.backend.model.admin.AdminWorkerWorkTimeResponse;
import com.example.backend.model.admin.AdminWorkerWorkTimesUpdateRequest;
import com.example.backend.model.worker.WorkerSkillCategoryNode;
import com.example.backend.model.worker.WorkerSkillServiceTypeOption;
import com.example.backend.mapper.TechnicianSkillsMapper;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.ImagesService;
import com.example.backend.service.OperationLogsService;
import com.example.backend.service.RepairOrdersService;
import com.example.backend.service.ServiceCategoriesService;
import com.example.backend.service.ServiceTypesService;
import com.example.backend.service.StoresService;
import com.example.backend.service.SystemMessagesService;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.service.TechnicianProfilesService;
import com.example.backend.service.TechnicianServiceAreasService;
import com.example.backend.service.TechnicianSkillsService;
import com.example.backend.service.TechnicianVisitFeePoliciesService;
import com.example.backend.service.TechnicianWorkTimesService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import com.example.backend.utils.oss.OssUtil;
import com.example.backend.utils.upload.UploadLimitUtil;
import jakarta.validation.Valid;
import org.springframework.dao.DuplicateKeyException;
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
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/admin/workers")
public class AdminWorkerManageController {

    private static final int ADMIN_OPERATOR_TYPE = 3;
    private static final int SYSTEM_MESSAGE_FOR_WORKER = 2;
    private static final int WORKER_ACCOUNT_STATUS_NORMAL = 1;
    private static final int WORKER_ACCOUNT_STATUS_FROZEN = 3;
    private static final int SERVICE_MODE_ONSITE_REPAIR = 1;
    private static final int SERVICE_MODE_ONSITE_INSTALL = 2;
    private static final int SERVICE_MODE_OFFLINE_REPAIR = 3;
    private static final LocalTime DEFAULT_WORK_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_WORK_END_TIME = LocalTime.of(18, 0);
    private static final DateTimeFormatter WORK_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter WORK_TIME_SHORT_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final TechnicianAccountsService technicianAccountsService;
    private final TechnicianProfilesService technicianProfilesService;
    private final TechnicianServiceAreasService technicianServiceAreasService;
    private final TechnicianSkillsService technicianSkillsService;
    private final TechnicianSkillsMapper technicianSkillsMapper;
    private final ServiceTypesService serviceTypesService;
    private final ServiceCategoriesService serviceCategoriesService;
    private final TechnicianVisitFeePoliciesService technicianVisitFeePoliciesService;
    private final TechnicianWorkTimesService technicianWorkTimesService;
    private final RepairOrdersService repairOrdersService;
    private final ImagesService imagesService;
    private final OssUtil ossUtil;
    private final OperationLogsService operationLogsService;
    private final SystemMessagesService systemMessagesService;
    private final StoresService storesService;

    public AdminWorkerManageController(
        TechnicianAccountsService technicianAccountsService,
        TechnicianProfilesService technicianProfilesService,
        TechnicianServiceAreasService technicianServiceAreasService,
        TechnicianSkillsService technicianSkillsService,
        TechnicianSkillsMapper technicianSkillsMapper,
        ServiceTypesService serviceTypesService,
        ServiceCategoriesService serviceCategoriesService,
        TechnicianVisitFeePoliciesService technicianVisitFeePoliciesService,
        TechnicianWorkTimesService technicianWorkTimesService,
        RepairOrdersService repairOrdersService,
        ImagesService imagesService,
        OssUtil ossUtil,
        OperationLogsService operationLogsService,
        SystemMessagesService systemMessagesService,
        StoresService storesService
    ) {
        this.technicianAccountsService = technicianAccountsService;
        this.technicianProfilesService = technicianProfilesService;
        this.technicianServiceAreasService = technicianServiceAreasService;
        this.technicianSkillsService = technicianSkillsService;
        this.technicianSkillsMapper = technicianSkillsMapper;
        this.serviceTypesService = serviceTypesService;
        this.serviceCategoriesService = serviceCategoriesService;
        this.technicianVisitFeePoliciesService = technicianVisitFeePoliciesService;
        this.technicianWorkTimesService = technicianWorkTimesService;
        this.repairOrdersService = repairOrdersService;
        this.imagesService = imagesService;
        this.ossUtil = ossUtil;
        this.operationLogsService = operationLogsService;
        this.systemMessagesService = systemMessagesService;
        this.storesService = storesService;
    }

    @GetMapping
    public Result<Page<AdminWorkerListItemResponse>> listWorkers(
        @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
        @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
        @RequestParam(value = "keyword", required = false) String keyword
    ) {
        LoginUserInfo admin = requireAdmin();
        long currentPage = pageNum <= 0 ? 1 : pageNum;
        long currentSize = pageSize <= 0 ? 10 : pageSize;

        LambdaQueryWrapper<TechnicianAccounts> wrapper = new LambdaQueryWrapper<TechnicianAccounts>()
            .eq(TechnicianAccounts::getIsDelete, 0);

        // 门店管理员只能看到自己门店的师傅
        if (admin.isStoreAdmin() && admin.getStoreId() != null) {
            wrapper.eq(TechnicianAccounts::getStoreId, admin.getStoreId());
        }

        if (StringUtils.hasText(keyword)) {
            String trimmedKeyword = keyword.trim();
            wrapper.and(w -> w.like(TechnicianAccounts::getUsername, trimmedKeyword)
                .or().like(TechnicianAccounts::getPhone, trimmedKeyword)
                .or().like(TechnicianAccounts::getEmail, trimmedKeyword));
        }
        wrapper.orderByDesc(TechnicianAccounts::getCreatedTime);

        Page<TechnicianAccounts> accountPage = technicianAccountsService.page(new Page<>(currentPage, currentSize), wrapper);
        List<TechnicianAccounts> accountList = accountPage.getRecords();
        List<AdminWorkerListItemResponse> responseRecords = new ArrayList<>();
        if (!accountList.isEmpty()) {
            Set<String> accountIdSet = new HashSet<>();
            for (TechnicianAccounts account : accountList) {
                accountIdSet.add(account.getId());
            }

            Map<String, TechnicianProfiles> profileMap = new HashMap<>();
            List<TechnicianProfiles> profileList = technicianProfilesService.list(
                new LambdaQueryWrapper<TechnicianProfiles>()
                    .in(TechnicianProfiles::getTechnicianAccountId, accountIdSet)
                    .eq(TechnicianProfiles::getIsDelete, 0)
            );
            for (TechnicianProfiles profile : profileList) {
                profileMap.put(profile.getTechnicianAccountId(), profile);
            }

            Map<String, String> avatarUrlMap = queryAvatarUrlMap(accountIdSet);

            for (TechnicianAccounts account : accountList) {
                AdminWorkerListItemResponse item = new AdminWorkerListItemResponse();
                item.setId(account.getId());
                item.setUsername(account.getUsername());
                item.setPhone(account.getPhone());
                item.setEmail(account.getEmail());
                item.setAccountStatus(account.getAccountStatus());
                item.setWorkStatus(account.getWorkStatus());
                item.setRating(account.getRating());
                item.setCreatedTime(account.getCreatedTime());

                TechnicianProfiles profile = profileMap.get(account.getId());
                if (profile != null) {
                    item.setRealName(profile.getRealName());
                }
                item.setAvatarUrl(avatarUrlMap.get(account.getId()));
                responseRecords.add(item);
            }
        }

        Page<AdminWorkerListItemResponse> resultPage = new Page<>(accountPage.getCurrent(), accountPage.getSize(), accountPage.getTotal());
        resultPage.setRecords(responseRecords);
        return Result.success(resultPage);
    }

    @GetMapping("/{id}")
    public Result<AdminWorkerDetailResponse> getWorkerDetail(@PathVariable("id") String id) {
        LoginUserInfo admin = requireAdmin();
        TechnicianAccounts account = getAndCheckWorkerAccount(id);

        // 门店管理员只能查看自己门店的师傅
        if (admin.isStoreAdmin()) {
            if (!StringUtils.hasText(account.getStoreId()) || !account.getStoreId().equals(admin.getStoreId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看其他门店的师傅");
            }
        }
        TechnicianProfiles profile = queryWorkerProfile(id);
        TechnicianServiceAreas serviceArea = queryDefaultServiceArea(id);

        AdminWorkerDetailResponse response = new AdminWorkerDetailResponse();
        response.setId(account.getId());
        response.setUsername(account.getUsername());
        response.setPhone(account.getPhone());
        response.setEmail(account.getEmail());
        response.setAccountStatus(account.getAccountStatus());
        response.setWorkStatus(account.getWorkStatus());
        response.setRating(account.getRating());
        response.setCreatedTime(account.getCreatedTime());
        response.setOrderCount(account.getOrderCount());
        response.setCompletionRate(account.getCompletionRate());
        response.setAvatarUrl(querySingleAvatarUrl(id));

        if (serviceArea != null) {
            response.setAddress(serviceArea.getCenterAddress());
            AdminWorkerServiceAreaCenterResponse serviceAreaCenter = new AdminWorkerServiceAreaCenterResponse();
            serviceAreaCenter.setId(serviceArea.getId());
            serviceAreaCenter.setAreaName(serviceArea.getAreaName());
            serviceAreaCenter.setCenterAddress(serviceArea.getCenterAddress());
            serviceAreaCenter.setCenterLatitude(serviceArea.getCenterLatitude());
            serviceAreaCenter.setCenterLongitude(serviceArea.getCenterLongitude());
            serviceAreaCenter.setIsDefault(serviceArea.getIsDefault());
            serviceAreaCenter.setIsActive(serviceArea.getIsActive());
            serviceAreaCenter.setUpdatedTime(serviceArea.getUpdatedTime());
            response.setServiceAreaCenter(serviceAreaCenter);
        }

        if (profile != null) {
            response.setRealName(profile.getRealName());
            response.setIdCard(profile.getIdCard());
            response.setGender(profile.getGender());
            if (profile.getBirthday() != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                response.setBirthday(dateFormat.format(new Date(profile.getBirthday())));
            }
            response.setWorkYears(profile.getWorkYears());
            response.setEducation(profile.getEducation());
            response.setIntroduction(profile.getIntroduction());
            response.setResponseTime(profile.getResponseTime());
        }

        response.setVisitFeePolicies(buildVisitFeePolicyResponseList(id));
        response.setWorkTimes(buildWorkerWorkTimeResponseList(id));
        response.setOrderStats(buildOrderStats(id));
        return Result.success(response);
    }

    @GetMapping("/{id}/skills")
    public Result<List<AdminWorkerSkillItemResponse>> listWorkerSkills(@PathVariable("id") String id) {
        requireAdmin();
        getAndCheckWorkerAccount(id);
        return Result.success(buildWorkerSkillResponseList(id));
    }

    @GetMapping("/{id}/skills/available/categories")
    public Result<List<WorkerSkillCategoryNode>> listAvailableSkillCategories(
        @PathVariable("id") String id,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "serviceMode", required = false) Integer serviceMode
    ) {
        requireAdmin();
        getAndCheckWorkerAccount(id);

        Integer mode = normalizeServiceMode(serviceMode);
        String normalizedKeyword = trimToNull(keyword);

        List<ServiceCategories> activeCategories = listActiveCategories();
        if (activeCategories.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        Map<String, ServiceCategories> categoryMap = new LinkedHashMap<>();
        for (ServiceCategories category : activeCategories) {
            if (category == null || !StringUtils.hasText(category.getId())) {
                continue;
            }
            categoryMap.put(category.getId(), category);
        }

        List<ServiceTypes> availableServiceTypes = listAvailableServiceTypeEntities(id, normalizedKeyword, mode);
        if (availableServiceTypes.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        Set<String> relevantCategoryIds = new LinkedHashSet<>();
        for (ServiceTypes serviceType : availableServiceTypes) {
            if (serviceType == null || !StringUtils.hasText(serviceType.getCategoryId())) {
                continue;
            }
            String currentId = serviceType.getCategoryId();
            while (StringUtils.hasText(currentId)) {
                if (!relevantCategoryIds.add(currentId)) {
                    break;
                }
                ServiceCategories currentCategory = categoryMap.get(currentId);
                if (currentCategory == null || !StringUtils.hasText(currentCategory.getParentId())) {
                    break;
                }
                currentId = currentCategory.getParentId();
            }
        }

        if (relevantCategoryIds.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        Map<String, List<ServiceCategories>> childrenByParentId = new LinkedHashMap<>();
        for (ServiceCategories category : activeCategories) {
            if (category == null || !StringUtils.hasText(category.getId())) {
                continue;
            }
            if (!relevantCategoryIds.contains(category.getId())) {
                continue;
            }
            String parentId = trimToNull(category.getParentId());
            if (parentId == null) {
                parentId = "ROOT";
            }
            childrenByParentId.computeIfAbsent(parentId, key -> new ArrayList<>()).add(category);
        }

        List<ServiceCategories> level1Categories = childrenByParentId.getOrDefault("ROOT", Collections.emptyList());
        List<WorkerSkillCategoryNode> tree = new ArrayList<>();
        for (ServiceCategories level1Category : level1Categories) {
            tree.add(buildCategoryNode(level1Category, childrenByParentId));
        }
        return Result.success(tree);
    }

    @GetMapping("/{id}/skills/available/service-types")
    public Result<List<WorkerSkillServiceTypeOption>> listAvailableSkillServiceTypes(
        @PathVariable("id") String id,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "serviceMode", required = false) Integer serviceMode,
        @RequestParam(value = "categoryId", required = false) String categoryId
    ) {
        requireAdmin();
        getAndCheckWorkerAccount(id);

        Integer mode = normalizeServiceMode(serviceMode);
        String normalizedKeyword = trimToNull(keyword);
        String normalizedCategoryId = trimToNull(categoryId);

        List<ServiceTypes> availableServiceTypes = listAvailableServiceTypeEntities(id, normalizedKeyword, mode);
        if (availableServiceTypes.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        List<ServiceCategories> activeCategories = listActiveCategories();
        Map<String, ServiceCategories> categoryMap = new LinkedHashMap<>();
        for (ServiceCategories category : activeCategories) {
            if (category == null || !StringUtils.hasText(category.getId())) {
                continue;
            }
            categoryMap.put(category.getId(), category);
        }

        Set<String> filterCategoryIds = null;
        if (StringUtils.hasText(normalizedCategoryId)) {
            if (!categoryMap.containsKey(normalizedCategoryId)) {
                return Result.success(new ArrayList<>());
            }
            filterCategoryIds = findDescendantCategoryIds(normalizedCategoryId, activeCategories);
        }

        List<WorkerSkillServiceTypeOption> result = new ArrayList<>();
        for (ServiceTypes serviceType : availableServiceTypes) {
            if (serviceType == null || !StringUtils.hasText(serviceType.getId())) {
                continue;
            }
            if (filterCategoryIds != null) {
                String currentCategoryId = trimToNull(serviceType.getCategoryId());
                if (!StringUtils.hasText(currentCategoryId) || !filterCategoryIds.contains(currentCategoryId)) {
                    continue;
                }
            }

            WorkerSkillServiceTypeOption item = new WorkerSkillServiceTypeOption();
            item.setId(serviceType.getId());
            item.setName(serviceType.getName());
            item.setType(serviceType.getType());
            item.setTypeText(resolveServiceModeText(serviceType.getType()));
            item.setDescription(serviceType.getDescription());
            item.setCategoryId(serviceType.getCategoryId());

            ServiceCategories category = categoryMap.get(serviceType.getCategoryId());
            item.setCategoryName(category == null ? null : category.getName());
            item.setCategoryPath(buildCategoryPath(serviceType.getCategoryId(), categoryMap));
            result.add(item);
        }
        return Result.success(result);
    }

    @PostMapping("/{id}/skills")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchAddWorkerSkills(
        @PathVariable("id") String id,
        @Valid @RequestBody AdminWorkerSkillBatchUpdateRequest request
    ) {
        LoginUserInfo admin = requireAdmin();
        getAndCheckWorkerAccount(id);
        if (request == null || request.getServiceTypeIds() == null || request.getServiceTypeIds().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择要添加的技能");
        }

        LinkedHashSet<String> uniqueServiceTypeIds = new LinkedHashSet<>();
        for (String serviceTypeId : request.getServiceTypeIds()) {
            uniqueServiceTypeIds.add(normalizeServiceTypeId(serviceTypeId));
        }

        for (String serviceTypeId : uniqueServiceTypeIds) {
            addSkillForWorker(id, serviceTypeId);
        }

        long now = System.currentTimeMillis();
        saveOperationLog(
            admin.getAccountId(),
            "UPDATE",
            "管理员添加师傅技能",
            "/admin/workers/" + id + "/skills",
            "{\"count\":" + uniqueServiceTypeIds.size() + "}"
        );
        saveSystemMessage(
            id,
            "师傅技能已更新",
            "您的技能信息已由管理员更新，请前往技能页面查看。",
            "ADMIN_WORKER_SKILL_ADD",
            2,
            now
        );
        return Result.success();
    }

    @PostMapping("/{id}/skills/remove")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> removeWorkerSkill(
        @PathVariable("id") String id,
        @Valid @RequestBody AdminWorkerSkillDeleteRequest request
    ) {
        LoginUserInfo admin = requireAdmin();
        getAndCheckWorkerAccount(id);
        String serviceTypeId = normalizeServiceTypeId(request == null ? null : request.getServiceTypeId());
        technicianSkillsMapper.logicalDeleteByTechnicianAndServiceType(id, serviceTypeId, System.currentTimeMillis());

        long now = System.currentTimeMillis();
        saveOperationLog(
            admin.getAccountId(),
            "UPDATE",
            "管理员删除师傅技能",
            "/admin/workers/" + id + "/skills/remove",
            "{\"serviceTypeId\":\"" + serviceTypeId + "\"}"
        );
        saveSystemMessage(
            id,
            "师傅技能已更新",
            "您的技能信息已由管理员更新，请前往技能页面查看。",
            "ADMIN_WORKER_SKILL_REMOVE",
            2,
            now
        );
        return Result.success();
    }

    @PostMapping("/{id}/update")
    public Result<Void> updateWorker(
        @PathVariable("id") String id,
        @Valid @RequestBody AdminWorkerUpdateRequest request
    ) {
        LoginUserInfo admin = requireAdmin();
        TechnicianAccounts account = getAndCheckWorkerAccount(id);
        long now = System.currentTimeMillis();
        String oldUsername = account.getUsername();

        account.setUsername(request.getUsername());
        account.setEmail(request.getEmail());
        account.setUpdatedTime(now);
        technicianAccountsService.updateById(account);

        TechnicianProfiles profile = queryWorkerProfile(id);
        boolean isNewProfile = profile == null;
        if (isNewProfile) {
            profile = new TechnicianProfiles();
            profile.setId(SnowflakeIdUtil.nextTechnicianProfileId());
            profile.setTechnicianAccountId(id);
            profile.setCreatedTime(now);
            profile.setIsDelete(0);
        }
        profile.setWorkYears(request.getWorkYears());
        profile.setEducation(request.getEducation());
        profile.setIntroduction(request.getIntroduction());
        profile.setUpdatedTime(now);
        if (isNewProfile) {
            technicianProfilesService.save(profile);
        } else {
            technicianProfilesService.updateById(profile);
        }

        saveOperationLog(
            admin.getAccountId(),
            "UPDATE",
            "管理员修改师傅基础信息",
            "/admin/workers/" + id + "/update",
            "{\"oldUsername\":\"" + oldUsername + "\",\"newUsername\":\"" + request.getUsername() + "\"}"
        );
        saveSystemMessage(
            id,
            "师傅账号信息已被管理员更新",
            "您的师傅账号信息已由管理员更新，如有疑问请联系平台客服。",
            "ADMIN_WORKER_UPDATE",
            2,
            now
        );
        return Result.success();
    }

    @PostMapping("/{id}/status")
    public Result<Void> updateWorkerStatus(
        @PathVariable("id") String id,
        @Valid @RequestBody AdminWorkerStatusUpdateRequest request
    ) {
        LoginUserInfo admin = requireAdmin();
        TechnicianAccounts account = getAndCheckWorkerAccount(id);
        Integer targetStatus = request.getAccountStatus();
        if (targetStatus == null || (targetStatus != WORKER_ACCOUNT_STATUS_NORMAL && targetStatus != WORKER_ACCOUNT_STATUS_FROZEN)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "状态仅支持正常或冻结");
        }
        Integer currentStatus = account.getAccountStatus();
        if (currentStatus == null || (currentStatus != WORKER_ACCOUNT_STATUS_NORMAL && currentStatus != WORKER_ACCOUNT_STATUS_FROZEN)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前状态不支持该操作");
        }
        if (currentStatus.equals(targetStatus)) {
            return Result.success();
        }

        long now = System.currentTimeMillis();
        account.setAccountStatus(targetStatus);
        account.setUpdatedTime(now);
        technicianAccountsService.updateById(account);

        saveOperationLog(
            admin.getAccountId(),
            "UPDATE",
            "管理员修改师傅账号状态",
            "/admin/workers/" + id + "/status",
            "{\"oldStatus\":" + currentStatus + ",\"newStatus\":" + targetStatus + "}"
        );
        String statusText = targetStatus == WORKER_ACCOUNT_STATUS_NORMAL ? "正常" : "冻结";
        saveSystemMessage(
            id,
            "师傅账号状态已被管理员调整",
            "您的师傅账号状态已被调整为：" + statusText + "，如有疑问请联系平台客服。",
            "ADMIN_WORKER_STATUS",
            1,
            now
        );
        return Result.success();
    }

    @PostMapping("/{id}/bind-store")
    public Result<Void> bindWorkerToStore(
        @PathVariable("id") String id,
        @RequestBody Map<String, String> body
    ) {
        LoginUserInfo admin = requireAdmin();
        // 仅超级管理员可绑定师傅到门店
        if (admin.getAdminRole() != null && admin.getAdminRole() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅超级管理员可绑定师傅到门店");
        }
        TechnicianAccounts account = getAndCheckWorkerAccount(id);
        String storeId = body.get("storeId");
        if (!StringUtils.hasText(storeId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "门店ID不能为空");
        }
        Stores store = storesService.getById(storeId);
        if (store == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "门店不存在");
        }
        technicianAccountsService.bindStore(id, storeId);

        saveOperationLog(
            admin.getAccountId(),
            "UPDATE",
            "绑定师傅到门店：" + store.getName(),
            "/admin/workers/" + id + "/bind-store",
            "{\"storeId\":\"" + storeId + "\"}"
        );
        return Result.success();
    }

    @PostMapping("/{id}/unbind-store")
    public Result<Void> unbindWorkerFromStore(@PathVariable("id") String id) {
        LoginUserInfo admin = requireAdmin();
        if (admin.getAdminRole() != null && admin.getAdminRole() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅超级管理员可解绑师傅");
        }
        TechnicianAccounts account = getAndCheckWorkerAccount(id);
        if (!StringUtils.hasText(account.getStoreId())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该师傅未绑定门店");
        }
        technicianAccountsService.unbindStore(id);

        saveOperationLog(
            admin.getAccountId(),
            "UPDATE",
            "解绑师傅门店",
            "/admin/workers/" + id + "/unbind-store",
            "{}"
        );
        return Result.success();
    }

    @PostMapping("/{id}/visit-fee-policies")
    public Result<Void> updateVisitFeePolicies(
        @PathVariable("id") String id,
        @Valid @RequestBody AdminWorkerVisitFeePoliciesUpdateRequest request
    ) {
        LoginUserInfo admin = requireAdmin();
        getAndCheckWorkerAccount(id);
        if (request == null || request.getPolicies() == null || request.getPolicies().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "计费策略不能为空");
        }

        Set<Integer> serviceKindSet = new HashSet<>();
        for (AdminWorkerVisitFeePoliciesUpdateRequest.PolicyItem policyItem : request.getPolicies()) {
            validatePolicyItem(policyItem);
            if (!serviceKindSet.add(policyItem.getServiceKind())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "同一服务类型不能重复提交");
            }
        }

        long now = System.currentTimeMillis();
        for (AdminWorkerVisitFeePoliciesUpdateRequest.PolicyItem policyItem : request.getPolicies()) {
            TechnicianVisitFeePolicies policy = findPolicyForUpdate(id, policyItem);
            boolean isNew = policy == null;
            if (isNew) {
                policy = new TechnicianVisitFeePolicies();
                policy.setId(SnowflakeIdUtil.nextTechnicianVisitFeePolicyId());
                policy.setTechnicianAccountId(id);
                policy.setServiceKind(policyItem.getServiceKind());
                policy.setCreatedTime(now);
                policy.setEffectiveTime(now);
                policy.setIsDelete(0);
            }

            policy.setMinVisitFee(policyItem.getMinVisitFee());
            policy.setBaseRadiusKm(policyItem.getBaseRadiusKm());
            policy.setExtraFeePerKm(policyItem.getExtraFeePerKm());
            policy.setDistanceCalcType(policyItem.getDistanceCalcType());
            policy.setRoundingRule(policyItem.getRoundingRule());
            policy.setMaxVisitFee(policyItem.getMaxVisitFee());
            policy.setIsActive(policyItem.getIsActive() != null && policyItem.getIsActive() == 0 ? 0 : 1);
            policy.setUpdatedTime(now);

            if (isNew) {
                technicianVisitFeePoliciesService.save(policy);
            } else {
                technicianVisitFeePoliciesService.updateById(policy);
            }
        }

        saveOperationLog(
            admin.getAccountId(),
            "UPDATE",
            "管理员修改师傅上门计费策略",
            "/admin/workers/" + id + "/visit-fee-policies",
            "{\"count\":" + request.getPolicies().size() + "}"
        );
        saveSystemMessage(
            id,
            "上门计费策略已更新",
            "您的上门计费策略已由管理员更新，如有疑问请联系平台客服。",
            "ADMIN_WORKER_FEE_POLICY",
            2,
            now
        );
        return Result.success();
    }

    @PostMapping("/{id}/work-times")
    public Result<Void> updateWorkerWorkTimes(
        @PathVariable("id") String id,
        @Valid @RequestBody AdminWorkerWorkTimesUpdateRequest request
    ) {
        LoginUserInfo admin = requireAdmin();
        getAndCheckWorkerAccount(id);
        if (request == null || request.getWorkTimes() == null || request.getWorkTimes().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "工作时间不能为空");
        }

        Set<Integer> daySet = new HashSet<>();
        for (AdminWorkerWorkTimesUpdateRequest.WorkTimeItem item : request.getWorkTimes()) {
            validateWorkTimeItem(item);
            if (!daySet.add(item.getDayOfWeek())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "同一天的工作时间不能重复提交");
            }
        }

        long now = System.currentTimeMillis();
        for (AdminWorkerWorkTimesUpdateRequest.WorkTimeItem item : request.getWorkTimes()) {
            LocalTime startTime = parseWorkTime(item.getStartTime(), "startTime");
            LocalTime endTime = parseWorkTime(item.getEndTime(), "endTime");
            if (!startTime.isBefore(endTime)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "开始时间必须早于结束时间");
            }

            TechnicianWorkTimes workTime = findWorkTimeForUpdate(id, item);
            boolean isNew = workTime == null;
            if (isNew) {
                workTime = new TechnicianWorkTimes();
                workTime.setId(SnowflakeIdUtil.nextTechnicianWorkTimeId());
                workTime.setTechnicianAccountId(id);
                workTime.setCreatedTime(now);
            }

            workTime.setDayOfWeek(item.getDayOfWeek());
            workTime.setStartTime(Time.valueOf(startTime));
            workTime.setEndTime(Time.valueOf(endTime));
            workTime.setIsAvailable(item.getIsAvailable() != null && item.getIsAvailable() == 0 ? 0 : 1);
            workTime.setUpdatedTime(now);
            workTime.setIsDelete(0);

            if (isNew) {
                technicianWorkTimesService.save(workTime);
            } else {
                technicianWorkTimesService.updateById(workTime);
            }
        }

        saveOperationLog(
            admin.getAccountId(),
            "UPDATE",
            "管理员修改师傅工作时间",
            "/admin/workers/" + id + "/work-times",
            "{\"count\":" + request.getWorkTimes().size() + "}"
        );
        saveSystemMessage(
            id,
            "工作时间已更新",
            "您的可接单工作时间已由管理员更新，如有疑问请联系平台客服。",
            "ADMIN_WORKER_WORK_TIME",
            2,
            now
        );
        return Result.success();
    }

    @PostMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadWorkerAvatar(
        @PathVariable("id") String id,
        @RequestPart("file") MultipartFile file
    ) {
        LoginUserInfo admin = requireAdmin();
        getAndCheckWorkerAccount(id);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }
        UploadLimitUtil.validateImageSize(file);

        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null) {
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < originalFilename.length() - 1) {
                ext = originalFilename.substring(dotIndex);
            }
        }
        String objectName = "avatars/" + id + "/" + UUID.randomUUID() + ext;
        String uploadUrl;
        try (InputStream inputStream = file.getInputStream()) {
            uploadUrl = ossUtil.upload(objectName, inputStream);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取上传文件失败");
        }

        long now = System.currentTimeMillis();
        Images image = new Images();
        image.setId(SnowflakeIdUtil.nextImageId());
        image.setOriginalName(originalFilename);
        image.setFileName(objectName);
        image.setFilePath(objectName);
        image.setFileUrl(uploadUrl);
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
        image.setUploaderId(admin.getAccountId());
        image.setUploaderType(ADMIN_OPERATOR_TYPE);
        image.setBusinessType("AVATAR");
        image.setBusinessId(id);
        image.setCreatedTime(now);
        image.setIsDelete(0);
        imagesService.save(image);

        saveOperationLog(
            admin.getAccountId(),
            "UPDATE",
            "管理员修改师傅头像",
            "/admin/workers/" + id + "/avatar",
            "{\"filename\":\"" + originalFilename + "\"}"
        );
        saveSystemMessage(
            id,
            "师傅头像已被管理员修改",
            "您的头像已由管理员修改，如有疑问请联系平台客服。",
            "ADMIN_WORKER_AVATAR",
            2,
            now
        );
        return Result.success(uploadUrl);
    }

    private LoginUserInfo requireAdmin() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问管理员接口");
        }
        return user;
    }

    private TechnicianAccounts getAndCheckWorkerAccount(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "师傅ID不能为空");
        }
        TechnicianAccounts account = technicianAccountsService.getById(id);
        if (account == null || (account.getIsDelete() != null && account.getIsDelete() != 0)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "师傅账号不存在");
        }
        return account;
    }

    private TechnicianProfiles queryWorkerProfile(String accountId) {
        return technicianProfilesService.getOne(
            new LambdaQueryWrapper<TechnicianProfiles>()
                .eq(TechnicianProfiles::getTechnicianAccountId, accountId)
                .eq(TechnicianProfiles::getIsDelete, 0),
            false
        );
    }

    private TechnicianServiceAreas queryDefaultServiceArea(String accountId) {
        return technicianServiceAreasService.getOne(
            new LambdaQueryWrapper<TechnicianServiceAreas>()
                .eq(TechnicianServiceAreas::getTechnicianAccountId, accountId)
                .eq(TechnicianServiceAreas::getIsDelete, 0)
                .orderByDesc(TechnicianServiceAreas::getIsDefault)
                .orderByDesc(TechnicianServiceAreas::getCreatedTime)
                .last("limit 1"),
            false
        );
    }

    private Map<String, String> queryAvatarUrlMap(Set<String> accountIdSet) {
        Map<String, String> avatarUrlMap = new HashMap<>();
        if (accountIdSet == null || accountIdSet.isEmpty()) {
            return avatarUrlMap;
        }
        List<Images> imageList = imagesService.list(
            new LambdaQueryWrapper<Images>()
                .eq(Images::getBusinessType, "AVATAR")
                .in(Images::getBusinessId, accountIdSet)
                .eq(Images::getIsDelete, 0)
                .orderByDesc(Images::getCreatedTime)
        );
        for (Images image : imageList) {
            String businessId = image.getBusinessId();
            if (!avatarUrlMap.containsKey(businessId) && StringUtils.hasText(image.getFileUrl())) {
                avatarUrlMap.put(businessId, image.getFileUrl());
            }
        }
        return avatarUrlMap;
    }

    private String querySingleAvatarUrl(String accountId) {
        Images avatarImage = imagesService.getOne(
            new LambdaQueryWrapper<Images>()
                .eq(Images::getBusinessType, "AVATAR")
                .eq(Images::getBusinessId, accountId)
                .eq(Images::getIsDelete, 0)
                .orderByDesc(Images::getCreatedTime)
                .last("limit 1"),
            false
        );
        if (avatarImage == null || !StringUtils.hasText(avatarImage.getFileUrl())) {
            return null;
        }
        return avatarImage.getFileUrl();
    }

    private List<AdminWorkerVisitFeePolicyResponse> buildVisitFeePolicyResponseList(String accountId) {
        List<TechnicianVisitFeePolicies> allPolicyList = technicianVisitFeePoliciesService.list(
            new LambdaQueryWrapper<TechnicianVisitFeePolicies>()
                .eq(TechnicianVisitFeePolicies::getTechnicianAccountId, accountId)
                .eq(TechnicianVisitFeePolicies::getIsDelete, 0)
                .orderByAsc(TechnicianVisitFeePolicies::getServiceKind)
                .orderByDesc(TechnicianVisitFeePolicies::getEffectiveTime)
                .orderByDesc(TechnicianVisitFeePolicies::getCreatedTime)
        );

        Map<Integer, TechnicianVisitFeePolicies> latestPolicyMap = new LinkedHashMap<>();
        for (TechnicianVisitFeePolicies policy : allPolicyList) {
            Integer serviceKind = policy.getServiceKind();
            if (serviceKind == null || (serviceKind != 1 && serviceKind != 2)) {
                continue;
            }
            if (!latestPolicyMap.containsKey(serviceKind)) {
                latestPolicyMap.put(serviceKind, policy);
            }
        }

        List<AdminWorkerVisitFeePolicyResponse> responseList = new ArrayList<>();
        responseList.add(toVisitFeePolicyResponse(latestPolicyMap.get(1), 1));
        responseList.add(toVisitFeePolicyResponse(latestPolicyMap.get(2), 2));
        return responseList;
    }

    private AdminWorkerVisitFeePolicyResponse toVisitFeePolicyResponse(TechnicianVisitFeePolicies policy, Integer defaultServiceKind) {
        AdminWorkerVisitFeePolicyResponse response = new AdminWorkerVisitFeePolicyResponse();
        if (policy == null) {
            response.setServiceKind(defaultServiceKind);
            response.setMinVisitFee(BigDecimal.ZERO);
            response.setBaseRadiusKm(BigDecimal.ZERO);
            response.setExtraFeePerKm(BigDecimal.ZERO);
            response.setDistanceCalcType(1);
            response.setRoundingRule(1);
            response.setIsActive(1);
            return response;
        }
        response.setId(policy.getId());
        response.setServiceKind(policy.getServiceKind());
        response.setMinVisitFee(policy.getMinVisitFee() == null ? BigDecimal.ZERO : policy.getMinVisitFee());
        response.setBaseRadiusKm(policy.getBaseRadiusKm() == null ? BigDecimal.ZERO : policy.getBaseRadiusKm());
        response.setExtraFeePerKm(policy.getExtraFeePerKm() == null ? BigDecimal.ZERO : policy.getExtraFeePerKm());
        response.setDistanceCalcType(policy.getDistanceCalcType());
        response.setRoundingRule(policy.getRoundingRule());
        response.setMaxVisitFee(policy.getMaxVisitFee());
        response.setIsActive(policy.getIsActive());
        response.setEffectiveTime(policy.getEffectiveTime());
        response.setUpdatedTime(policy.getUpdatedTime());
        return response;
    }

    private List<AdminWorkerWorkTimeResponse> buildWorkerWorkTimeResponseList(String accountId) {
        List<TechnicianWorkTimes> workTimesList = technicianWorkTimesService.list(
            new LambdaQueryWrapper<TechnicianWorkTimes>()
                .eq(TechnicianWorkTimes::getTechnicianAccountId, accountId)
                .eq(TechnicianWorkTimes::getIsDelete, 0)
                .orderByAsc(TechnicianWorkTimes::getDayOfWeek)
                .orderByDesc(TechnicianWorkTimes::getUpdatedTime)
                .orderByDesc(TechnicianWorkTimes::getCreatedTime)
        );
        Map<Integer, TechnicianWorkTimes> latestWorkTimeMap = new LinkedHashMap<>();
        for (TechnicianWorkTimes item : workTimesList) {
            if (item == null || item.getDayOfWeek() == null) {
                continue;
            }
            Integer dayOfWeek = item.getDayOfWeek();
            if (dayOfWeek < 1 || dayOfWeek > 7) {
                continue;
            }
            if (!latestWorkTimeMap.containsKey(dayOfWeek)) {
                latestWorkTimeMap.put(dayOfWeek, item);
            }
        }

        List<AdminWorkerWorkTimeResponse> responseList = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            responseList.add(toWorkTimeResponse(latestWorkTimeMap.get(day), day));
        }
        return responseList;
    }

    private AdminWorkerWorkTimeResponse toWorkTimeResponse(TechnicianWorkTimes entity, Integer defaultDayOfWeek) {
        AdminWorkerWorkTimeResponse response = new AdminWorkerWorkTimeResponse();
        if (entity == null) {
            response.setDayOfWeek(defaultDayOfWeek);
            response.setStartTime(WORK_TIME_FORMATTER.format(DEFAULT_WORK_START_TIME));
            response.setEndTime(WORK_TIME_FORMATTER.format(DEFAULT_WORK_END_TIME));
            response.setIsAvailable(1);
            return response;
        }
        response.setId(entity.getId());
        response.setDayOfWeek(entity.getDayOfWeek() == null ? defaultDayOfWeek : entity.getDayOfWeek());
        response.setStartTime(formatWorkTime(entity.getStartTime(), DEFAULT_WORK_START_TIME));
        response.setEndTime(formatWorkTime(entity.getEndTime(), DEFAULT_WORK_END_TIME));
        response.setIsAvailable(entity.getIsAvailable() != null && entity.getIsAvailable() == 0 ? 0 : 1);
        response.setUpdatedTime(entity.getUpdatedTime());
        return response;
    }

    private String formatWorkTime(Date dateValue, LocalTime defaultValue) {
        LocalTime localTime = toLocalTime(dateValue, defaultValue);
        return WORK_TIME_FORMATTER.format(localTime.withNano(0));
    }

    private LocalTime toLocalTime(Date dateValue, LocalTime defaultValue) {
        if (dateValue == null) {
            return defaultValue;
        }
        if (dateValue instanceof Time) {
            return ((Time) dateValue).toLocalTime().withNano(0);
        }
        return Instant.ofEpochMilli(dateValue.getTime())
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .withNano(0);
    }

    private LocalTime parseWorkTime(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + " 不能为空");
        }
        String normalized = value.trim();
        try {
            if (normalized.length() == 5) {
                return LocalTime.parse(normalized, WORK_TIME_SHORT_FORMATTER);
            }
            return LocalTime.parse(normalized, WORK_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + " 格式错误，应为 HH:mm 或 HH:mm:ss");
        }
    }

    private void validateWorkTimeItem(AdminWorkerWorkTimesUpdateRequest.WorkTimeItem item) {
        if (item == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "工作时间不能为空");
        }
        Integer dayOfWeek = item.getDayOfWeek();
        if (dayOfWeek == null || dayOfWeek < 1 || dayOfWeek > 7) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "dayOfWeek 仅支持 1-7");
        }
        if (!StringUtils.hasText(item.getStartTime()) || !StringUtils.hasText(item.getEndTime())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "开始时间和结束时间不能为空");
        }
        if (item.getIsAvailable() != null && item.getIsAvailable() != 0 && item.getIsAvailable() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "isAvailable 仅支持 0 或 1");
        }
    }

    private TechnicianWorkTimes findWorkTimeForUpdate(String accountId, AdminWorkerWorkTimesUpdateRequest.WorkTimeItem item) {
        if (StringUtils.hasText(item.getId())) {
            TechnicianWorkTimes byId = technicianWorkTimesService.getById(item.getId());
            if (byId != null && accountId.equals(byId.getTechnicianAccountId())) {
                return byId;
            }
        }
        return technicianWorkTimesService.getOne(
            new LambdaQueryWrapper<TechnicianWorkTimes>()
                .eq(TechnicianWorkTimes::getTechnicianAccountId, accountId)
                .eq(TechnicianWorkTimes::getDayOfWeek, item.getDayOfWeek())
                .orderByDesc(TechnicianWorkTimes::getUpdatedTime)
                .orderByDesc(TechnicianWorkTimes::getCreatedTime)
                .last("limit 1"),
            false
        );
    }

    private AdminWorkerOrderStatsResponse buildOrderStats(String accountId) {
        AdminWorkerOrderStatsResponse stats = new AdminWorkerOrderStatsResponse();
        stats.setTotalCount(countOrdersByStatuses(accountId));
        stats.setWaitingCount(countOrdersByStatuses(accountId, 1));
        stats.setOngoingCount(countOrdersByStatuses(accountId, 2, 3, 4));
        stats.setWaitingPayCount(countOrdersByStatuses(accountId, 5));
        stats.setCompletedCount(countOrdersByStatuses(accountId, 6));
        stats.setCanceledCount(countOrdersByStatuses(accountId, 7));
        stats.setRefundedCount(countOrdersByStatuses(accountId, 8));

        RepairOrders latestOrder = repairOrdersService.getOne(
            new LambdaQueryWrapper<RepairOrders>()
                .eq(RepairOrders::getTechnicianAccountId, accountId)
                .eq(RepairOrders::getIsDelete, 0)
                .orderByDesc(RepairOrders::getCreatedTime)
                .last("limit 1"),
            false
        );
        if (latestOrder != null) {
            stats.setLatestOrderTime(latestOrder.getCreatedTime());
        }
        return stats;
    }

    private long countOrdersByStatuses(String accountId, Integer... statusArray) {
        LambdaQueryWrapper<RepairOrders> wrapper = new LambdaQueryWrapper<RepairOrders>()
            .eq(RepairOrders::getTechnicianAccountId, accountId)
            .eq(RepairOrders::getIsDelete, 0);
        if (statusArray != null && statusArray.length > 0) {
            if (statusArray.length == 1) {
                wrapper.eq(RepairOrders::getStatus, statusArray[0]);
            } else {
                wrapper.in(RepairOrders::getStatus, (Object[]) statusArray);
            }
        }
        return repairOrdersService.count(wrapper);
    }

    private void validatePolicyItem(AdminWorkerVisitFeePoliciesUpdateRequest.PolicyItem policyItem) {
        if (policyItem == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "计费策略不能为空");
        }
        Integer serviceKind = policyItem.getServiceKind();
        if (serviceKind == null || (serviceKind != 1 && serviceKind != 2)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "服务类型仅支持上门维修和上门安装");
        }
        if (policyItem.getMinVisitFee().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "最低上门费不能小于0");
        }
        if (policyItem.getBaseRadiusKm().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "基础服务半径不能小于0");
        }
        if (policyItem.getExtraFeePerKm().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "超区每公里费用不能小于0");
        }
        if (policyItem.getDistanceCalcType() == null
            || (policyItem.getDistanceCalcType() != 1 && policyItem.getDistanceCalcType() != 2)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "距离计算方式不合法");
        }
        if (policyItem.getRoundingRule() == null
            || (policyItem.getRoundingRule() != 1 && policyItem.getRoundingRule() != 2)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "公里取整规则不合法");
        }
        if (policyItem.getMaxVisitFee() != null && policyItem.getMaxVisitFee().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "封顶公里数不能小于0");
        }
        if (policyItem.getIsActive() != null && policyItem.getIsActive() != 0 && policyItem.getIsActive() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "启用状态不合法");
        }
    }

    private TechnicianVisitFeePolicies findPolicyForUpdate(String accountId, AdminWorkerVisitFeePoliciesUpdateRequest.PolicyItem policyItem) {
        if (StringUtils.hasText(policyItem.getId())) {
            TechnicianVisitFeePolicies byId = technicianVisitFeePoliciesService.getById(policyItem.getId());
            if (byId != null
                && byId.getIsDelete() != null
                && byId.getIsDelete() == 0
                && accountId.equals(byId.getTechnicianAccountId())
                && policyItem.getServiceKind().equals(byId.getServiceKind())) {
                return byId;
            }
        }
        return technicianVisitFeePoliciesService.getOne(
            new LambdaQueryWrapper<TechnicianVisitFeePolicies>()
                .eq(TechnicianVisitFeePolicies::getTechnicianAccountId, accountId)
                .eq(TechnicianVisitFeePolicies::getServiceKind, policyItem.getServiceKind())
                .eq(TechnicianVisitFeePolicies::getIsDelete, 0)
                .orderByDesc(TechnicianVisitFeePolicies::getEffectiveTime)
                .orderByDesc(TechnicianVisitFeePolicies::getCreatedTime)
                .last("limit 1"),
            false
        );
    }

    private List<AdminWorkerSkillItemResponse> buildWorkerSkillResponseList(String accountId) {
        List<TechnicianSkills> skills = technicianSkillsService.list(
            new LambdaQueryWrapper<TechnicianSkills>()
                .eq(TechnicianSkills::getTechnicianAccountId, accountId)
                .eq(TechnicianSkills::getIsDelete, 0)
                .orderByDesc(TechnicianSkills::getUpdatedTime)
                .orderByDesc(TechnicianSkills::getCreatedTime)
        );
        if (skills == null || skills.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> serviceTypeIds = new LinkedHashSet<>();
        for (TechnicianSkills skill : skills) {
            if (skill != null && StringUtils.hasText(skill.getServiceTypeId())) {
                serviceTypeIds.add(skill.getServiceTypeId());
            }
        }

        Map<String, ServiceTypes> serviceTypeMap = new LinkedHashMap<>();
        if (!serviceTypeIds.isEmpty()) {
            List<ServiceTypes> serviceTypeList = serviceTypesService.list(
                new LambdaQueryWrapper<ServiceTypes>()
                    .in(ServiceTypes::getId, serviceTypeIds)
                    .eq(ServiceTypes::getIsDelete, 0)
            );
            for (ServiceTypes serviceType : serviceTypeList) {
                if (serviceType == null || !StringUtils.hasText(serviceType.getId())) {
                    continue;
                }
                serviceTypeMap.put(serviceType.getId(), serviceType);
            }
        }

        Map<String, ServiceCategories> categoryMap = new LinkedHashMap<>();
        List<ServiceCategories> categories = listActiveCategories();
        for (ServiceCategories category : categories) {
            if (category == null || !StringUtils.hasText(category.getId())) {
                continue;
            }
            categoryMap.put(category.getId(), category);
        }

        List<AdminWorkerSkillItemResponse> responseList = new ArrayList<>();
        for (TechnicianSkills skill : skills) {
            if (skill == null || !StringUtils.hasText(skill.getServiceTypeId())) {
                continue;
            }
            ServiceTypes serviceType = serviceTypeMap.get(skill.getServiceTypeId());
            responseList.add(toWorkerSkillItemResponse(skill, serviceType, categoryMap));
        }
        return responseList;
    }

    private AdminWorkerSkillItemResponse toWorkerSkillItemResponse(
        TechnicianSkills skill,
        ServiceTypes serviceType,
        Map<String, ServiceCategories> categoryMap
    ) {
        AdminWorkerSkillItemResponse response = new AdminWorkerSkillItemResponse();
        response.setId(skill.getId());
        response.setServiceTypeId(skill.getServiceTypeId());
        response.setServiceTypeName(serviceType == null ? "未知服务" : serviceType.getName());
        Integer serviceMode = serviceType == null ? null : serviceType.getType();
        response.setServiceMode(serviceMode);
        response.setServiceModeText(resolveServiceModeText(serviceMode));
        String categoryId = serviceType == null ? null : serviceType.getCategoryId();
        response.setCategoryId(categoryId);
        response.setCategoryPath(buildCategoryPath(categoryId, categoryMap));
        Integer skillLevel = skill.getSkillLevel() == null ? 1 : skill.getSkillLevel();
        response.setSkillLevel(skillLevel);
        response.setSkillLevelText(resolveSkillLevelText(skillLevel));
        response.setIsActive(skill.getIsActive() != null && skill.getIsActive() == 0 ? 0 : 1);
        response.setUpdatedTime(skill.getUpdatedTime());
        return response;
    }

    private List<ServiceCategories> listActiveCategories() {
        return serviceCategoriesService.list(
            new LambdaQueryWrapper<ServiceCategories>()
                .eq(ServiceCategories::getIsActive, 1)
                .eq(ServiceCategories::getIsDelete, 0)
                .orderByAsc(ServiceCategories::getLevel)
                .orderByAsc(ServiceCategories::getSortOrder)
                .orderByAsc(ServiceCategories::getCreatedTime)
        );
    }

    private List<ServiceTypes> listAvailableServiceTypeEntities(String accountId, String keyword, Integer serviceMode) {
        List<TechnicianSkills> existingSkills = technicianSkillsService.list(
            new LambdaQueryWrapper<TechnicianSkills>()
                .eq(TechnicianSkills::getTechnicianAccountId, accountId)
                .eq(TechnicianSkills::getIsDelete, 0)
        );
        Set<String> selectedTypeIds = new LinkedHashSet<>();
        for (TechnicianSkills skill : existingSkills) {
            if (skill != null && StringUtils.hasText(skill.getServiceTypeId())) {
                selectedTypeIds.add(skill.getServiceTypeId());
            }
        }

        List<ServiceTypes> serviceTypes = serviceTypesService.list(
            new LambdaQueryWrapper<ServiceTypes>()
                .eq(ServiceTypes::getIsActive, 1)
                .eq(ServiceTypes::getIsDelete, 0)
                .eq(serviceMode != null, ServiceTypes::getType, serviceMode)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                    .like(ServiceTypes::getName, keyword)
                    .or()
                    .like(ServiceTypes::getDescription, keyword)
                )
                .orderByAsc(ServiceTypes::getSortOrder)
                .orderByAsc(ServiceTypes::getCreatedTime)
        );

        List<ServiceTypes> available = new ArrayList<>();
        for (ServiceTypes serviceType : serviceTypes) {
            if (serviceType == null || !StringUtils.hasText(serviceType.getId())) {
                continue;
            }
            if (selectedTypeIds.contains(serviceType.getId())) {
                continue;
            }
            available.add(serviceType);
        }
        return available;
    }

    private Set<String> findDescendantCategoryIds(String categoryId, List<ServiceCategories> categories) {
        Set<String> descendants = new LinkedHashSet<>();
        if (!StringUtils.hasText(categoryId) || categories == null || categories.isEmpty()) {
            return descendants;
        }

        Map<String, List<String>> childrenByParentId = new LinkedHashMap<>();
        Set<String> allCategoryIds = new LinkedHashSet<>();
        for (ServiceCategories category : categories) {
            if (category == null || !StringUtils.hasText(category.getId())) {
                continue;
            }
            allCategoryIds.add(category.getId());
            String parentId = trimToNull(category.getParentId());
            if (!StringUtils.hasText(parentId)) {
                continue;
            }
            childrenByParentId.computeIfAbsent(parentId, key -> new ArrayList<>()).add(category.getId());
        }

        if (!allCategoryIds.contains(categoryId)) {
            return descendants;
        }

        List<String> queue = new ArrayList<>();
        queue.add(categoryId);
        for (int i = 0; i < queue.size(); i++) {
            String currentId = queue.get(i);
            if (!descendants.add(currentId)) {
                continue;
            }
            List<String> children = childrenByParentId.getOrDefault(currentId, Collections.emptyList());
            queue.addAll(children);
        }
        return descendants;
    }

    private WorkerSkillCategoryNode buildCategoryNode(
        ServiceCategories category,
        Map<String, List<ServiceCategories>> childrenByParentId
    ) {
        WorkerSkillCategoryNode node = new WorkerSkillCategoryNode();
        node.setId(category.getId());
        node.setName(category.getName());
        node.setLevel(category.getLevel());
        node.setParentId(category.getParentId());

        List<ServiceCategories> children = childrenByParentId.getOrDefault(category.getId(), Collections.emptyList());
        for (ServiceCategories child : children) {
            node.getChildren().add(buildCategoryNode(child, childrenByParentId));
        }
        return node;
    }

    private String buildCategoryPath(String categoryId, Map<String, ServiceCategories> categoryMap) {
        if (!StringUtils.hasText(categoryId) || categoryMap == null || categoryMap.isEmpty()) {
            return "";
        }
        List<String> names = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        String currentId = categoryId;
        while (StringUtils.hasText(currentId) && !visited.contains(currentId)) {
            visited.add(currentId);
            ServiceCategories category = categoryMap.get(currentId);
            if (category == null) {
                break;
            }
            if (StringUtils.hasText(category.getName())) {
                names.add(0, category.getName());
            }
            currentId = trimToNull(category.getParentId());
        }
        return String.join(" / ", names);
    }

    private void addSkillForWorker(String accountId, String serviceTypeId) {
        requireActiveServiceType(serviceTypeId);

        TechnicianSkills currentSkill = technicianSkillsService.getOne(
            new LambdaQueryWrapper<TechnicianSkills>()
                .eq(TechnicianSkills::getTechnicianAccountId, accountId)
                .eq(TechnicianSkills::getServiceTypeId, serviceTypeId)
                .eq(TechnicianSkills::getIsDelete, 0),
            false
        );
        if (currentSkill != null && (currentSkill.getIsActive() == null || currentSkill.getIsActive() == 1)) {
            return;
        }

        long now = System.currentTimeMillis();
        if (currentSkill != null) {
            currentSkill.setSkillLevel(1);
            currentSkill.setIsActive(1);
            currentSkill.setUpdatedTime(now);
            technicianSkillsService.updateById(currentSkill);
            return;
        }

        TechnicianSkills anySkill = technicianSkillsMapper.selectAnyByTechnicianAndServiceType(accountId, serviceTypeId);
        if (anySkill != null) {
            int restored = technicianSkillsMapper.restoreByTechnicianAndServiceType(accountId, serviceTypeId, now);
            if (restored <= 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "新增技能失败");
            }
            return;
        }

        TechnicianSkills skill = new TechnicianSkills();
        skill.setId(SnowflakeIdUtil.nextTechnicianSkillId());
        skill.setTechnicianAccountId(accountId);
        skill.setServiceTypeId(serviceTypeId);
        skill.setSkillLevel(1);
        skill.setIsActive(1);
        skill.setCreatedTime(now);
        skill.setUpdatedTime(now);
        skill.setIsDelete(0);

        try {
            boolean saved = technicianSkillsService.save(skill);
            if (!saved) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "新增技能失败");
            }
        } catch (DuplicateKeyException ex) {
            int restored = technicianSkillsMapper.restoreByTechnicianAndServiceType(accountId, serviceTypeId, now);
            if (restored <= 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "技能已存在");
            }
        }
    }

    private Integer normalizeServiceMode(Integer serviceMode) {
        if (serviceMode == null) {
            return null;
        }
        if (serviceMode == SERVICE_MODE_ONSITE_REPAIR
            || serviceMode == SERVICE_MODE_ONSITE_INSTALL
            || serviceMode == SERVICE_MODE_OFFLINE_REPAIR) {
            return serviceMode;
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "serviceMode 参数错误");
    }

    private String normalizeServiceTypeId(String serviceTypeId) {
        if (!StringUtils.hasText(serviceTypeId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "服务类型不能为空");
        }
        return serviceTypeId.trim();
    }

    private ServiceTypes requireActiveServiceType(String serviceTypeId) {
        ServiceTypes serviceType = serviceTypesService.getOne(
            new LambdaQueryWrapper<ServiceTypes>()
                .eq(ServiceTypes::getId, serviceTypeId)
                .eq(ServiceTypes::getIsActive, 1)
                .eq(ServiceTypes::getIsDelete, 0),
            false
        );
        if (serviceType == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "服务类型不存在或已禁用");
        }
        return serviceType;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String resolveSkillLevelText(Integer skillLevel) {
        if (skillLevel == null) {
            return "初级";
        }
        return switch (skillLevel) {
            case 1 -> "初级";
            case 2 -> "中级";
            case 3 -> "高级";
            case 4 -> "专家";
            default -> "初级";
        };
    }

    private String resolveServiceModeText(Integer serviceMode) {
        if (serviceMode == null) {
            return "未知类型";
        }
        return switch (serviceMode) {
            case SERVICE_MODE_ONSITE_REPAIR -> "上门维修";
            case SERVICE_MODE_ONSITE_INSTALL -> "上门安装";
            case SERVICE_MODE_OFFLINE_REPAIR -> "到店维修";
            default -> "未知类型";
        };
    }

    private void saveOperationLog(
        String adminAccountId,
        String operationType,
        String operationDesc,
        String requestUrl,
        String requestParams
    ) {
        long now = System.currentTimeMillis();
        OperationLogs log = new OperationLogs();
        log.setId("OL" + now + (int) (Math.random() * 1000));
        log.setOperatorId(adminAccountId);
        log.setOperatorType(ADMIN_OPERATOR_TYPE);
        log.setOperatorName(adminAccountId);
        log.setOperationType(operationType);
        log.setOperationDesc(operationDesc);
        log.setModuleName("ADMIN_WORKER");
        log.setRequestMethod("POST");
        log.setRequestUrl(requestUrl);
        log.setRequestParams(requestParams);
        log.setStatus(1);
        log.setCreatedTime(now);
        log.setIpAddress("");
        log.setVersion(1);
        log.setIsDelete(0);
        operationLogsService.save(log);
    }

    private void saveSystemMessage(
        String workerId,
        String title,
        String content,
        String businessType,
        int priority,
        long now
    ) {
        SystemMessages message = new SystemMessages();
        message.setId("SM" + now + (int) (Math.random() * 1000));
        message.setReceiverId(workerId);
        message.setReceiverType(SYSTEM_MESSAGE_FOR_WORKER);
        message.setTitle(title);
        message.setContent(content);
        message.setMessageType(3);
        message.setBusinessType(businessType);
        message.setBusinessId(workerId);
        message.setPriority(priority);
        message.setIsRead(0);
        message.setCreatedTime(now);
        message.setUpdatedTime(now);
        message.setVersion(1);
        message.setIsDelete(0);
        systemMessagesService.save(message);
    }
}
