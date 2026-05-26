package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.FaultPhenomena;
import com.example.backend.entity.Images;
import com.example.backend.entity.RepairOrderFaults;
import com.example.backend.entity.ServiceCategories;
import com.example.backend.entity.ServiceTypes;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.admin.AdminFaultPhenomenonCreateRequest;
import com.example.backend.model.admin.AdminFaultPhenomenonBatchCopyRequest;
import com.example.backend.model.admin.AdminFaultPhenomenonResponse;
import com.example.backend.model.admin.AdminFaultPhenomenonUpdateRequest;
import com.example.backend.model.admin.AdminServiceCategoryCreateRequest;
import com.example.backend.model.admin.AdminServiceCategoryResponse;
import com.example.backend.model.admin.AdminServiceCategoryUpdateRequest;
import com.example.backend.model.admin.AdminServiceTypeBatchCopyRequest;
import com.example.backend.model.admin.AdminServiceTypeCreateRequest;
import com.example.backend.model.admin.AdminServiceTypeResponse;
import com.example.backend.model.admin.AdminServiceTypeUpdateRequest;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.FaultPhenomenaService;
import com.example.backend.service.ImagesService;
import com.example.backend.service.AdminServiceConfigService;
import com.example.backend.service.RepairOrderFaultsService;
import com.example.backend.service.ServiceCategoriesService;
import com.example.backend.service.ServiceTypesService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import com.example.backend.utils.oss.OssUtil;
import com.example.backend.utils.upload.UploadLimitUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminServiceConfigServiceImpl implements AdminServiceConfigService {

    private static final String SERVICE_CATEGORY_ICON_BUSINESS_TYPE = "SERVERCATEGORY";

    private final ServiceCategoriesService serviceCategoriesService;
    private final ServiceTypesService serviceTypesService;
    private final FaultPhenomenaService faultPhenomenaService;
    private final RepairOrderFaultsService repairOrderFaultsService;
    private final ImagesService imagesService;
    private final OssUtil ossUtil;

    public AdminServiceConfigServiceImpl(
        ServiceCategoriesService serviceCategoriesService,
        ServiceTypesService serviceTypesService,
        FaultPhenomenaService faultPhenomenaService,
        RepairOrderFaultsService repairOrderFaultsService,
        ImagesService imagesService,
        OssUtil ossUtil
    ) {
        this.serviceCategoriesService = serviceCategoriesService;
        this.serviceTypesService = serviceTypesService;
        this.faultPhenomenaService = faultPhenomenaService;
        this.repairOrderFaultsService = repairOrderFaultsService;
        this.imagesService = imagesService;
        this.ossUtil = ossUtil;
    }

    @Override
    public List<AdminServiceCategoryResponse> listServiceCategories() {
        List<ServiceCategories> categories = serviceCategoriesService.list(
            new LambdaQueryWrapper<ServiceCategories>()
                .orderByAsc(ServiceCategories::getSortOrder)
                .orderByDesc(ServiceCategories::getCreatedTime)
        );
        Map<String, ServiceCategories> map = categories.stream()
            .collect(Collectors.toMap(ServiceCategories::getId, c -> c, (a, b) -> a));
        Map<String, String> iconMap = loadLatestCategoryIconUrlMap(
            categories.stream()
                .filter(c -> Objects.equals(c.getLevel(), 3))
                .map(ServiceCategories::getId)
                .collect(Collectors.toList())
        );
        List<AdminServiceCategoryResponse> resp = new ArrayList<>();
        for (ServiceCategories c : categories) {
            resp.add(toCategoryResponse(c, map, iconMap.get(c.getId())));
        }
        return resp;
    }

    @Override
    public AdminServiceCategoryResponse createServiceCategory(AdminServiceCategoryCreateRequest request) {
        String parentId = normalizeBlankToNull(request.getParentId());

        ServiceCategories parent = null;
        if (StringUtils.hasText(parentId)) {
            parent = serviceCategoriesService.getById(parentId);
            if (parent == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "Service category not found");
            }
            if (defaultIfNull(parent.getLevel(), 1) >= 3) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "Service category supports up to 3 levels");
            }
        }

        long now = System.currentTimeMillis();
        ServiceCategories category = new ServiceCategories();
        category.setId(SnowflakeIdUtil.nextServiceCategoryId());
        category.setName(request.getName());
        category.setCode(generateCategoryCode(category.getId()));
        category.setParentId(parentId);
        category.setDescription(request.getDescription());
        int categoryIsActive = defaultIfNull(request.getIsActive(), 1);
        if (parent != null && Objects.equals(parent.getIsActive(), 0)) {
            categoryIsActive = 0;
        }
        category.setIsActive(categoryIsActive);
        category.setSortOrder(defaultIfNull(request.getSortOrder(), 0));
        category.setCreatedTime(now);
        category.setUpdatedTime(now);

        if (parent == null) {
            category.setLevel(1);
            category.setPath("/" + category.getId() + "/");
        } else {
            category.setLevel(defaultIfNull(parent.getLevel(), 1) + 1);
            category.setPath(parent.getPath() + category.getId() + "/");
        }

        boolean ok = serviceCategoriesService.save(category);
        if (!ok) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Create service category failed");
        }

        Map<String, ServiceCategories> map = new HashMap<>();
        map.put(category.getId(), category);
        if (parent != null) {
            map.put(parent.getId(), parent);
        }
        return toCategoryResponse(category, map, null);
    }

    @Override
    public AdminServiceCategoryResponse updateServiceCategory(String id, AdminServiceCategoryUpdateRequest request) {
        ServiceCategories current = serviceCategoriesService.getById(id);
        if (current == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Service category not found");
        }

        String newParentId = normalizeBlankToNull(request.getParentId());
        if (Objects.equals(newParentId, id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Parent category cannot be itself");
        }

        ServiceCategories newParent = null;
        if (StringUtils.hasText(newParentId)) {
            newParent = serviceCategoriesService.getById(newParentId);
            if (newParent == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "Service category not found");
            }
            if (StringUtils.hasText(current.getPath()) && StringUtils.hasText(newParent.getPath())
                && newParent.getPath().startsWith(current.getPath())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "Parent category cannot be a child of current category");
            }
        }

        long now = System.currentTimeMillis();

        String oldPath = current.getPath();
        Integer oldLevel = defaultIfNull(current.getLevel(), 1);

        current.setName(request.getName());
        if (!StringUtils.hasText(current.getCode())) {
            current.setCode(generateCategoryCode(current.getId()));
        }
        current.setDescription(request.getDescription());
        int requestedIsActive = defaultIfNull(request.getIsActive(), 1);
        if (newParent != null && Objects.equals(newParent.getIsActive(), 0)) {
            requestedIsActive = 0;
        }
        current.setIsActive(requestedIsActive);
        current.setSortOrder(defaultIfNull(request.getSortOrder(), 0));
        current.setUpdatedTime(now);

        Integer newLevel;
        String newPath;
        if (newParent == null) {
            newLevel = 1;
            newPath = "/" + current.getId() + "/";
        } else {
            newLevel = defaultIfNull(newParent.getLevel(), 1) + 1;
            newPath = newParent.getPath() + current.getId() + "/";
        }
        if (newLevel > 3) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Service category supports up to 3 levels");
        }

        boolean parentChanged = !Objects.equals(normalizeBlankToNull(current.getParentId()), newParentId);
        boolean pathChanged = parentChanged && !Objects.equals(oldPath, newPath);
        boolean cascadeDisable = requestedIsActive == 0;
        if (pathChanged && StringUtils.hasText(oldPath)) {
            List<ServiceCategories> descendantsForLevelCheck = serviceCategoriesService.list(
                new LambdaQueryWrapper<ServiceCategories>()
                    .likeRight(ServiceCategories::getPath, oldPath)
            );
            int maxOldLevel = oldLevel;
            for (ServiceCategories d : descendantsForLevelCheck) {
                maxOldLevel = Math.max(maxOldLevel, defaultIfNull(d.getLevel(), oldLevel));
            }
            int subtreeDepth = maxOldLevel - oldLevel;
            if (newLevel + subtreeDepth > 3) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "Service category supports up to 3 levels");
            }
        }

        current.setParentId(newParentId);
        current.setLevel(newLevel);
        current.setPath(newPath);

        boolean ok = serviceCategoriesService.updateById(current);
        if (!ok) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Update service category failed");
        }

        if ((pathChanged || cascadeDisable) && StringUtils.hasText(oldPath)) {
            int delta = newLevel - oldLevel;
            List<ServiceCategories> descendants = serviceCategoriesService.list(
                new LambdaQueryWrapper<ServiceCategories>()
                    .likeRight(ServiceCategories::getPath, oldPath)
            );
            List<ServiceCategories> toUpdate = new ArrayList<>();
            for (ServiceCategories d : descendants) {
                if (Objects.equals(d.getId(), id)) {
                    continue;
                }
                boolean changed = false;

                if (pathChanged) {
                    String dPath = d.getPath();
                    if (!StringUtils.hasText(dPath) || dPath.length() < oldPath.length()) {
                        continue;
                    }
                    d.setPath(newPath + dPath.substring(oldPath.length()));
                    d.setLevel(defaultIfNull(d.getLevel(), 1) + delta);
                    changed = true;
                }

                if (cascadeDisable && !Objects.equals(d.getIsActive(), 0)) {
                    d.setIsActive(0);
                    changed = true;
                }

                if (changed) {
                    d.setUpdatedTime(now);
                    toUpdate.add(d);
                }
            }
            if (!toUpdate.isEmpty()) {
                serviceCategoriesService.updateBatchById(toUpdate);
            }
        }

        Map<String, ServiceCategories> map = serviceCategoriesService.list().stream()
            .collect(Collectors.toMap(ServiceCategories::getId, c -> c, (a, b) -> a));
        String iconUrl = null;
        if (Objects.equals(current.getLevel(), 3)) {
            iconUrl = loadLatestCategoryIconUrlMap(List.of(current.getId())).get(current.getId());
        }
        return toCategoryResponse(current, map, iconUrl);
    }

    @Override
    public void deleteServiceCategory(String id) {
        ServiceCategories current = serviceCategoriesService.getById(id);
        if (current == null) {
            return;
        }

        long childrenCount = serviceCategoriesService.count(
            new LambdaQueryWrapper<ServiceCategories>()
                .eq(ServiceCategories::getParentId, id)
        );
        if (childrenCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Category has child categories and cannot be deleted");
        }

        long typeCount = serviceTypesService.count(
            new LambdaQueryWrapper<ServiceTypes>()
                .eq(ServiceTypes::getCategoryId, id)
        );
        if (typeCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Category has service types and cannot be deleted");
        }

        serviceCategoriesService.removeById(id);
    }

    @Override
    public List<AdminServiceTypeResponse> listServiceTypes() {
        List<ServiceTypes> types = serviceTypesService.list(
            new LambdaQueryWrapper<ServiceTypes>()
                .orderByAsc(ServiceTypes::getSortOrder)
                .orderByDesc(ServiceTypes::getCreatedTime)
        );
        Map<String, ServiceCategories> categoryMap = serviceCategoriesService.list().stream()
            .collect(Collectors.toMap(ServiceCategories::getId, item -> item, (a, b) -> a));

        List<AdminServiceTypeResponse> resp = new ArrayList<>();
        for (ServiceTypes t : types) {
            resp.add(toServiceTypeResponse(t, categoryMap.get(t.getCategoryId())));
        }
        return resp;
    }

    @Override
    public AdminServiceTypeResponse createServiceType(AdminServiceTypeCreateRequest request) {
        ServiceCategories category = requireServiceTypeCategory(request.getCategoryId());

        long now = System.currentTimeMillis();
        ServiceTypes t = new ServiceTypes();
        t.setId(SnowflakeIdUtil.nextServiceTypeId());
        t.setName(request.getName());
        t.setType(request.getType());
        t.setCategoryId(request.getCategoryId());
        t.setDescription(request.getDescription());
        t.setBasePrice(request.getBasePrice());
        t.setIsActive(defaultIfNull(request.getIsActive(), 1));
        t.setSortOrder(defaultIfNull(request.getSortOrder(), 0));
        t.setCreatedTime(now);
        t.setUpdatedTime(now);

        boolean ok = serviceTypesService.save(t);
        if (!ok) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to create service type");
        }

        return toServiceTypeResponse(t, category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AdminServiceTypeResponse> copyServiceTypes(AdminServiceTypeBatchCopyRequest request) {
        if (request == null || request.getSourceIds() == null || request.getSourceIds().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择要复制的服务类型");
        }
        Integer targetType = normalizeServiceTypeValue(request.getTargetType());
        List<String> sourceIds = request.getSourceIds().stream()
            .map(AdminServiceConfigServiceImpl::normalizeBlankToNull)
            .filter(StringUtils::hasText)
            .distinct()
            .collect(Collectors.toList());
        if (sourceIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择要复制的服务类型");
        }

        List<ServiceTypes> sourceTypes = serviceTypesService.list(
            new LambdaQueryWrapper<ServiceTypes>()
                .in(ServiceTypes::getId, sourceIds)
                .eq(ServiceTypes::getIsDelete, 0)
                .orderByAsc(ServiceTypes::getSortOrder)
                .orderByAsc(ServiceTypes::getCreatedTime)
        );
        if (sourceTypes.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "未找到可复制的服务类型");
        }

        Map<String, ServiceTypes> sourceTypeMap = sourceTypes.stream()
            .collect(Collectors.toMap(ServiceTypes::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
        List<ServiceTypes> orderedSourceTypes = sourceIds.stream()
            .map(sourceTypeMap::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        if (orderedSourceTypes.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "未找到可复制的服务类型");
        }

        Set<String> categoryIds = orderedSourceTypes.stream()
            .map(ServiceTypes::getCategoryId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> sourceNames = orderedSourceTypes.stream()
            .map(ServiceTypes::getName)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!categoryIds.isEmpty() && !sourceNames.isEmpty()) {
            List<ServiceTypes> existingTypes = serviceTypesService.list(
                new LambdaQueryWrapper<ServiceTypes>()
                    .eq(ServiceTypes::getType, targetType)
                    .in(ServiceTypes::getCategoryId, categoryIds)
                    .in(ServiceTypes::getName, sourceNames)
                    .eq(ServiceTypes::getIsDelete, 0)
            );
            Set<String> existingKeys = existingTypes.stream()
                .map(item -> buildTypeUniqueKey(item.getCategoryId(), item.getType(), item.getName()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
            List<String> duplicateNames = orderedSourceTypes.stream()
                .filter(item -> existingKeys.contains(buildTypeUniqueKey(item.getCategoryId(), targetType, item.getName())))
                .map(ServiceTypes::getName)
                .distinct()
                .collect(Collectors.toList());
            if (!duplicateNames.isEmpty()) {
                throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR,
                    "以下服务类型在目标类型下已存在：" + String.join("、", duplicateNames)
                );
            }
        }

        long now = System.currentTimeMillis();
        List<ServiceTypes> copiedTypes = new ArrayList<>();
        for (ServiceTypes sourceType : orderedSourceTypes) {
            ServiceTypes copiedType = new ServiceTypes();
            copiedType.setId(SnowflakeIdUtil.nextServiceTypeId());
            copiedType.setName(sourceType.getName());
            copiedType.setType(targetType);
            copiedType.setCategoryId(sourceType.getCategoryId());
            copiedType.setDescription(sourceType.getDescription());
            copiedType.setBasePrice(sourceType.getBasePrice());
            copiedType.setIsActive(sourceType.getIsActive());
            copiedType.setSortOrder(sourceType.getSortOrder());
            copiedType.setCreatedTime(now);
            copiedType.setUpdatedTime(now);
            copiedTypes.add(copiedType);
        }
        if (!serviceTypesService.saveBatch(copiedTypes)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "批量复制服务类型失败");
        }

        Map<String, String> copiedTypeIdMap = new LinkedHashMap<>();
        for (int i = 0; i < orderedSourceTypes.size() && i < copiedTypes.size(); i++) {
            copiedTypeIdMap.put(orderedSourceTypes.get(i).getId(), copiedTypes.get(i).getId());
        }
        copyFaultPhenomenaByTypeMapping(copiedTypeIdMap, now);

        Map<String, ServiceCategories> categoryMap = serviceCategoriesService.list().stream()
            .collect(Collectors.toMap(ServiceCategories::getId, item -> item, (a, b) -> a));
        return copiedTypes.stream()
            .map(item -> toServiceTypeResponse(item, categoryMap.get(item.getCategoryId())))
            .collect(Collectors.toList());
    }

    @Override
    public AdminServiceTypeResponse updateServiceType(String id, AdminServiceTypeUpdateRequest request) {
        ServiceTypes current = serviceTypesService.getById(id);
        if (current == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Service type not found");
        }

        ServiceCategories category = requireServiceTypeCategory(request.getCategoryId());

        long now = System.currentTimeMillis();
        current.setName(request.getName());
        current.setType(request.getType());
        current.setCategoryId(request.getCategoryId());
        current.setDescription(request.getDescription());
        current.setBasePrice(request.getBasePrice());
        current.setIsActive(defaultIfNull(request.getIsActive(), 1));
        current.setSortOrder(defaultIfNull(request.getSortOrder(), 0));
        current.setUpdatedTime(now);

        boolean ok = serviceTypesService.updateById(current);
        if (!ok) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to update service type");
        }

        return toServiceTypeResponse(current, category);
    }

    @Override
    public void deleteServiceType(String id) {
        ServiceTypes current = serviceTypesService.getById(id);
        if (current == null) {
            return;
        }

        long faultCount = faultPhenomenaService.count(
            new LambdaQueryWrapper<FaultPhenomena>()
                .eq(FaultPhenomena::getServiceTypeId, id)
        );
        if (faultCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Service type has fault phenomena and cannot be deleted");
        }

        serviceTypesService.removeById(id);
    }

    @Override
    public String uploadServiceCategoryIcon(String categoryId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Please select an icon file to upload");
        }
        UploadLimitUtil.validateImageSize(file);

        ServiceCategories category = serviceCategoriesService.getById(categoryId);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Service category not found");
        }
        if (!Objects.equals(category.getLevel(), 3)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Only level-3 service categories require an icon");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to read upload file");
        }

        Integer width = null;
        Integer height = null;
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image != null) {
                width = image.getWidth();
                height = image.getHeight();
            }
        } catch (Exception ignored) {
        }

        String origin = Optional.ofNullable(file.getOriginalFilename()).orElse("icon");
        String ext = "";
        int dot = origin.lastIndexOf('.');
        if (dot >= 0 && dot < origin.length() - 1) {
            ext = origin.substring(dot);
        }

        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String objectName = "service-category-icons/" + categoryId + "/" + date + "_" + UUID.randomUUID().toString().replace("-", "") + ext;

        String url = ossUtil.upload(objectName, new ByteArrayInputStream(bytes));

        LoginUserInfo user = AuthUserContext.get();
        long now = System.currentTimeMillis();
        Images image = new Images();
        image.setId(SnowflakeIdUtil.nextImageId());
        image.setOriginalName(origin);
        image.setFileName(objectName.substring(objectName.lastIndexOf('/') + 1));
        image.setFilePath(objectName);
        image.setFileUrl(url);
        image.setFileSize(file.getSize());
        image.setMimeType(file.getContentType());
        image.setWidth(width);
        image.setHeight(height);
        image.setUploaderId(user == null ? null : user.getAccountId());
        image.setUploaderType(3);
        image.setBusinessType(SERVICE_CATEGORY_ICON_BUSINESS_TYPE);
        image.setBusinessId(categoryId);
        image.setCreatedTime(now);

        boolean ok = imagesService.save(image);
        if (!ok) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to save icon record");
        }

        return url;
    }

    @Override
    public List<AdminFaultPhenomenonResponse> listFaultPhenomena(String serviceTypeId) {
        LambdaQueryWrapper<FaultPhenomena> wrapper = new LambdaQueryWrapper<FaultPhenomena>()
            .orderByAsc(FaultPhenomena::getSortOrder)
            .orderByDesc(FaultPhenomena::getCreatedTime);
        if (StringUtils.hasText(serviceTypeId)) {
            wrapper.eq(FaultPhenomena::getServiceTypeId, serviceTypeId);
        }
        List<FaultPhenomena> faults = faultPhenomenaService.list(wrapper);
        Map<String, ServiceTypes> typeMap = serviceTypesService.list().stream()
            .collect(Collectors.toMap(ServiceTypes::getId, item -> item, (a, b) -> a));
        Map<String, ServiceCategories> categoryMap = serviceCategoriesService.list().stream()
            .collect(Collectors.toMap(ServiceCategories::getId, item -> item, (a, b) -> a));

        List<AdminFaultPhenomenonResponse> resp = new ArrayList<>();
        for (FaultPhenomena f : faults) {
            resp.add(toFaultPhenomenonResponse(f, typeMap.get(f.getServiceTypeId()), categoryMap));
        }
        return resp;
    }

    @Override
    public AdminFaultPhenomenonResponse createFaultPhenomenon(AdminFaultPhenomenonCreateRequest request) {
        ServiceTypes type = serviceTypesService.getById(request.getServiceTypeId());
        if (type == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Service type not found");
        }
        Map<String, ServiceCategories> categoryMap = serviceCategoriesService.list().stream()
            .collect(Collectors.toMap(ServiceCategories::getId, item -> item, (a, b) -> a));

        long now = System.currentTimeMillis();
        FaultPhenomena f = new FaultPhenomena();
        f.setId(SnowflakeIdUtil.nextFaultPhenomenonId());
        f.setServiceTypeId(request.getServiceTypeId());
        f.setName(request.getName());
        f.setDescription(request.getDescription());
        f.setEstimatedPriceMin(request.getEstimatedPriceMin());
        f.setEstimatedPriceMax(request.getEstimatedPriceMax());
        f.setIsActive(defaultIfNull(request.getIsActive(), 1));
        f.setSortOrder(defaultIfNull(request.getSortOrder(), 0));
        f.setCreatedTime(now);
        f.setUpdatedTime(now);

        boolean ok = faultPhenomenaService.save(f);
        if (!ok) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to create fault phenomenon");
        }

        return toFaultPhenomenonResponse(f, type, categoryMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AdminFaultPhenomenonResponse> copyFaultPhenomena(AdminFaultPhenomenonBatchCopyRequest request) {
        if (request == null || request.getSourceIds() == null || request.getSourceIds().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择要复制的故障现象");
        }

        String targetServiceTypeId = normalizeBlankToNull(request.getTargetServiceTypeId());
        if (!StringUtils.hasText(targetServiceTypeId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择目标服务类型");
        }

        ServiceTypes targetType = serviceTypesService.getById(targetServiceTypeId);
        if (targetType == null || Objects.equals(targetType.getIsDelete(), 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "目标服务类型不存在");
        }

        List<String> sourceIds = request.getSourceIds().stream()
            .map(AdminServiceConfigServiceImpl::normalizeBlankToNull)
            .filter(StringUtils::hasText)
            .distinct()
            .collect(Collectors.toList());
        if (sourceIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择要复制的故障现象");
        }

        List<FaultPhenomena> sourceFaults = faultPhenomenaService.list(
            new LambdaQueryWrapper<FaultPhenomena>()
                .in(FaultPhenomena::getId, sourceIds)
                .eq(FaultPhenomena::getIsDelete, 0)
                .orderByAsc(FaultPhenomena::getSortOrder)
                .orderByAsc(FaultPhenomena::getCreatedTime)
        );
        if (sourceFaults.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "未找到可复制的故障现象");
        }

        Map<String, FaultPhenomena> sourceFaultMap = sourceFaults.stream()
            .collect(Collectors.toMap(FaultPhenomena::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
        List<FaultPhenomena> orderedSourceFaults = sourceIds.stream()
            .map(sourceFaultMap::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        if (orderedSourceFaults.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "未找到可复制的故障现象");
        }

        long now = System.currentTimeMillis();
        List<FaultPhenomena> copiedFaults = buildCopiedFaultPhenomena(orderedSourceFaults, targetServiceTypeId, now);
        if (!faultPhenomenaService.saveBatch(copiedFaults)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "批量复制故障现象失败");
        }

        Map<String, ServiceCategories> categoryMap = serviceCategoriesService.list().stream()
            .collect(Collectors.toMap(ServiceCategories::getId, item -> item, (a, b) -> a));
        return copiedFaults.stream()
            .map(item -> toFaultPhenomenonResponse(item, targetType, categoryMap))
            .collect(Collectors.toList());
    }

    @Override
    public AdminFaultPhenomenonResponse updateFaultPhenomenon(String id, AdminFaultPhenomenonUpdateRequest request) {
        FaultPhenomena current = faultPhenomenaService.getById(id);
        if (current == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Fault phenomenon not found");
        }

        ServiceTypes type = serviceTypesService.getById(request.getServiceTypeId());
        if (type == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Service type not found");
        }
        Map<String, ServiceCategories> categoryMap = serviceCategoriesService.list().stream()
            .collect(Collectors.toMap(ServiceCategories::getId, item -> item, (a, b) -> a));

        long now = System.currentTimeMillis();
        current.setServiceTypeId(request.getServiceTypeId());
        current.setName(request.getName());
        current.setDescription(request.getDescription());
        current.setEstimatedPriceMin(request.getEstimatedPriceMin());
        current.setEstimatedPriceMax(request.getEstimatedPriceMax());
        current.setIsActive(defaultIfNull(request.getIsActive(), 1));
        current.setSortOrder(defaultIfNull(request.getSortOrder(), 0));
        current.setUpdatedTime(now);

        boolean ok = faultPhenomenaService.updateById(current);
        if (!ok) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to update fault phenomenon");
        }

        return toFaultPhenomenonResponse(current, type, categoryMap);
    }

    @Override
    public void deleteFaultPhenomenon(String id) {
        FaultPhenomena current = faultPhenomenaService.getById(id);
        if (current == null) {
            return;
        }

        long useCount = repairOrderFaultsService.count(
            new LambdaQueryWrapper<RepairOrderFaults>()
                .eq(RepairOrderFaults::getFaultPhenomenonId, id)
        );
        if (useCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Delete failed: this fault phenomenon is referenced by orders");
        }

        faultPhenomenaService.removeById(id);
    }

    private AdminServiceCategoryResponse toCategoryResponse(ServiceCategories c, Map<String, ServiceCategories> map, String iconUrl) {
        AdminServiceCategoryResponse r = new AdminServiceCategoryResponse();
        r.setId(c.getId());
        r.setName(c.getName());
        r.setCode(c.getCode());
        r.setParentId(c.getParentId());
        if (StringUtils.hasText(c.getParentId())) {
            ServiceCategories p = map.get(c.getParentId());
            if (p != null) {
                r.setParentName(p.getName());
            }
        }
        r.setLevel(c.getLevel());
        r.setPath(c.getPath());
        r.setDescription(c.getDescription());
        r.setIsActive(c.getIsActive());
        r.setSortOrder(c.getSortOrder());
        r.setCreatedTime(c.getCreatedTime());
        r.setUpdatedTime(c.getUpdatedTime());
        r.setIconUrl(iconUrl);
        return r;
    }

    private Map<String, String> loadLatestCategoryIconUrlMap(List<String> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
        List<Images> images = imagesService.list(
            new LambdaQueryWrapper<Images>()
                .eq(Images::getBusinessType, SERVICE_CATEGORY_ICON_BUSINESS_TYPE)
                .in(Images::getBusinessId, categoryIds)
                .orderByDesc(Images::getCreatedTime)
        );
        Map<String, String> result = new HashMap<>();
        for (Images img : images) {
            if (!result.containsKey(img.getBusinessId())) {
                result.put(img.getBusinessId(), img.getFileUrl());
            }
        }
        return result;
    }

    private ServiceCategories requireServiceTypeCategory(String categoryId) {
        ServiceCategories category = serviceCategoriesService.getById(categoryId);
        if (category == null || Objects.equals(category.getIsDelete(), 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Service category not found");
        }
        Integer level = defaultIfNull(category.getLevel(), 0);
        if (level != 2 && level != 3) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Service type category must be level 2 or level 3");
        }
        return category;
    }

    private AdminServiceTypeResponse toServiceTypeResponse(ServiceTypes serviceType, ServiceCategories category) {
        AdminServiceTypeResponse resp = new AdminServiceTypeResponse();
        resp.setId(serviceType.getId());
        resp.setName(serviceType.getName());
        resp.setType(serviceType.getType());
        resp.setCategoryId(serviceType.getCategoryId());
        resp.setCategoryName(category == null ? null : category.getName());
        resp.setDescription(serviceType.getDescription());
        resp.setBasePrice(serviceType.getBasePrice());
        resp.setIsActive(serviceType.getIsActive());
        resp.setSortOrder(serviceType.getSortOrder());
        resp.setCreatedTime(serviceType.getCreatedTime());
        resp.setUpdatedTime(serviceType.getUpdatedTime());
        return resp;
    }

    private AdminFaultPhenomenonResponse toFaultPhenomenonResponse(
        FaultPhenomena fault,
        ServiceTypes serviceType,
        Map<String, ServiceCategories> categoryMap
    ) {
        AdminFaultPhenomenonResponse resp = new AdminFaultPhenomenonResponse();
        resp.setId(fault.getId());
        resp.setServiceTypeId(fault.getServiceTypeId());
        resp.setServiceTypeName(serviceType == null ? null : serviceType.getName());
        resp.setServiceTypeType(serviceType == null ? null : serviceType.getType());
        String categoryId = serviceType == null ? null : normalizeBlankToNull(serviceType.getCategoryId());
        ServiceCategories category = StringUtils.hasText(categoryId) && categoryMap != null ? categoryMap.get(categoryId) : null;
        resp.setServiceCategoryId(categoryId);
        resp.setServiceCategoryName(category == null ? null : category.getName());
        resp.setServiceCategoryPath(buildCategoryPath(categoryId, categoryMap));
        resp.setName(fault.getName());
        resp.setDescription(fault.getDescription());
        resp.setEstimatedPriceMin(fault.getEstimatedPriceMin());
        resp.setEstimatedPriceMax(fault.getEstimatedPriceMax());
        resp.setIsActive(fault.getIsActive());
        resp.setSortOrder(fault.getSortOrder());
        resp.setCreatedTime(fault.getCreatedTime());
        resp.setUpdatedTime(fault.getUpdatedTime());
        return resp;
    }

    private String buildCategoryPath(String categoryId, Map<String, ServiceCategories> categoryMap) {
        if (!StringUtils.hasText(categoryId) || categoryMap == null || categoryMap.isEmpty()) {
            return "";
        }
        List<String> names = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        String currentId = categoryId;
        while (StringUtils.hasText(currentId) && visited.add(currentId)) {
            ServiceCategories category = categoryMap.get(currentId);
            if (category == null) {
                break;
            }
            if (StringUtils.hasText(category.getName())) {
                names.add(0, category.getName());
            }
            currentId = normalizeBlankToNull(category.getParentId());
        }
        return String.join(" / ", names);
    }

    private Integer normalizeServiceTypeValue(Integer type) {
        if (type == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择复制后的服务类型");
        }
        if (type == 1 || type == 2 || type == 3) {
            return type;
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "服务类型仅支持上门维修、上门安装、线下维修");
    }

    private void copyFaultPhenomenaByTypeMapping(Map<String, String> sourceToTargetTypeIdMap, long now) {
        if (sourceToTargetTypeIdMap == null || sourceToTargetTypeIdMap.isEmpty()) {
            return;
        }

        List<FaultPhenomena> sourceFaults = faultPhenomenaService.list(
            new LambdaQueryWrapper<FaultPhenomena>()
                .in(FaultPhenomena::getServiceTypeId, sourceToTargetTypeIdMap.keySet())
                .eq(FaultPhenomena::getIsDelete, 0)
                .orderByAsc(FaultPhenomena::getSortOrder)
                .orderByAsc(FaultPhenomena::getCreatedTime)
        );
        if (sourceFaults.isEmpty()) {
            return;
        }

        List<FaultPhenomena> copiedFaults = new ArrayList<>();
        for (FaultPhenomena sourceFault : sourceFaults) {
            String targetServiceTypeId = sourceToTargetTypeIdMap.get(sourceFault.getServiceTypeId());
            if (!StringUtils.hasText(targetServiceTypeId)) {
                continue;
            }
            copiedFaults.add(buildCopiedFaultPhenomenon(sourceFault, targetServiceTypeId, now));
        }
        if (!copiedFaults.isEmpty() && !faultPhenomenaService.saveBatch(copiedFaults)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "复制服务类型下属故障现象失败");
        }
    }

    private List<FaultPhenomena> buildCopiedFaultPhenomena(List<FaultPhenomena> sourceFaults, String targetServiceTypeId, long now) {
        return sourceFaults.stream()
            .map(item -> buildCopiedFaultPhenomenon(item, targetServiceTypeId, now))
            .collect(Collectors.toList());
    }

    private FaultPhenomena buildCopiedFaultPhenomenon(FaultPhenomena sourceFault, String targetServiceTypeId, long now) {
        FaultPhenomena copiedFault = new FaultPhenomena();
        copiedFault.setId(SnowflakeIdUtil.nextFaultPhenomenonId());
        copiedFault.setServiceTypeId(targetServiceTypeId);
        copiedFault.setName(sourceFault.getName());
        copiedFault.setDescription(sourceFault.getDescription());
        copiedFault.setEstimatedPriceMin(sourceFault.getEstimatedPriceMin());
        copiedFault.setEstimatedPriceMax(sourceFault.getEstimatedPriceMax());
        copiedFault.setIsActive(sourceFault.getIsActive());
        copiedFault.setSortOrder(sourceFault.getSortOrder());
        copiedFault.setCreatedTime(now);
        copiedFault.setUpdatedTime(now);
        return copiedFault;
    }

    private String buildTypeUniqueKey(String categoryId, Integer type, String name) {
        return normalizeBlankToNull(categoryId) + "|" + defaultIfNull(type, 0) + "|" + normalizeBlankToNull(name);
    }

    private static String normalizeBlankToNull(String v) {
        if (!StringUtils.hasText(v)) {
            return null;
        }
        return v.trim();
    }

    private static String generateCategoryCode(String categoryId) {
        if (!StringUtils.hasText(categoryId) || categoryId.length() <= 2) {
            return "CAT" + System.currentTimeMillis();
        }
        return "CAT" + categoryId.substring(2);
    }

    private static <T> T defaultIfNull(T v, T def) {
        return v == null ? def : v;
    }
}


