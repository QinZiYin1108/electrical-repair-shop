package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.ServiceCategories;
import com.example.backend.entity.ServiceTypes;
import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.entity.TechnicianSkills;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.TechnicianSkillsMapper;
import com.example.backend.model.worker.WorkerSkillBatchCreateRequest;
import com.example.backend.model.worker.WorkerSkillCategoryNode;
import com.example.backend.model.worker.WorkerSkillCreateRequest;
import com.example.backend.model.worker.WorkerSkillDeleteRequest;
import com.example.backend.model.worker.WorkerSkillItem;
import com.example.backend.model.worker.WorkerSkillServiceTypeOption;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.ServiceCategoriesService;
import com.example.backend.service.ServiceTypesService;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.service.TechnicianSkillsService;
import com.example.backend.service.WorkerSkillService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class WorkerSkillServiceImpl implements WorkerSkillService {

    private static final int SERVICE_MODE_ONSITE_REPAIR = 1;
    private static final int SERVICE_MODE_ONSITE_INSTALL = 2;
    private static final int SERVICE_MODE_OFFLINE_REPAIR = 3;

    private final TechnicianSkillsService technicianSkillsService;
    private final TechnicianSkillsMapper technicianSkillsMapper;
    private final ServiceTypesService serviceTypesService;
    private final ServiceCategoriesService serviceCategoriesService;
    private final TechnicianAccountsService technicianAccountsService;

    public WorkerSkillServiceImpl(
        TechnicianSkillsService technicianSkillsService,
        TechnicianSkillsMapper technicianSkillsMapper,
        ServiceTypesService serviceTypesService,
        ServiceCategoriesService serviceCategoriesService,
        TechnicianAccountsService technicianAccountsService
    ) {
        this.technicianSkillsService = technicianSkillsService;
        this.technicianSkillsMapper = technicianSkillsMapper;
        this.serviceTypesService = serviceTypesService;
        this.serviceCategoriesService = serviceCategoriesService;
        this.technicianAccountsService = technicianAccountsService;
    }

    @Override
    public List<WorkerSkillItem> listCurrentWorkerSkills() {
        String accountId = requireWorker().getAccountId();
        requireTechnicianAccount(accountId);

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
            List<ServiceTypes> serviceTypes = serviceTypesService.list(
                new LambdaQueryWrapper<ServiceTypes>()
                    .in(ServiceTypes::getId, serviceTypeIds)
                    .eq(ServiceTypes::getIsDelete, 0)
            );
            for (ServiceTypes serviceType : serviceTypes) {
                if (serviceType == null || !StringUtils.hasText(serviceType.getId())) {
                    continue;
                }
                serviceTypeMap.put(serviceType.getId(), serviceType);
            }
        }

        Map<String, ServiceCategories> categoryMap = new LinkedHashMap<>();
        List<ServiceCategories> categories = serviceCategoriesService.list(
            new LambdaQueryWrapper<ServiceCategories>()
                .eq(ServiceCategories::getIsDelete, 0)
                .orderByAsc(ServiceCategories::getLevel)
                .orderByAsc(ServiceCategories::getSortOrder)
                .orderByAsc(ServiceCategories::getCreatedTime)
        );
        for (ServiceCategories category : categories) {
            if (category == null || !StringUtils.hasText(category.getId())) {
                continue;
            }
            categoryMap.put(category.getId(), category);
        }

        List<WorkerSkillItem> result = new ArrayList<>();
        for (TechnicianSkills skill : skills) {
            if (skill == null || !StringUtils.hasText(skill.getServiceTypeId())) {
                continue;
            }
            WorkerSkillItem item = new WorkerSkillItem();
            item.setId(skill.getId());
            item.setServiceTypeId(skill.getServiceTypeId());
            ServiceTypes serviceType = serviceTypeMap.get(skill.getServiceTypeId());
            item.setServiceTypeName(serviceType == null ? "未知服务" : serviceType.getName());
            item.setServiceMode(serviceType == null ? null : serviceType.getType());
            item.setServiceModeText(resolveServiceModeText(serviceType == null ? null : serviceType.getType()));
            item.setCategoryId(serviceType == null ? null : serviceType.getCategoryId());
            ServiceCategories category = serviceType == null ? null : categoryMap.get(serviceType.getCategoryId());
            item.setCategoryName(category == null ? null : category.getName());
            item.setCategoryPath(serviceType == null ? "" : buildCategoryPath(serviceType.getCategoryId(), categoryMap));
            Integer skillLevel = skill.getSkillLevel() == null ? 1 : skill.getSkillLevel();
            item.setSkillLevel(skillLevel);
            item.setSkillLevelText(resolveSkillLevelText(skillLevel));
            item.setIsActive(skill.getIsActive() != null && skill.getIsActive() == 0 ? 0 : 1);
            result.add(item);
        }
        return result;
    }

    @Override
    public List<WorkerSkillCategoryNode> listAvailableCategoryTree(String keyword, Integer serviceMode) {
        String accountId = requireWorker().getAccountId();
        requireTechnicianAccount(accountId);
        Integer mode = normalizeServiceMode(serviceMode);
        String normalizedKeyword = trimToNull(keyword);

        List<ServiceCategories> activeCategories = listActiveCategories();
        if (activeCategories.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, ServiceCategories> categoryMap = new LinkedHashMap<>();
        for (ServiceCategories category : activeCategories) {
            if (category == null || !StringUtils.hasText(category.getId())) {
                continue;
            }
            categoryMap.put(category.getId(), category);
        }

        List<ServiceTypes> availableServiceTypes = listAvailableServiceTypeEntities(accountId, normalizedKeyword, mode);
        if (availableServiceTypes.isEmpty()) {
            return new ArrayList<>();
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
            return new ArrayList<>();
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
        return tree;
    }

    @Override
    public List<WorkerSkillServiceTypeOption> listAvailableServiceTypes(String keyword, Integer serviceMode, String categoryId) {
        String accountId = requireWorker().getAccountId();
        requireTechnicianAccount(accountId);

        Integer mode = normalizeServiceMode(serviceMode);
        String normalizedKeyword = trimToNull(keyword);
        String normalizedCategoryId = trimToNull(categoryId);

        List<ServiceTypes> availableServiceTypes = listAvailableServiceTypeEntities(accountId, normalizedKeyword, mode);
        if (availableServiceTypes.isEmpty()) {
            return new ArrayList<>();
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
                return new ArrayList<>();
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
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addCurrentWorkerSkill(WorkerSkillCreateRequest request) {
        String accountId = requireWorker().getAccountId();
        requireTechnicianAccount(accountId);
        addSkillInternal(accountId, normalizeServiceTypeId(request == null ? null : request.getServiceTypeId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAddCurrentWorkerSkills(WorkerSkillBatchCreateRequest request) {
        String accountId = requireWorker().getAccountId();
        requireTechnicianAccount(accountId);
        if (request == null || request.getServiceTypeIds() == null || request.getServiceTypeIds().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择要添加的技能");
        }

        LinkedHashSet<String> uniqueServiceTypeIds = new LinkedHashSet<>();
        for (String serviceTypeId : request.getServiceTypeIds()) {
            uniqueServiceTypeIds.add(normalizeServiceTypeId(serviceTypeId));
        }

        for (String serviceTypeId : uniqueServiceTypeIds) {
            addSkillInternal(accountId, serviceTypeId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCurrentWorkerSkill(WorkerSkillDeleteRequest request) {
        String accountId = requireWorker().getAccountId();
        requireTechnicianAccount(accountId);
        String serviceTypeId = normalizeServiceTypeId(request == null ? null : request.getServiceTypeId());
        technicianSkillsMapper.logicalDeleteByTechnicianAndServiceType(accountId, serviceTypeId, System.currentTimeMillis());
    }

    private void addSkillInternal(String accountId, String serviceTypeId) {
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

    private TechnicianAccounts requireTechnicianAccount(String accountId) {
        TechnicianAccounts technician = technicianAccountsService.getById(accountId);
        if (technician == null || (technician.getIsDelete() != null && technician.getIsDelete() != 0)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "师傅账号不存在");
        }
        return technician;
    }

    private LoginUserInfo requireWorker() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.WORKER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅允许师傅访问");
        }
        return user;
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
}
