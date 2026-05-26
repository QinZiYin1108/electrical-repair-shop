package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.FaultPhenomena;
import com.example.backend.entity.Images;
import com.example.backend.entity.PaymentRecords;
import com.example.backend.entity.RepairOrderFaults;
import com.example.backend.entity.RepairOrderPayments;
import com.example.backend.entity.RepairOrders;
import com.example.backend.entity.ServiceCategories;
import com.example.backend.entity.ServiceTypes;
import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.service.UserFollowTechniciansService;
import com.example.backend.service.TechnicianProfilesService;
import com.example.backend.entity.UserFollowTechnicians;
import com.example.backend.entity.TechnicianProfiles;
import com.example.backend.entity.TechnicianServiceAreas;
import com.example.backend.entity.TechnicianSkills;
import com.example.backend.entity.TechnicianVisitFeePolicies;
import com.example.backend.entity.TechnicianWorkTimes;
import com.example.backend.entity.UserAddresses;
import com.example.backend.entity.Videos;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.user.UserOrderFlowModel;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.FaultPhenomenaService;
import com.example.backend.service.ImagesService;
import com.example.backend.service.PaymentRecordsService;
import com.example.backend.service.RepairOrderFaultsService;
import com.example.backend.service.RepairOrderPaymentsService;
import com.example.backend.service.RepairOrdersService;
import com.example.backend.service.RepairOrderFundService;
import com.example.backend.service.ReviewsService;
import com.example.backend.service.ServiceCategoriesService;
import com.example.backend.service.ServiceTypesService;
import com.example.backend.service.SystemConfigsService;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.service.TechnicianServiceAreasService;
import com.example.backend.service.TechnicianSkillsService;
import com.example.backend.service.TechnicianVisitFeePoliciesService;
import com.example.backend.service.TechnicianWorkTimesService;
import com.example.backend.service.UserAddressesService;
import com.example.backend.service.UserOrderFlowService;
import com.example.backend.service.VideosService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import com.example.backend.utils.oss.OssUtil;
import com.example.backend.utils.upload.UploadLimitUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Time;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class UserOrderFlowServiceImpl implements UserOrderFlowService {

    private static final int SERVICE_MODE_ONSITE_REPAIR = 1;
    private static final int SERVICE_MODE_ONSITE_INSTALL = 2;
    private static final int SERVICE_MODE_OFFLINE_REPAIR = 3;
    private static final int TECHNICIAN_ACCOUNT_ACTIVE = 1;
    private static final int TECHNICIAN_WORK_ONLINE = 1;
    private static final int TECHNICIAN_WORK_BUSY = 2;
    private static final int ORDER_STATUS_PENDING = 1;
    private static final int ORDER_STATUS_ACCEPTED = 2;
    private static final int ORDER_STATUS_ON_THE_WAY = 3;
    private static final int ORDER_STATUS_IN_SERVICE = 4;
    private static final int ORDER_STATUS_WAITING_PAY = 5;
    private static final int ORDER_STATUS_COMPLETED = 6;
    private static final int DEFAULT_APPOINTMENT_DAYS = 7;
    private static final int DEFAULT_MAX_APPOINTMENT_DAYS = 30;
    private static final int DEFAULT_MIN_APPOINTMENT_LEAD_MINUTES = 60;
    private static final int PAYMENT_METHOD_WECHAT = 1;
    private static final int PAYMENT_METHOD_ALIPAY = 2;
    private static final int PAYMENT_METHOD_WALLET = 5;
    private static final int PAYMENT_RECORD_STATUS_SUCCESS = 3;
    private static final BigDecimal BIG_DECIMAL_ZERO = BigDecimal.ZERO;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_SHORT_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final String AVATAR_BUSINESS_TYPE = "AVATAR";
    private static final String SERVICE_CATEGORY_ICON_BUSINESS_TYPE = "SERVERCATEGORY";

    private final ServiceCategoriesService serviceCategoriesService;
    private final ServiceTypesService serviceTypesService;
    private final FaultPhenomenaService faultPhenomenaService;
    private final UserAddressesService userAddressesService;
    private final TechnicianAccountsService technicianAccountsService;
    private final TechnicianProfilesService technicianProfilesService;
    private final UserFollowTechniciansService userFollowTechniciansService;
    private final TechnicianSkillsService technicianSkillsService;
    private final TechnicianServiceAreasService technicianServiceAreasService;
    private final TechnicianVisitFeePoliciesService technicianVisitFeePoliciesService;
    private final TechnicianWorkTimesService technicianWorkTimesService;
    private final RepairOrdersService repairOrdersService;
    private final RepairOrderPaymentsService repairOrderPaymentsService;
    private final PaymentRecordsService paymentRecordsService;
    private final RepairOrderFaultsService repairOrderFaultsService;
    private final ImagesService imagesService;
    private final VideosService videosService;
    private final RepairOrderFundService repairOrderFundService;
    private final ReviewsService reviewsService;
    private final OssUtil ossUtil;
    private final SystemConfigsService systemConfigsService;

    public UserOrderFlowServiceImpl(
        ServiceCategoriesService serviceCategoriesService,
        ServiceTypesService serviceTypesService,
        FaultPhenomenaService faultPhenomenaService,
        UserAddressesService userAddressesService,
        TechnicianAccountsService technicianAccountsService,
        TechnicianProfilesService technicianProfilesService,
        UserFollowTechniciansService userFollowTechniciansService,
        TechnicianSkillsService technicianSkillsService,
        TechnicianServiceAreasService technicianServiceAreasService,
        TechnicianVisitFeePoliciesService technicianVisitFeePoliciesService,
        TechnicianWorkTimesService technicianWorkTimesService,
        RepairOrdersService repairOrdersService,
        RepairOrderPaymentsService repairOrderPaymentsService,
        PaymentRecordsService paymentRecordsService,
        RepairOrderFaultsService repairOrderFaultsService,
        ImagesService imagesService,
        VideosService videosService,
        RepairOrderFundService repairOrderFundService,
        ReviewsService reviewsService,
        OssUtil ossUtil,
        SystemConfigsService systemConfigsService
    ) {
        this.serviceCategoriesService = serviceCategoriesService;
        this.serviceTypesService = serviceTypesService;
        this.faultPhenomenaService = faultPhenomenaService;
        this.userAddressesService = userAddressesService;
        this.technicianAccountsService = technicianAccountsService;
        this.technicianProfilesService = technicianProfilesService;
        this.userFollowTechniciansService = userFollowTechniciansService;
        this.technicianSkillsService = technicianSkillsService;
        this.technicianServiceAreasService = technicianServiceAreasService;
        this.technicianVisitFeePoliciesService = technicianVisitFeePoliciesService;
        this.technicianWorkTimesService = technicianWorkTimesService;
        this.repairOrdersService = repairOrdersService;
        this.repairOrderPaymentsService = repairOrderPaymentsService;
        this.paymentRecordsService = paymentRecordsService;
        this.repairOrderFaultsService = repairOrderFaultsService;
        this.imagesService = imagesService;
        this.videosService = videosService;
        this.repairOrderFundService = repairOrderFundService;
        this.reviewsService = reviewsService;
        this.ossUtil = ossUtil;
        this.systemConfigsService = systemConfigsService;
    }

    @Override
    public List<UserOrderFlowModel.ServiceModeItem> listServiceModes() {
        List<UserOrderFlowModel.ServiceModeItem> list = new ArrayList<>();
        list.add(buildServiceModeItem(
            SERVICE_MODE_ONSITE_REPAIR,
            "上门维修",
            "工程师上门检测并维修，适合需要现场处理的故障场景"
        ));
        list.add(buildServiceModeItem(
            SERVICE_MODE_ONSITE_INSTALL,
            "上门安装",
            "工程师上门安装和调试，适合新设备安装、拆旧换新等场景"
        ));
        list.add(buildServiceModeItem(
            SERVICE_MODE_OFFLINE_REPAIR,
            "到店维修",
            "将设备送到门店检测维修，适合便于携带或需要深度检修的设备"
        ));
        return list;
    }

    @Override
    public List<UserOrderFlowModel.CategoryNode> listCategoryTree(String keyword) {
        String normalizedKeyword = trimToNull(keyword);
        List<ServiceCategories> level1List = serviceCategoriesService.list(
            new LambdaQueryWrapper<ServiceCategories>()
                .eq(ServiceCategories::getLevel, 1)
                .eq(ServiceCategories::getIsActive, 1)
                .eq(ServiceCategories::getIsDelete, 0)
                .like(StringUtils.hasText(normalizedKeyword), ServiceCategories::getName, normalizedKeyword)
                .orderByAsc(ServiceCategories::getSortOrder)
                .orderByAsc(ServiceCategories::getCreatedTime)
        );
        if (level1List.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> level1Ids = level1List.stream().map(ServiceCategories::getId).collect(Collectors.toList());
        List<ServiceCategories> level2List = serviceCategoriesService.list(
            new LambdaQueryWrapper<ServiceCategories>()
                .eq(ServiceCategories::getLevel, 2)
                .eq(ServiceCategories::getIsActive, 1)
                .eq(ServiceCategories::getIsDelete, 0)
                .in(ServiceCategories::getParentId, level1Ids)
                .orderByAsc(ServiceCategories::getSortOrder)
                .orderByAsc(ServiceCategories::getCreatedTime)
        );

        List<String> level2Ids = level2List.stream().map(ServiceCategories::getId).collect(Collectors.toList());
        List<ServiceCategories> level3List = level2Ids.isEmpty()
            ? Collections.emptyList()
            : serviceCategoriesService.list(
                new LambdaQueryWrapper<ServiceCategories>()
                    .eq(ServiceCategories::getLevel, 3)
                    .eq(ServiceCategories::getIsActive, 1)
                    .eq(ServiceCategories::getIsDelete, 0)
                    .in(ServiceCategories::getParentId, level2Ids)
                    .orderByAsc(ServiceCategories::getSortOrder)
                    .orderByAsc(ServiceCategories::getCreatedTime)
            );

        Map<String, List<ServiceCategories>> level2ByParent = groupByParentId(level2List);
        Map<String, List<ServiceCategories>> level3ByParent = groupByParentId(level3List);

        List<UserOrderFlowModel.CategoryNode> tree = new ArrayList<>();
        for (ServiceCategories level1 : level1List) {
            UserOrderFlowModel.CategoryNode level1Node = toCategoryNode(level1);
            List<ServiceCategories> level2Children = level2ByParent.getOrDefault(level1.getId(), Collections.emptyList());
            for (ServiceCategories level2 : level2Children) {
                UserOrderFlowModel.CategoryNode level2Node = toCategoryNode(level2);
                List<ServiceCategories> level3Children = level3ByParent.getOrDefault(level2.getId(), Collections.emptyList());
                for (ServiceCategories level3 : level3Children) {
                    level2Node.getChildren().add(toCategoryNode(level3));
                }
                level1Node.getChildren().add(level2Node);
            }
            tree.add(level1Node);
        }
        return tree;
    }

    @Override
    public UserOrderFlowModel.CategoryDetailResponse getCategoryDetail(String categoryId) {
        String normalizedCategoryId = trimToNull(categoryId);
        if (!StringUtils.hasText(normalizedCategoryId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "categoryId 不能为空");
        }

        List<ServiceCategories> activeCategories = listActiveServiceCategories();
        if (activeCategories.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "服务分类不存在");
        }
        Map<String, ServiceCategories> categoryMap = activeCategories.stream()
            .collect(Collectors.toMap(ServiceCategories::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
        ServiceCategories category = categoryMap.get(normalizedCategoryId);
        if (category == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "服务分类不存在");
        }

        UserOrderFlowModel.CategoryDetailResponse response = new UserOrderFlowModel.CategoryDetailResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setCode(category.getCode());
        response.setDescription(trimToNull(category.getDescription()));
        response.setLevel(category.getLevel());
        response.setParentId(category.getParentId());

        ServiceCategories parent = StringUtils.hasText(category.getParentId()) ? categoryMap.get(category.getParentId()) : null;
        response.setParentName(parent == null ? null : parent.getName());
        response.setPathText(buildCategoryPath(category, categoryMap));
        response.setIconUrl(loadLatestImageUrlMap(Collections.singletonList(category.getId()), SERVICE_CATEGORY_ICON_BUSINESS_TYPE)
            .get(category.getId()));
        fillCategoryLevelInfo(response, category, categoryMap);
        return response;
    }

    @Override
    public List<UserOrderFlowModel.ServiceTypeItem> listServiceTypes(Integer serviceMode, String categoryId) {
        int mode = normalizeServiceMode(serviceMode);
        String normalizedCategoryId = trimToNull(categoryId);
        if (!StringUtils.hasText(normalizedCategoryId)) {
            return Collections.emptyList();
        }
        List<ServiceCategories> activeCategories = listActiveServiceCategories();
        Set<String> filterCategoryIds = resolveApplicableServiceTypeCategoryIds(normalizedCategoryId, activeCategories);
        if (filterCategoryIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<ServiceTypes> list = serviceTypesService.list(
            new LambdaQueryWrapper<ServiceTypes>()
                .eq(ServiceTypes::getType, mode)
                .in(ServiceTypes::getCategoryId, filterCategoryIds)
                .eq(ServiceTypes::getIsActive, 1)
                .eq(ServiceTypes::getIsDelete, 0)
                .orderByAsc(ServiceTypes::getSortOrder)
                .orderByAsc(ServiceTypes::getCreatedTime)
        );
        List<UserOrderFlowModel.ServiceTypeItem> response = new ArrayList<>();
        for (ServiceTypes item : list) {
            UserOrderFlowModel.ServiceTypeItem row = new UserOrderFlowModel.ServiceTypeItem();
            row.setId(item.getId());
            row.setName(item.getName());
            row.setType(item.getType());
            row.setCategoryId(item.getCategoryId());
            row.setBasePrice(formatMoney(item.getBasePrice()));
            response.add(row);
        }
        return response;
    }

    @Override
    public UserOrderFlowModel.SelectionContextResponse getSelectionContext(Integer serviceMode, String serviceTypeId, String addressId) {
        LoginUserInfo user = requireUser();
        int mode = normalizeServiceMode(serviceMode);
        ServiceTypes serviceType = requireServiceTypeByMode(serviceTypeId, mode);
        String accountId = user.getAccountId();
        boolean onsiteMode = isOnsiteMode(mode);

        List<UserAddresses> userAddressList = listUserAddresses(accountId);
        Map<String, UserAddresses> addressMap = userAddressList.stream()
            .collect(Collectors.toMap(UserAddresses::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
        List<UserOrderFlowModel.AddressItem> addressItems = toAddressItems(userAddressList);

        String selectedAddressId = resolveSelectedAddressId(addressId, addressItems, onsiteMode);
        List<UserOrderFlowModel.TechnicianItem> technicians = listSelectableTechnicians(
            mode,
            serviceType.getId(),
            selectedAddressId,
            userAddressList
        );

        UserOrderFlowModel.SelectionContextResponse response = new UserOrderFlowModel.SelectionContextResponse();
        response.setServiceMode(mode);
        response.setServiceModeName(getServiceModeName(mode));
        response.setServiceTypeId(serviceType.getId());
        response.setServiceTypeName(serviceType.getName());
        response.setCategoryPath(buildCategoryPathText(serviceType.getCategoryId()));
        response.setShowAddressSection(onsiteMode);
        response.setSelectedAddressId(selectedAddressId);
        response.setAddresses(addressItems);
        response.setTechnicians(technicians);

        if (onsiteMode && StringUtils.hasText(selectedAddressId) && !addressMap.containsKey(selectedAddressId)) {
            response.setSelectedAddressId("");
            response.setTechnicians(Collections.emptyList());
        }
        return response;
    }

    @Override
    public List<UserOrderFlowModel.FaultOptionItem> listFaultOptions(String serviceTypeId) {
        String normalizedServiceTypeId = trimToNull(serviceTypeId);
        if (!StringUtils.hasText(normalizedServiceTypeId)) {
            return Collections.emptyList();
        }
        List<FaultPhenomena> list = faultPhenomenaService.list(
            new LambdaQueryWrapper<FaultPhenomena>()
                .eq(FaultPhenomena::getServiceTypeId, normalizedServiceTypeId)
                .eq(FaultPhenomena::getIsActive, 1)
                .eq(FaultPhenomena::getIsDelete, 0)
                .orderByAsc(FaultPhenomena::getSortOrder)
                .orderByAsc(FaultPhenomena::getCreatedTime)
        );
        List<UserOrderFlowModel.FaultOptionItem> response = new ArrayList<>();
        for (FaultPhenomena item : list) {
            UserOrderFlowModel.FaultOptionItem row = new UserOrderFlowModel.FaultOptionItem();
            row.setId(item.getId());
            row.setName(item.getName());
            response.add(row);
        }
        return response;
    }

        @Override
    public List<UserOrderFlowModel.TechnicianItem> listTechnicians(Integer serviceMode, String serviceTypeId, String addressId) {
        LoginUserInfo user = requireUser();
        int mode = normalizeServiceMode(serviceMode);
        ServiceTypes serviceType = requireServiceTypeByMode(serviceTypeId, mode);

        List<UserAddresses> userAddressList = listUserAddresses(user.getAccountId());
        String selectedAddressId = resolveSelectedAddressId(addressId, toAddressItems(userAddressList), isOnsiteMode(mode));
        List<UserOrderFlowModel.TechnicianItem> technicians = listSelectableTechnicians(
            mode,
            serviceType.getId(),
            selectedAddressId,
            userAddressList
        );
        fillTechnicianFollowAndAvatar(user.getAccountId(), technicians);
        return technicians;
    }

    @Override
    public UserOrderFlowModel.TechnicianBrowseResponse listAllTechnicians(String addressId) {
        LoginUserInfo user = requireUser();
        List<UserAddresses> userAddressList = listUserAddresses(user.getAccountId());
        List<UserOrderFlowModel.AddressItem> addressItems = toAddressItems(userAddressList);
        String selectedAddressId = resolveSelectedAddressId(addressId, addressItems, true);
        UserAddresses referenceAddress = findAddressById(userAddressList, selectedAddressId);
        if (referenceAddress == null && !userAddressList.isEmpty()) {
            referenceAddress = userAddressList.get(0);
            selectedAddressId = referenceAddress.getId();
        }

        List<UserOrderFlowModel.TechnicianItem> technicians = listBrowsableTechnicians(selectedAddressId, userAddressList);
        fillTechnicianFollowAndAvatar(user.getAccountId(), technicians);

        UserOrderFlowModel.TechnicianBrowseResponse response = new UserOrderFlowModel.TechnicianBrowseResponse();
        response.setReferenceAddressId(selectedAddressId);
        response.setReferenceAddressDetail(referenceAddress == null ? "" : buildAddressDetail(referenceAddress));
        response.setTechnicians(technicians);
        return response;
    }

    @Override
    public UserOrderFlowModel.TechnicianDetailResponse getTechnicianDetail(String technicianId) {
        LoginUserInfo user = requireUser();
        String normalizedId = trimToNull(technicianId);
        if (!StringUtils.hasText(normalizedId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "technicianId 不能为空");
        }
        TechnicianAccounts technician = technicianAccountsService.getOne(
            new LambdaQueryWrapper<TechnicianAccounts>()
                .eq(TechnicianAccounts::getId, normalizedId)
                .eq(TechnicianAccounts::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (technician == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "技师不存在");
        }
        TechnicianProfiles profile = technicianProfilesService.getOne(
            new LambdaQueryWrapper<TechnicianProfiles>()
                .eq(TechnicianProfiles::getTechnicianAccountId, technician.getId())
                .eq(TechnicianProfiles::getIsDelete, 0)
                .last("limit 1"),
            false
        );

        UserOrderFlowModel.TechnicianDetailResponse response = new UserOrderFlowModel.TechnicianDetailResponse();
        response.setId(technician.getId());
        response.setName(technician.getUsername());
        response.setRating(formatDecimal(defaultZero(technician.getRating()), 1));
        response.setOrderCount(technician.getOrderCount() == null ? 0 : technician.getOrderCount());
        response.setAccountStatus(technician.getAccountStatus());
        response.setWorkStatus(technician.getWorkStatus());
        response.setWorkStatusText(mapWorkStatusText(technician.getWorkStatus()));
        response.setWorkStatusType(mapWorkStatusType(technician.getWorkStatus()));
        response.setAvatarUrl(loadLatestImageUrlMap(Collections.singletonList(technician.getId()), AVATAR_BUSINESS_TYPE)
            .get(technician.getId()));
        response.setIsFollowed(isTechnicianFollowed(user.getAccountId(), technician.getId()));
        response.setCompletedOrderCount(repairOrdersService.count(
            new LambdaQueryWrapper<RepairOrders>()
                .eq(RepairOrders::getTechnicianAccountId, technician.getId())
                .eq(RepairOrders::getStatus, ORDER_STATUS_COMPLETED)
                .eq(RepairOrders::getIsDelete, 0)
        ));
        if (profile != null) {
            response.setWorkYears(profile.getWorkYears());
            response.setIntroduction(trimToNull(profile.getIntroduction()));
            response.setSpecialties(trimToNull(profile.getSpecialties()));
            response.setCertificates(trimToNull(profile.getCertificates()));
            response.setEducation(trimToNull(profile.getEducation()));
        }
        TechnicianServiceAreas serviceArea = getDefaultServiceArea(technician.getId());
        if (serviceArea != null) {
            response.setLocationAddress(trimToNull(serviceArea.getCenterAddress()));
            response.setLatitude(serviceArea.getCenterLatitude() == null ? null : serviceArea.getCenterLatitude().toPlainString());
            response.setLongitude(serviceArea.getCenterLongitude() == null ? null : serviceArea.getCenterLongitude().toPlainString());
        }
        response.setReviews(reviewsService.listPublicTechnicianReviews(technician.getId()));
        return response;
    }

    @Override
    public UserOrderFlowModel.FollowTechnicianResponse toggleTechnicianFollow(UserOrderFlowModel.FollowTechnicianRequest request) {
        LoginUserInfo user = requireUser();
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数不能为空");
        }
        String technicianId = trimToNull(request.getTechnicianId());
        if (!StringUtils.hasText(technicianId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "technicianId 不能为空");
        }
        TechnicianAccounts technician = technicianAccountsService.getOne(
            new LambdaQueryWrapper<TechnicianAccounts>()
                .eq(TechnicianAccounts::getId, technicianId)
                .eq(TechnicianAccounts::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (technician == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "技师不存在");
        }

        boolean desiredFollow = request.getFollow() == null ? true : Boolean.TRUE.equals(request.getFollow());
        long now = System.currentTimeMillis();
        UserFollowTechnicians follow = userFollowTechniciansService.getOne(
            new LambdaQueryWrapper<UserFollowTechnicians>()
                .eq(UserFollowTechnicians::getAccountId, user.getAccountId())
                .eq(UserFollowTechnicians::getTechnicianAccountId, technicianId)
                .last("limit 1"),
            false
        );

        boolean isFollowed;
        if (follow == null) {
            if (!desiredFollow) {
                isFollowed = false;
            } else {
                follow = new UserFollowTechnicians();
                follow.setId(SnowflakeIdUtil.nextUserFollowTechnicianId());
                follow.setAccountId(user.getAccountId());
                follow.setTechnicianAccountId(technicianId);
                follow.setCreatedTime(now);
                follow.setUpdatedTime(now);
                follow.setIsDelete(0);
                follow.setVersion(1);
                if (!userFollowTechniciansService.save(follow)) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "关注师傅失败");
                }
                isFollowed = true;
            }
        } else {
            boolean currentFollowed = follow.getIsDelete() == null || follow.getIsDelete() == 0;
            if (request.getFollow() == null) {
                isFollowed = !currentFollowed;
            } else {
                isFollowed = desiredFollow;
            }
            follow.setIsDelete(isFollowed ? 0 : 1);
            follow.setUpdatedTime(now);
            if (!userFollowTechniciansService.updateById(follow)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新关注状态失败");
            }
        }

        UserOrderFlowModel.FollowTechnicianResponse response = new UserOrderFlowModel.FollowTechnicianResponse();
        response.setTechnicianId(technicianId);
        response.setIsFollowed(isFollowed);
        return response;
    }

@Override
    public UserOrderFlowModel.AppointmentSlotsResponse listAppointmentSlots(
        Integer serviceMode,
        String serviceTypeId,
        String technicianId,
        String addressId,
        Integer days
    ) {
        LoginUserInfo user = requireUser();
        int mode = normalizeServiceMode(serviceMode);
        ServiceTypes serviceType = requireServiceTypeByMode(serviceTypeId, mode);
        TechnicianAccounts technician = requireSelectableTechnician(technicianId);
        ensureTechnicianSkill(technician.getId(), serviceType.getId());

        List<UserAddresses> userAddressList = listUserAddresses(user.getAccountId());
        String selectedAddressId = resolveSelectedAddressId(addressId, toAddressItems(userAddressList), isOnsiteMode(mode));
        UserAddresses selectedAddress = findAddressById(userAddressList, selectedAddressId);

        UserOrderFlowModel.AppointmentSlotsResponse response = new UserOrderFlowModel.AppointmentSlotsResponse();
        response.setServiceMode(mode);
        response.setServiceModeName(getServiceModeName(mode));
        response.setServiceTypeName(serviceType.getName());
        response.setTechnicianName(technician.getUsername());
        response.setMinLeadMinutes(getMinAppointmentLeadMinutes());
        response.setAddressDetail(
            selectedAddress == null
                ? (isOnsiteMode(mode) ? "" : "线下维修无需上门地址")
                : buildAddressDetail(selectedAddress)
        );

        int safeDays = resolveAppointmentDays(days);
        LocalDateTime now = LocalDateTime.now().plusMinutes(getMinAppointmentLeadMinutes()).withSecond(0).withNano(0);
        LocalDate startDate = now.toLocalDate();
        response.setBookingDays(safeDays);
        response.setBookingStartDate(startDate.format(DATE_FORMATTER));
        response.setBookingEndDate(startDate.plusDays(safeDays - 1L).format(DATE_FORMATTER));

        if (!isOnsiteMode(mode)) {
            response.setWorkWindows(Collections.emptyList());
            response.setAppointmentSlots(Collections.emptyList());
            return response;
        }

        List<TechnicianWorkTimes> workTimeList = listAvailableWorkTimes(technician.getId());
        Map<Integer, List<TechnicianWorkTimes>> dayToRecords = groupWorkTimesByDay(workTimeList);
        response.setWorkWindows(buildAppointmentWorkWindows(dayToRecords));
        response.setAppointmentSlots(buildSuggestedAppointmentSlots(dayToRecords, startDate, now, safeDays));
        return response;
    }

    @Override
    public UserOrderFlowModel.FeePreviewResponse getFeePreview(
        Integer serviceMode,
        String serviceTypeId,
        String technicianId,
        String addressId
    ) {
        LoginUserInfo user = requireUser();
        int mode = normalizeServiceMode(serviceMode);
        if (!isOnsiteMode(mode)) {
            return null;
        }
        ServiceTypes serviceType = requireServiceTypeByMode(serviceTypeId, mode);
        TechnicianAccounts technician = requireSelectableTechnician(technicianId);
        ensureTechnicianSkill(technician.getId(), serviceType.getId());
        UserAddresses address = requireAddress(user.getAccountId(), addressId);

        FeeCalcResult fee = calculateFee(mode, technician.getId(), address);
        UserOrderFlowModel.FeePreviewResponse response = new UserOrderFlowModel.FeePreviewResponse();
        response.setDistanceKm(formatDecimal(fee.distanceKm, 1));
        response.setDoorFee(formatMoney(fee.doorFee));
        response.setDistanceFee(formatMoney(fee.distanceFee));
        response.setTotalAmount(formatMoney(fee.totalAmount));
        response.setFormula(buildFeeFormula(fee));
        return response;
    }

    @Override
    public UserOrderFlowModel.UploadMediaResponse uploadFaultMedia(String mediaType, MultipartFile file) {
        LoginUserInfo user = requireUser();
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }

        String mediaKind = resolveUploadMediaType(mediaType, file.getContentType(), file.getOriginalFilename());
        UploadLimitUtil.validateMediaSize(mediaKind, file);
        String accountId = user.getAccountId();
        String originalFilename = trimToNull(file.getOriginalFilename());
        String extension = resolveUploadExtension(originalFilename, file.getContentType(), mediaKind);
        String objectName = "repair-orders/" + accountId + "/" + mediaKind + "/" + UUID.randomUUID() + extension;

        String uploadUrl;
        try (InputStream in = file.getInputStream()) {
            uploadUrl = ossUtil.upload(objectName, in);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传文件失败");
        }

        UserOrderFlowModel.UploadMediaResponse response = new UserOrderFlowModel.UploadMediaResponse();
        response.setUrl(uploadUrl);
        response.setName(StringUtils.hasText(originalFilename) ? originalFilename : objectName);
        response.setFileSize(file.getSize());
        response.setMimeType(resolveUploadMimeType(file.getContentType(), mediaKind));

        if ("image".equals(mediaKind)) {
            try (InputStream in = file.getInputStream()) {
                BufferedImage bufferedImage = ImageIO.read(in);
                if (bufferedImage != null) {
                    response.setWidth(bufferedImage.getWidth());
                    response.setHeight(bufferedImage.getHeight());
                }
            } catch (Exception ignored) {
            }
        }
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserOrderFlowModel.SubmitResponse submitOrder(UserOrderFlowModel.SubmitRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数不能为空");
        }
        LoginUserInfo user = requireUser();
        String accountId = user.getAccountId();
        int mode = normalizeServiceMode(request.getServiceMode());
        ServiceTypes serviceType = requireServiceTypeByMode(request.getServiceTypeId(), mode);
        TechnicianAccounts technician = requireSelectableTechnician(request.getTechnicianId());
        ensureTechnicianSkill(technician.getId(), serviceType.getId());

        String requestCategoryId = trimToNull(request.getCategoryId());
        if (StringUtils.hasText(requestCategoryId)) {
            List<ServiceCategories> activeCategories = listActiveServiceCategories();
            if (!isServiceTypeApplicableToCategory(serviceType, requestCategoryId, activeCategories)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "所选分类不匹配当前服务类型");
            }
        }

        List<UserAddresses> userAddressList = listUserAddresses(accountId);
        UserAddresses selectedAddress = resolveSubmitAddress(mode, request.getServiceAddressId(), userAddressList);
        Long appointmentTime = resolveSubmitAppointmentTime(mode, request.getAppointmentTime(), technician.getId());

        FeeCalcResult fee = isOnsiteMode(mode)
            ? calculateFee(mode, technician.getId(), selectedAddress)
            : FeeCalcResult.zero();

        Integer paymentMethod = isOnsiteMode(mode) ? normalizePaymentMethod(request.getPaymentMethod()) : null;
        long now = System.currentTimeMillis();
        String orderId = SnowflakeIdUtil.nextRepairOrderId();
        String orderNo = buildOrderNo(orderId);
        boolean needPrepay = isOnsiteMode(mode) && fee.totalAmount.compareTo(BIG_DECIMAL_ZERO) > 0;
        Integer paymentStatus = needPrepay ? 2 : 1;
        if (isOnsiteMode(mode) && fee.totalAmount.compareTo(BIG_DECIMAL_ZERO) <= 0) {
            paymentStatus = 2;
        }

        RepairOrders order = new RepairOrders();
        order.setId(orderId);
        order.setOrderNo(orderNo);
        order.setAccountId(accountId);
        order.setTechnicianAccountId(technician.getId());
        order.setServiceTypeId(serviceType.getId());
        order.setApplianceBrand(trimToNull(request.getApplianceBrand()));
        order.setApplianceModel(trimToNull(request.getApplianceModel()));
        order.setPurchaseDate(parsePurchaseDate(request.getPurchaseDate()));
        order.setServiceAddressId(selectedAddress == null ? null : selectedAddress.getId());
        order.setAppointmentTime(appointmentTime);
        order.setStatus(1);
        order.setPaymentStatus(paymentStatus);
        order.setCreatedTime(now);
        order.setUpdatedTime(now);
        order.setIsDelete(0);
        if (!repairOrdersService.save(order)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "閸掓稑缂撶紒缈犳叏鐠併垹宕熸径杈Е");
        }

        RepairOrderPayments payment = new RepairOrderPayments();
        payment.setId(SnowflakeIdUtil.nextRepairOrderPaymentId());
        payment.setRepairOrderId(orderId);
        payment.setDoorFee(fee.doorFee);
        payment.setDistanceFee(fee.distanceFee);
        payment.setServiceDistanceKm(fee.distanceKm);
        payment.setBaseRadiusKmSnapshot(fee.baseRadiusKm);
        payment.setDistanceOverKm(fee.distanceOverKm);
        payment.setMinVisitFeeSnapshot(fee.minVisitFeeSnapshot);
        payment.setExtraFeePerKmSnapshot(fee.extraFeePerKmSnapshot);
        payment.setDistanceCalcTypeSnapshot(fee.distanceCalcTypeSnapshot);
        payment.setRoundingRuleSnapshot(fee.roundingRuleSnapshot);
        payment.setPricingLockedTime(now);
        payment.setFeeRuleSnapshot(buildFeeRuleSnapshot(fee));
        payment.setServiceFee(BIG_DECIMAL_ZERO);
        payment.setMaterialFee(BIG_DECIMAL_ZERO);
        payment.setOvertimeFee(BIG_DECIMAL_ZERO);
        payment.setTotalAmount(fee.totalAmount);
        payment.setDiscountAmount(BIG_DECIMAL_ZERO);
        payment.setActualAmount(fee.totalAmount);
        payment.setCouponId(trimToNull(request.getCouponId()));
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentTime(needPrepay ? now : null);
        payment.setCreatedTime(now);
        payment.setUpdatedTime(now);
        payment.setIsDelete(0);
        if (!repairOrderPaymentsService.save(payment)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存订单支付信息失败");
        }

        if (needPrepay) {
            createPaymentRecord(orderId, orderNo, accountId, paymentMethod, fee.totalAmount, now);
        }

        saveFaultDetails(orderId, serviceType.getId(), accountId, request.getFaultList(), now);

        if (needPrepay) {
            repairOrderFundService.recordOrderPrepay(
                accountId,
                technician.getId(),
                orderId,
                orderNo,
                paymentMethod,
                fee.totalAmount,
                now
            );
        }

        UserOrderFlowModel.SubmitResponse response = new UserOrderFlowModel.SubmitResponse();
        response.setOrderId(orderId);
        response.setOrderNo(orderNo);
        response.setPaymentStatus(paymentStatus);
        response.setTotalAmount(formatMoney(fee.totalAmount));
        response.setPaidAmount(needPrepay ? formatMoney(fee.totalAmount) : formatMoney(BIG_DECIMAL_ZERO));
        return response;
    }

    @Override
    public void validateAppointmentTime(String technicianId, Long appointmentTimeMillis, String excludeOrderId) {
        validateAppointmentTimeInternal(technicianId, appointmentTimeMillis, excludeOrderId);
    }

    private UserOrderFlowModel.ServiceModeItem buildServiceModeItem(Integer id, String name, String desc) {
        UserOrderFlowModel.ServiceModeItem item = new UserOrderFlowModel.ServiceModeItem();
        item.setId(id);
        item.setName(name);
        item.setDesc(desc);
        return item;
    }

    private String getServiceModeName(int serviceMode) {
        if (serviceMode == SERVICE_MODE_ONSITE_REPAIR) {
            return "上门维修";
        }
        if (serviceMode == SERVICE_MODE_ONSITE_INSTALL) {
            return "上门安装";
        }
        if (serviceMode == SERVICE_MODE_OFFLINE_REPAIR) {
            return "到店维修";
        }
        return "";
    }

    private int normalizeServiceMode(Integer serviceMode) {
        if (serviceMode == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "serviceMode 不能为空");
        }
        if (serviceMode != SERVICE_MODE_ONSITE_REPAIR
            && serviceMode != SERVICE_MODE_ONSITE_INSTALL
            && serviceMode != SERVICE_MODE_OFFLINE_REPAIR) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "serviceMode 仅支持 1、2、3");
        }
        return serviceMode;
    }

    private boolean isOnsiteMode(int serviceMode) {
        return serviceMode == SERVICE_MODE_ONSITE_REPAIR || serviceMode == SERVICE_MODE_ONSITE_INSTALL;
    }

    private ServiceTypes requireServiceTypeByMode(String serviceTypeId, int serviceMode) {
        String normalizedServiceTypeId = trimToNull(serviceTypeId);
        if (!StringUtils.hasText(normalizedServiceTypeId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "serviceTypeId 不能为空");
        }
        ServiceTypes serviceType = serviceTypesService.getOne(
            new LambdaQueryWrapper<ServiceTypes>()
                .eq(ServiceTypes::getId, normalizedServiceTypeId)
                .eq(ServiceTypes::getIsActive, 1)
                .eq(ServiceTypes::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (serviceType == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "服务类型不存在");
        }
        if (serviceType.getType() == null || serviceType.getType() != serviceMode) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "服务类型与服务方式不匹配");
        }
        return serviceType;
    }

    private TechnicianAccounts requireSelectableTechnician(String technicianId) {
        String normalizedTechnicianId = trimToNull(technicianId);
        if (!StringUtils.hasText(normalizedTechnicianId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "technicianId 不能为空");
        }
        TechnicianAccounts technician = technicianAccountsService.getOne(
            new LambdaQueryWrapper<TechnicianAccounts>()
                .eq(TechnicianAccounts::getId, normalizedTechnicianId)
                .eq(TechnicianAccounts::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (technician == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "技师不存在");
        }
        if (technician.getAccountStatus() == null || technician.getAccountStatus() != TECHNICIAN_ACCOUNT_ACTIVE) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "技师账号不可接单");
        }
        Integer workStatus = technician.getWorkStatus();
        if (workStatus == null || (workStatus != TECHNICIAN_WORK_ONLINE && workStatus != TECHNICIAN_WORK_BUSY)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "技师当前不可接单");
        }
        return technician;
    }

    private void ensureTechnicianSkill(String technicianId, String serviceTypeId) {
        TechnicianSkills skill = technicianSkillsService.getOne(
            new LambdaQueryWrapper<TechnicianSkills>()
                .eq(TechnicianSkills::getTechnicianAccountId, technicianId)
                .eq(TechnicianSkills::getServiceTypeId, serviceTypeId)
                .eq(TechnicianSkills::getIsActive, 1)
                .eq(TechnicianSkills::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (skill == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "师傅不支持当前服务");
        }
    }

    private Map<String, List<ServiceCategories>> groupByParentId(List<ServiceCategories> list) {
        Map<String, List<ServiceCategories>> map = new HashMap<>();
        for (ServiceCategories item : list) {
            if (!StringUtils.hasText(item.getParentId())) {
                continue;
            }
            map.computeIfAbsent(item.getParentId(), key -> new ArrayList<>()).add(item);
        }
        for (Map.Entry<String, List<ServiceCategories>> entry : map.entrySet()) {
            entry.getValue().sort(Comparator
                .comparing((ServiceCategories value) -> value.getSortOrder() == null ? Integer.MAX_VALUE : value.getSortOrder())
                .thenComparing(value -> value.getCreatedTime() == null ? Long.MAX_VALUE : value.getCreatedTime()));
        }
        return map;
    }

    private UserOrderFlowModel.CategoryNode toCategoryNode(ServiceCategories entity) {
        UserOrderFlowModel.CategoryNode node = new UserOrderFlowModel.CategoryNode();
        node.setId(entity.getId());
        node.setName(entity.getName());
        node.setLevel(entity.getLevel());
        node.setParentId(entity.getParentId());
        return node;
    }

    private void fillCategoryLevelInfo(
        UserOrderFlowModel.CategoryDetailResponse response,
        ServiceCategories category,
        Map<String, ServiceCategories> categoryMap
    ) {
        if (response == null || category == null || categoryMap == null || categoryMap.isEmpty()) {
            return;
        }
        List<ServiceCategories> chain = new ArrayList<>();
        ServiceCategories current = category;
        while (current != null) {
            chain.add(current);
            if (!StringUtils.hasText(current.getParentId())) {
                break;
            }
            current = categoryMap.get(current.getParentId());
        }
        Collections.reverse(chain);
        for (ServiceCategories item : chain) {
            if (item == null || item.getLevel() == null) {
                continue;
            }
            if (item.getLevel() == 1) {
                response.setLevel1Id(item.getId());
                response.setLevel1Name(item.getName());
            } else if (item.getLevel() == 2) {
                response.setLevel2Id(item.getId());
                response.setLevel2Name(item.getName());
            } else if (item.getLevel() == 3) {
                response.setLevel3Id(item.getId());
                response.setLevel3Name(item.getName());
            }
        }
    }

    private String buildCategoryPath(ServiceCategories category, Map<String, ServiceCategories> categoryMap) {
        if (category == null) {
            return "";
        }
        List<String> names = new ArrayList<>();
        ServiceCategories current = category;
        while (current != null) {
            if (StringUtils.hasText(current.getName())) {
                names.add(current.getName());
            }
            if (!StringUtils.hasText(current.getParentId())) {
                break;
            }
            current = categoryMap.get(current.getParentId());
        }
        Collections.reverse(names);
        return String.join(" / ", names);
    }

    private List<UserAddresses> listUserAddresses(String accountId) {
        return userAddressesService.list(
            new LambdaQueryWrapper<UserAddresses>()
                .eq(UserAddresses::getAccountId, accountId)
                .eq(UserAddresses::getIsDelete, 0)
                .orderByDesc(UserAddresses::getIsDefault)
                .orderByDesc(UserAddresses::getUpdatedTime)
                .orderByDesc(UserAddresses::getCreatedTime)
        );
    }

    private List<UserOrderFlowModel.AddressItem> toAddressItems(List<UserAddresses> userAddressList) {
        List<UserOrderFlowModel.AddressItem> result = new ArrayList<>();
        for (UserAddresses address : userAddressList) {
            UserOrderFlowModel.AddressItem row = new UserOrderFlowModel.AddressItem();
            row.setId(address.getId());
            row.setLabel((safeString(address.getContactName()) + " " + safeString(address.getContactPhone())).trim());
            row.setDetail(buildAddressDetail(address));
            row.setIsDefault(address.getIsDefault() != null && address.getIsDefault() == 1 ? 1 : 0);
            row.setLatitude(address.getLatitude() == null ? null : address.getLatitude().toPlainString());
            row.setLongitude(address.getLongitude() == null ? null : address.getLongitude().toPlainString());
            result.add(row);
        }
        return result;
    }

    private String buildAddressDetail(UserAddresses address) {
        if (address == null) {
            return "";
        }
        return safeString(address.getProvince())
            + safeString(address.getCity())
            + safeString(address.getDistrict())
            + safeString(address.getStreet())
            + safeString(address.getDetailedAddress());
    }

    private String resolveSelectedAddressId(String addressId, List<UserOrderFlowModel.AddressItem> addresses, boolean onsiteMode) {
        if (!onsiteMode) {
            return "";
        }
        String normalizedAddressId = trimToNull(addressId);
        if (StringUtils.hasText(normalizedAddressId)) {
            boolean exists = addresses.stream().anyMatch(item -> normalizedAddressId.equals(item.getId()));
            if (exists) {
                return normalizedAddressId;
            }
        }
        UserOrderFlowModel.AddressItem defaultAddress = addresses.stream()
            .filter(item -> item.getIsDefault() != null && item.getIsDefault() == 1)
            .findFirst()
            .orElse(null);
        if (defaultAddress != null) {
            return defaultAddress.getId();
        }
        return addresses.isEmpty() ? "" : addresses.get(0).getId();
    }

    private List<UserOrderFlowModel.TechnicianItem> listSelectableTechnicians(
        int serviceMode,
        String serviceTypeId,
        String selectedAddressId,
        List<UserAddresses> userAddressList
    ) {
        List<TechnicianSkills> skillList = technicianSkillsService.list(
            new LambdaQueryWrapper<TechnicianSkills>()
                .eq(TechnicianSkills::getServiceTypeId, serviceTypeId)
                .eq(TechnicianSkills::getIsActive, 1)
                .eq(TechnicianSkills::getIsDelete, 0)
                .orderByDesc(TechnicianSkills::getUpdatedTime)
                .orderByDesc(TechnicianSkills::getCreatedTime)
        );
        if (skillList.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> technicianIdSet = skillList.stream()
            .map(TechnicianSkills::getTechnicianAccountId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        if (technicianIdSet.isEmpty()) {
            return Collections.emptyList();
        }

        List<TechnicianAccounts> technicianList = technicianAccountsService.list(
            new LambdaQueryWrapper<TechnicianAccounts>()
                .in(TechnicianAccounts::getId, technicianIdSet)
                .eq(TechnicianAccounts::getAccountStatus, TECHNICIAN_ACCOUNT_ACTIVE)
                .in(TechnicianAccounts::getWorkStatus, TECHNICIAN_WORK_ONLINE, TECHNICIAN_WORK_BUSY)
                .eq(TechnicianAccounts::getIsDelete, 0)
        );
        if (technicianList.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> validTechnicianIds = technicianList.stream().map(TechnicianAccounts::getId).collect(Collectors.toSet());

        List<TechnicianServiceAreas> areaList = technicianServiceAreasService.list(
            new LambdaQueryWrapper<TechnicianServiceAreas>()
                .in(TechnicianServiceAreas::getTechnicianAccountId, validTechnicianIds)
                .eq(TechnicianServiceAreas::getIsActive, 1)
                .eq(TechnicianServiceAreas::getIsDelete, 0)
                .orderByDesc(TechnicianServiceAreas::getIsDefault)
                .orderByDesc(TechnicianServiceAreas::getUpdatedTime)
                .orderByDesc(TechnicianServiceAreas::getCreatedTime)
        );
        Map<String, TechnicianServiceAreas> defaultAreaMap = new HashMap<>();
        for (TechnicianServiceAreas area : areaList) {
            if (!defaultAreaMap.containsKey(area.getTechnicianAccountId())) {
                defaultAreaMap.put(area.getTechnicianAccountId(), area);
            }
        }

        int serviceKind = serviceMode == SERVICE_MODE_ONSITE_INSTALL ? 2 : 1;
        List<TechnicianVisitFeePolicies> policyList = technicianVisitFeePoliciesService.list(
            new LambdaQueryWrapper<TechnicianVisitFeePolicies>()
                .in(TechnicianVisitFeePolicies::getTechnicianAccountId, validTechnicianIds)
                .eq(TechnicianVisitFeePolicies::getServiceKind, serviceKind)
                .eq(TechnicianVisitFeePolicies::getIsActive, 1)
                .eq(TechnicianVisitFeePolicies::getIsDelete, 0)
                .orderByDesc(TechnicianVisitFeePolicies::getEffectiveTime)
                .orderByDesc(TechnicianVisitFeePolicies::getCreatedTime)
        );
        Map<String, TechnicianVisitFeePolicies> latestPolicyMap = new HashMap<>();
        for (TechnicianVisitFeePolicies policy : policyList) {
            if (!latestPolicyMap.containsKey(policy.getTechnicianAccountId())) {
                latestPolicyMap.put(policy.getTechnicianAccountId(), policy);
            }
        }

        UserAddresses referenceAddress = findAddressById(userAddressList, selectedAddressId);
        if (referenceAddress == null && !userAddressList.isEmpty()) {
            referenceAddress = userAddressList.get(0);
        }

        List<TechnicianOptionWrapper> wrappers = new ArrayList<>();
        boolean onsiteMode = isOnsiteMode(serviceMode);
        for (TechnicianAccounts technician : technicianList) {
            TechnicianServiceAreas area = defaultAreaMap.get(technician.getId());
            if (area == null) {
                continue;
            }
            BigDecimal distanceKm = calculateDistanceKm(
                referenceAddress == null ? null : referenceAddress.getLatitude(),
                referenceAddress == null ? null : referenceAddress.getLongitude(),
                area.getCenterLatitude(),
                area.getCenterLongitude()
            );

            TechnicianVisitFeePolicies policy = latestPolicyMap.get(technician.getId());
            if (onsiteMode && policy == null) {
                continue;
            }
            if (onsiteMode && policy != null && policy.getMaxVisitFee() != null && policy.getMaxVisitFee().compareTo(BIG_DECIMAL_ZERO) > 0) {
                if (distanceKm.compareTo(policy.getMaxVisitFee()) > 0) {
                    continue;
                }
            }

            UserOrderFlowModel.TechnicianItem item = new UserOrderFlowModel.TechnicianItem();
            item.setId(technician.getId());
            item.setName(technician.getUsername());
            item.setRating(formatDecimal(defaultZero(technician.getRating()), 1));
            item.setOrderCount(technician.getOrderCount() == null ? 0 : technician.getOrderCount());
            item.setAccountStatus(technician.getAccountStatus());
            item.setWorkStatus(technician.getWorkStatus());
            item.setWorkStatusText(mapWorkStatusText(technician.getWorkStatus()));
            item.setWorkStatusType(mapWorkStatusType(technician.getWorkStatus()));
            item.setDistanceText(formatDecimal(distanceKm, 1));
            item.setMaxDistanceText(policy == null || policy.getMaxVisitFee() == null
                ? "-"
                : formatDecimal(policy.getMaxVisitFee(), 1));
            BigDecimal recommendScore = calculateRecommendScore(technician.getRating(), technician.getOrderCount());
            item.setRecommendScore(formatDecimal(recommendScore, 2));
            item.setIsRecommend(false);

            TechnicianOptionWrapper wrapper = new TechnicianOptionWrapper();
            wrapper.item = item;
            wrapper.distanceKm = distanceKm;
            wrapper.recommendScore = recommendScore;
            wrappers.add(wrapper);
        }

        if (onsiteMode) {
            wrappers.sort((a, b) -> {
                int scoreCompare = b.recommendScore.compareTo(a.recommendScore);
                if (scoreCompare != 0) {
                    return scoreCompare;
                }
                return a.distanceKm.compareTo(b.distanceKm);
            });
        } else {
            wrappers.sort((a, b) -> {
                int distanceCompare = a.distanceKm.compareTo(b.distanceKm);
                if (distanceCompare != 0) {
                    return distanceCompare;
                }
                return b.recommendScore.compareTo(a.recommendScore);
            });
        }

        List<UserOrderFlowModel.TechnicianItem> result = wrappers.stream()
            .map(wrapper -> wrapper.item)
            .collect(Collectors.toList());
        if (!result.isEmpty()) {
            result.get(0).setIsRecommend(true);
        }
        return result;
    }

    private List<UserOrderFlowModel.TechnicianItem> listBrowsableTechnicians(
        String selectedAddressId,
        List<UserAddresses> userAddressList
    ) {
        List<TechnicianAccounts> technicianList = technicianAccountsService.list(
            new LambdaQueryWrapper<TechnicianAccounts>()
                .eq(TechnicianAccounts::getAccountStatus, TECHNICIAN_ACCOUNT_ACTIVE)
                .eq(TechnicianAccounts::getIsDelete, 0)
                .orderByDesc(TechnicianAccounts::getUpdatedTime)
                .orderByDesc(TechnicianAccounts::getCreatedTime)
        );
        if (technicianList.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> technicianIds = technicianList.stream()
            .map(TechnicianAccounts::getId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        if (technicianIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<TechnicianServiceAreas> areaList = technicianServiceAreasService.list(
            new LambdaQueryWrapper<TechnicianServiceAreas>()
                .in(TechnicianServiceAreas::getTechnicianAccountId, technicianIds)
                .eq(TechnicianServiceAreas::getIsActive, 1)
                .eq(TechnicianServiceAreas::getIsDelete, 0)
                .orderByDesc(TechnicianServiceAreas::getIsDefault)
                .orderByDesc(TechnicianServiceAreas::getUpdatedTime)
                .orderByDesc(TechnicianServiceAreas::getCreatedTime)
        );
        Map<String, TechnicianServiceAreas> defaultAreaMap = new HashMap<>();
        for (TechnicianServiceAreas area : areaList) {
            if (!defaultAreaMap.containsKey(area.getTechnicianAccountId())) {
                defaultAreaMap.put(area.getTechnicianAccountId(), area);
            }
        }

        UserAddresses referenceAddress = findAddressById(userAddressList, selectedAddressId);
        if (referenceAddress == null && !userAddressList.isEmpty()) {
            referenceAddress = userAddressList.get(0);
        }

        List<TechnicianOptionWrapper> wrappers = new ArrayList<>();
        for (TechnicianAccounts technician : technicianList) {
            TechnicianServiceAreas area = defaultAreaMap.get(technician.getId());
            if (area == null) {
                continue;
            }

            BigDecimal distanceKm = calculateDistanceKm(
                referenceAddress == null ? null : referenceAddress.getLatitude(),
                referenceAddress == null ? null : referenceAddress.getLongitude(),
                area.getCenterLatitude(),
                area.getCenterLongitude()
            );

            UserOrderFlowModel.TechnicianItem item = new UserOrderFlowModel.TechnicianItem();
            item.setId(technician.getId());
            item.setName(technician.getUsername());
            item.setRating(formatDecimal(defaultZero(technician.getRating()), 1));
            item.setOrderCount(technician.getOrderCount() == null ? 0 : technician.getOrderCount());
            item.setAccountStatus(technician.getAccountStatus());
            item.setWorkStatus(technician.getWorkStatus());
            item.setWorkStatusText(mapWorkStatusText(technician.getWorkStatus()));
            item.setWorkStatusType(mapWorkStatusType(technician.getWorkStatus()));
            item.setDistanceText(formatDecimal(distanceKm, 1));
            item.setMaxDistanceText("-");
            item.setRecommendScore(formatDecimal(calculateRecommendScore(technician.getRating(), technician.getOrderCount()), 2));
            item.setIsRecommend(false);

            TechnicianOptionWrapper wrapper = new TechnicianOptionWrapper();
            wrapper.item = item;
            wrapper.distanceKm = distanceKm;
            wrapper.recommendScore = calculateRecommendScore(technician.getRating(), technician.getOrderCount());
            wrappers.add(wrapper);
        }

        wrappers.sort((a, b) -> {
            int distanceCompare = a.distanceKm.compareTo(b.distanceKm);
            if (distanceCompare != 0) {
                return distanceCompare;
            }
            return b.recommendScore.compareTo(a.recommendScore);
        });

        return wrappers.stream()
            .map(wrapper -> wrapper.item)
            .collect(Collectors.toList());
    }

    private void fillTechnicianFollowAndAvatar(String accountId, List<UserOrderFlowModel.TechnicianItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<String> technicianIds = items.stream()
            .map(UserOrderFlowModel.TechnicianItem::getId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toList());
        if (technicianIds.isEmpty()) {
            return;
        }
        Map<String, String> avatarMap = loadLatestImageUrlMap(technicianIds, AVATAR_BUSINESS_TYPE);
        Set<String> followedIds = findFollowedTechnicianIdSet(accountId, technicianIds);
        for (UserOrderFlowModel.TechnicianItem item : items) {
            if (item == null || !StringUtils.hasText(item.getId())) {
                continue;
            }
            item.setAvatarUrl(avatarMap.get(item.getId()));
            item.setIsFollowed(followedIds.contains(item.getId()));
        }
    }

    private boolean isTechnicianFollowed(String accountId, String technicianId) {
        if (!StringUtils.hasText(trimToNull(accountId)) || !StringUtils.hasText(trimToNull(technicianId))) {
            return false;
        }
        UserFollowTechnicians follow = userFollowTechniciansService.getOne(
            new LambdaQueryWrapper<UserFollowTechnicians>()
                .eq(UserFollowTechnicians::getAccountId, accountId)
                .eq(UserFollowTechnicians::getTechnicianAccountId, technicianId)
                .eq(UserFollowTechnicians::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        return follow != null;
    }

    private Set<String> findFollowedTechnicianIdSet(String accountId, List<String> technicianIds) {
        if (!StringUtils.hasText(trimToNull(accountId)) || technicianIds == null || technicianIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<UserFollowTechnicians> follows = userFollowTechniciansService.list(
            new LambdaQueryWrapper<UserFollowTechnicians>()
                .eq(UserFollowTechnicians::getAccountId, accountId)
                .in(UserFollowTechnicians::getTechnicianAccountId, technicianIds)
                .eq(UserFollowTechnicians::getIsDelete, 0)
        );
        if (follows == null || follows.isEmpty()) {
            return Collections.emptySet();
        }
        return follows.stream()
            .map(UserFollowTechnicians::getTechnicianAccountId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
    }

    private Map<String, String> loadLatestImageUrlMap(List<String> businessIds, String businessType) {
        if (businessIds == null || businessIds.isEmpty() || !StringUtils.hasText(trimToNull(businessType))) {
            return new HashMap<>();
        }
        List<Images> images = imagesService.list(
            new LambdaQueryWrapper<Images>()
                .eq(Images::getBusinessType, businessType)
                .in(Images::getBusinessId, businessIds)
                .orderByDesc(Images::getCreatedTime)
        );
        Map<String, String> result = new HashMap<>();
        for (Images image : images) {
            if (image == null || !StringUtils.hasText(image.getBusinessId()) || !StringUtils.hasText(image.getFileUrl())) {
                continue;
            }
            if (!result.containsKey(image.getBusinessId())) {
                result.put(image.getBusinessId(), image.getFileUrl());
            }
        }
        return result;
    }

    private String buildCategoryPathText(String categoryId) {
        String normalizedCategoryId = trimToNull(categoryId);
        if (!StringUtils.hasText(normalizedCategoryId)) {
            return "";
        }
        ServiceCategories current = serviceCategoriesService.getById(normalizedCategoryId);
        if (current == null || current.getIsDelete() != null && current.getIsDelete() == 1) {
            return "";
        }
        List<String> names = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        while (current != null && StringUtils.hasText(current.getId()) && visited.add(current.getId())) {
            if (StringUtils.hasText(current.getName())) {
                names.add(0, safeString(current.getName()));
            }
            String parentId = trimToNull(current.getParentId());
            if (!StringUtils.hasText(parentId)) {
                break;
            }
            current = serviceCategoriesService.getById(parentId);
        }
        return names.stream().filter(StringUtils::hasText).collect(Collectors.joining(" / "));
    }

    private List<ServiceCategories> listActiveServiceCategories() {
        return serviceCategoriesService.list(
            new LambdaQueryWrapper<ServiceCategories>()
                .eq(ServiceCategories::getIsActive, 1)
                .eq(ServiceCategories::getIsDelete, 0)
        );
    }

    private Set<String> resolveApplicableServiceTypeCategoryIds(String categoryId, List<ServiceCategories> categories) {
        Set<String> ids = new LinkedHashSet<>();
        String normalizedCategoryId = trimToNull(categoryId);
        if (!StringUtils.hasText(normalizedCategoryId) || categories == null || categories.isEmpty()) {
            return ids;
        }
        Map<String, ServiceCategories> categoryMap = categories.stream()
            .filter(Objects::nonNull)
            .filter(item -> StringUtils.hasText(item.getId()))
            .collect(Collectors.toMap(ServiceCategories::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
        ServiceCategories current = categoryMap.get(normalizedCategoryId);
        if (current == null) {
            return ids;
        }
        Integer level = safeInt(current.getLevel());
        if (level == 2) {
            ids.add(current.getId());
            ids.addAll(findDescendantCategoryIds(current.getId(), categories, 3));
            return ids;
        }
        if (level == 3) {
            ids.add(current.getId());
            String parentId = trimToNull(current.getParentId());
            if (StringUtils.hasText(parentId)) {
                ids.add(parentId);
            }
            return ids;
        }
        return ids;
    }

    private List<String> findDescendantCategoryIds(String categoryId, List<ServiceCategories> categories, int targetLevel) {
        List<String> result = new ArrayList<>();
        if (!StringUtils.hasText(categoryId) || categories == null || categories.isEmpty()) {
            return result;
        }
        Map<String, List<ServiceCategories>> childrenByParentId = categories.stream()
            .filter(Objects::nonNull)
            .filter(item -> StringUtils.hasText(item.getParentId()))
            .collect(Collectors.groupingBy(ServiceCategories::getParentId, LinkedHashMap::new, Collectors.toList()));
        List<ServiceCategories> queue = new ArrayList<>(childrenByParentId.getOrDefault(categoryId, Collections.emptyList()));
        for (int i = 0; i < queue.size(); i++) {
            ServiceCategories current = queue.get(i);
            if (current == null || !StringUtils.hasText(current.getId())) {
                continue;
            }
            if (safeInt(current.getLevel()) == targetLevel) {
                result.add(current.getId());
            }
            queue.addAll(childrenByParentId.getOrDefault(current.getId(), Collections.emptyList()));
        }
        return result;
    }

    private boolean isServiceTypeApplicableToCategory(ServiceTypes serviceType, String categoryId, List<ServiceCategories> categories) {
        if (serviceType == null) {
            return false;
        }
        String serviceTypeCategoryId = trimToNull(serviceType.getCategoryId());
        String normalizedCategoryId = trimToNull(categoryId);
        if (!StringUtils.hasText(serviceTypeCategoryId) || !StringUtils.hasText(normalizedCategoryId)) {
            return false;
        }
        return resolveApplicableServiceTypeCategoryIds(normalizedCategoryId, categories).contains(serviceTypeCategoryId);
    }

    private UserAddresses findAddressById(List<UserAddresses> addresses, String addressId) {
        if (!StringUtils.hasText(addressId)) {
            return null;
        }
        for (UserAddresses address : addresses) {
            if (addressId.equals(address.getId())) {
                return address;
            }
        }
        return null;
    }

    private UserAddresses requireAddress(String accountId, String addressId) {
        String normalizedAddressId = trimToNull(addressId);
        if (!StringUtils.hasText(normalizedAddressId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "serviceAddressId 不能为空");
        }
        UserAddresses address = userAddressesService.getOne(
            new LambdaQueryWrapper<UserAddresses>()
                .eq(UserAddresses::getId, normalizedAddressId)
                .eq(UserAddresses::getAccountId, accountId)
                .eq(UserAddresses::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (address == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "地址不存在");
        }
        return address;
    }

    private UserAddresses resolveSubmitAddress(int serviceMode, String requestAddressId, List<UserAddresses> userAddressList) {
        UserAddresses selectedAddress = null;
        String normalizedAddressId = trimToNull(requestAddressId);
        if (StringUtils.hasText(normalizedAddressId)) {
            selectedAddress = findAddressById(userAddressList, normalizedAddressId);
            if (selectedAddress == null) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "服务地址不存在");
            }
        } else if (isOnsiteMode(serviceMode)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择服务地址");
        }

        if (!isOnsiteMode(serviceMode)) {
            return selectedAddress;
        }

        if (selectedAddress == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "服务地址不存在");
        }
        return selectedAddress;
    }

    private Long resolveSubmitAppointmentTime(int serviceMode, Long requestAppointmentTime, String technicianId) {
        if (isOnsiteMode(serviceMode)) {
            if (requestAppointmentTime == null || requestAppointmentTime <= 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "上门服务必须选择预约时间");
            }
            validateAppointmentTimeInternal(technicianId, requestAppointmentTime, null);
            return requestAppointmentTime;
        }
        long now = System.currentTimeMillis();
        if (requestAppointmentTime == null || requestAppointmentTime <= 0) {
            return now;
        }
        return requestAppointmentTime;
    }

    private List<TechnicianWorkTimes> listAvailableWorkTimes(String technicianId) {
        return technicianWorkTimesService.list(
            new LambdaQueryWrapper<TechnicianWorkTimes>()
                .eq(TechnicianWorkTimes::getTechnicianAccountId, technicianId)
                .eq(TechnicianWorkTimes::getIsAvailable, 1)
                .eq(TechnicianWorkTimes::getIsDelete, 0)
                .orderByAsc(TechnicianWorkTimes::getDayOfWeek)
                .orderByAsc(TechnicianWorkTimes::getStartTime)
        );
    }

    private Map<Integer, List<TechnicianWorkTimes>> groupWorkTimesByDay(List<TechnicianWorkTimes> workTimeList) {
        Map<Integer, List<TechnicianWorkTimes>> grouped = new HashMap<>();
        if (workTimeList == null || workTimeList.isEmpty()) {
            return grouped;
        }
        for (TechnicianWorkTimes item : workTimeList) {
            if (item == null || item.getDayOfWeek() == null) {
                continue;
            }
            int dayOfWeek = item.getDayOfWeek();
            if (dayOfWeek < 1 || dayOfWeek > 7) {
                continue;
            }
            LocalTime startTime = toLocalTime(item.getStartTime());
            LocalTime endTime = toLocalTime(item.getEndTime());
            if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
                continue;
            }
            grouped.computeIfAbsent(dayOfWeek, key -> new ArrayList<>()).add(item);
        }
        for (Map.Entry<Integer, List<TechnicianWorkTimes>> entry : grouped.entrySet()) {
            entry.getValue().sort(
                Comparator.comparing(item -> {
                    LocalTime time = toLocalTime(item.getStartTime());
                    return time == null ? LocalTime.MAX : time;
                })
            );
        }
        return grouped;
    }

    private List<UserOrderFlowModel.AppointmentWorkWindowItem> buildAppointmentWorkWindows(
        Map<Integer, List<TechnicianWorkTimes>> dayToRecords
    ) {
        List<UserOrderFlowModel.AppointmentWorkWindowItem> windows = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            List<TechnicianWorkTimes> dayWorkTimes = dayToRecords.getOrDefault(day, Collections.emptyList());
            for (TechnicianWorkTimes item : dayWorkTimes) {
                LocalTime startTime = toLocalTime(item.getStartTime());
                LocalTime endTime = toLocalTime(item.getEndTime());
                if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
                    continue;
                }
                UserOrderFlowModel.AppointmentWorkWindowItem row = new UserOrderFlowModel.AppointmentWorkWindowItem();
                row.setDayOfWeek(day);
                row.setDayLabel(getDayOfWeekLabel(day));
                row.setStartTime(startTime.format(TIME_FORMATTER));
                row.setEndTime(endTime.format(TIME_FORMATTER));
                windows.add(row);
            }
        }
        return windows;
    }

    private List<UserOrderFlowModel.AppointmentSlotItem> buildSuggestedAppointmentSlots(
        Map<Integer, List<TechnicianWorkTimes>> dayToRecords,
        LocalDate startDate,
        LocalDateTime now,
        int safeDays
    ) {
        List<UserOrderFlowModel.AppointmentSlotItem> slots = new ArrayList<>();
        LocalDateTime safeNow = now == null ? LocalDateTime.now().plusMinutes(getMinAppointmentLeadMinutes()).withSecond(0).withNano(0) : now;
        LocalDate currentDate = safeNow.toLocalDate();
        LocalDate baseDate = startDate == null ? currentDate : startDate;
        for (int i = 0; i < safeDays; i++) {
            LocalDate date = baseDate.plusDays(i);
            List<TechnicianWorkTimes> dayWorkTimes = dayToRecords.getOrDefault(date.getDayOfWeek().getValue(), Collections.emptyList());
            if (dayWorkTimes.isEmpty()) {
                continue;
            }

            LocalTime selectedTime = null;
            LocalTime selectedEnd = null;
            LocalTime nowTime = safeNow.toLocalTime();
            for (TechnicianWorkTimes item : dayWorkTimes) {
                LocalTime startTime = toLocalTime(item.getStartTime());
                LocalTime endTime = toLocalTime(item.getEndTime());
                if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
                    continue;
                }
                LocalTime candidate = startTime;
                if (date.equals(baseDate) && candidate.isBefore(nowTime)) {
                    candidate = nowTime;
                }
                if (candidate.isAfter(endTime)) {
                    continue;
                }
                selectedTime = candidate;
                selectedEnd = endTime;
                break;
            }

            if (selectedTime == null || selectedEnd == null) {
                continue;
            }

            UserOrderFlowModel.AppointmentSlotItem slot = new UserOrderFlowModel.AppointmentSlotItem();
            slot.setId(buildAppointmentSlotId(date, selectedTime));
            slot.setLabel(buildAppointmentLabel(date, selectedTime, selectedEnd, currentDate));
            slot.setAppointmentTime(LocalDateTime.of(date, selectedTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            slots.add(slot);
        }
        return slots;
    }

    private void validateAppointmentTimeInternal(String technicianId, Long appointmentTimeMillis, String excludeOrderId) {
        if (!StringUtils.hasText(trimToNull(technicianId))) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "technicianId 不能为空");
        }
        if (appointmentTimeMillis == null || appointmentTimeMillis <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "appointmentTime 不能为空");
        }

        LocalDateTime earliestDateTime = getEarliestAppointmentDateTime();
        LocalDateTime appointmentDateTime = Instant.ofEpochMilli(appointmentTimeMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .withSecond(0)
            .withNano(0);
        if (appointmentDateTime.isBefore(earliestDateTime)) {
            throw new BusinessException(
                ErrorCode.PARAM_ERROR,
                "预约时间需至少提前 " + getMinAppointmentLeadMinutes() + " 分钟"
            );
        }
        LocalDate maxDate = earliestDateTime.toLocalDate().plusDays(getMaxAppointmentDays() - 1L);
        if (appointmentDateTime.toLocalDate().isAfter(maxDate)) {
            throw new BusinessException(
                ErrorCode.PARAM_ERROR,
                "预约时间不能超过未来 " + getMaxAppointmentDays() + " 天"
            );
        }

        List<TechnicianWorkTimes> workTimeList = listAvailableWorkTimes(technicianId);
        Map<Integer, List<TechnicianWorkTimes>> dayToRecords = groupWorkTimesByDay(workTimeList);
        if (dayToRecords.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前技师暂未配置可预约时间");
        }

        int dayOfWeek = appointmentDateTime.getDayOfWeek().getValue();
        List<TechnicianWorkTimes> dayWorkTimes = dayToRecords.getOrDefault(dayOfWeek, Collections.emptyList());
        if (dayWorkTimes.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "所选日期不在师傅工作日内");
        }
        if (!isAppointmentInWorkWindows(appointmentDateTime.toLocalTime(), dayWorkTimes)) {
            throw new BusinessException(
                ErrorCode.BUSINESS_ERROR,
                "所选时间不在工作时间内，当日可选：" + buildWorkWindowText(dayWorkTimes)
            );
        }

        ensureAppointmentSlotAvailable(technicianId, appointmentTimeMillis, excludeOrderId);
    }

    private void ensureAppointmentSlotAvailable(String technicianId, Long appointmentTimeMillis, String excludeOrderId) {
        LambdaQueryWrapper<RepairOrders> wrapper = new LambdaQueryWrapper<RepairOrders>()
            .eq(RepairOrders::getTechnicianAccountId, technicianId)
            .eq(RepairOrders::getAppointmentTime, appointmentTimeMillis)
            .in(
                RepairOrders::getStatus,
                ORDER_STATUS_PENDING,
                ORDER_STATUS_ACCEPTED,
                ORDER_STATUS_ON_THE_WAY,
                ORDER_STATUS_IN_SERVICE,
                ORDER_STATUS_WAITING_PAY
            )
            .eq(RepairOrders::getIsDelete, 0);
        if (StringUtils.hasText(trimToNull(excludeOrderId))) {
            wrapper.ne(RepairOrders::getId, trimToNull(excludeOrderId));
        }
        long count = repairOrdersService.count(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该预约时间已被其他用户占用，请重新选择");
        }
    }

    private LocalDateTime getEarliestAppointmentDateTime() {
        return LocalDateTime.now().plusMinutes(getMinAppointmentLeadMinutes()).withSecond(0).withNano(0);
    }

    private boolean isAppointmentInWorkWindows(LocalTime appointmentTime, List<TechnicianWorkTimes> dayWorkTimes) {
        for (TechnicianWorkTimes item : dayWorkTimes) {
            LocalTime startTime = toLocalTime(item.getStartTime());
            LocalTime endTime = toLocalTime(item.getEndTime());
            if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
                continue;
            }
            if (!appointmentTime.isBefore(startTime) && !appointmentTime.isAfter(endTime)) {
                return true;
            }
        }
        return false;
    }

    private String buildWorkWindowText(List<TechnicianWorkTimes> dayWorkTimes) {
        List<String> ranges = new ArrayList<>();
        for (TechnicianWorkTimes item : dayWorkTimes) {
            LocalTime startTime = toLocalTime(item.getStartTime());
            LocalTime endTime = toLocalTime(item.getEndTime());
            if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
                continue;
            }
            ranges.add(startTime.format(TIME_FORMATTER) + "-" + endTime.format(TIME_FORMATTER));
        }
        return ranges.isEmpty() ? "暂无" : String.join("、", ranges);
    }

    private String getDayOfWeekLabel(int dayOfWeek) {
        switch (dayOfWeek) {
            case 1:
                return "周一";
            case 2:
                return "周二";
            case 3:
                return "周三";
            case 4:
                return "周四";
            case 5:
                return "周五";
            case 6:
                return "周六";
            case 7:
                return "周日";
            default:
                return "未知";
        }
    }

    private FeeCalcResult calculateFee(int serviceMode, String technicianId, UserAddresses address) {
        if (!isOnsiteMode(serviceMode)) {
            return FeeCalcResult.zero();
        }
        TechnicianServiceAreas area = getDefaultServiceArea(technicianId);
        if (area == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "技师未配置服务区域");
        }
        int serviceKind = serviceMode == SERVICE_MODE_ONSITE_INSTALL ? 2 : 1;
        TechnicianVisitFeePolicies policy = getLatestVisitPolicy(technicianId, serviceKind);
        if (policy == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "技师未配置上门费策略");
        }

        BigDecimal distanceKm = calculateDistanceKm(
            address.getLatitude(),
            address.getLongitude(),
            area.getCenterLatitude(),
            area.getCenterLongitude()
        ).setScale(3, RoundingMode.HALF_UP);

        if (policy.getMaxVisitFee() != null && policy.getMaxVisitFee().compareTo(BIG_DECIMAL_ZERO) > 0) {
            if (distanceKm.compareTo(policy.getMaxVisitFee()) > 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前地址超出服务范围");
            }
        }

        BigDecimal doorFee = defaultZero(policy.getMinVisitFee()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal baseRadiusKm = defaultZero(policy.getBaseRadiusKm()).setScale(3, RoundingMode.HALF_UP);
        BigDecimal overDistanceKm = distanceKm.subtract(baseRadiusKm);
        if (overDistanceKm.compareTo(BIG_DECIMAL_ZERO) < 0) {
            overDistanceKm = BIG_DECIMAL_ZERO;
        }
        BigDecimal roundedOverDistanceKm = roundDistance(overDistanceKm, policy.getRoundingRule());
        BigDecimal extraFeePerKm = defaultZero(policy.getExtraFeePerKm()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal distanceFee = roundedOverDistanceKm.multiply(extraFeePerKm).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = doorFee.add(distanceFee).setScale(2, RoundingMode.HALF_UP);

        FeeCalcResult result = new FeeCalcResult();
        result.distanceKm = distanceKm;
        result.doorFee = doorFee;
        result.distanceFee = distanceFee;
        result.totalAmount = totalAmount;
        result.baseRadiusKm = baseRadiusKm;
        result.distanceOverKm = roundedOverDistanceKm.setScale(3, RoundingMode.HALF_UP);
        result.minVisitFeeSnapshot = doorFee;
        result.extraFeePerKmSnapshot = extraFeePerKm;
        result.distanceCalcTypeSnapshot = policy.getDistanceCalcType() == null ? 1 : policy.getDistanceCalcType();
        result.roundingRuleSnapshot = policy.getRoundingRule() == null ? 1 : policy.getRoundingRule();
        return result;
    }

    private BigDecimal roundDistance(BigDecimal overDistance, Integer roundingRule) {
        if (overDistance == null || overDistance.compareTo(BIG_DECIMAL_ZERO) <= 0) {
            return BIG_DECIMAL_ZERO;
        }
        if (roundingRule != null && roundingRule == 2) {
            return overDistance.setScale(0, RoundingMode.HALF_UP);
        }
        if (roundingRule != null && roundingRule == 3) {
            return overDistance.setScale(3, RoundingMode.HALF_UP);
        }
        return overDistance.setScale(0, RoundingMode.CEILING);
    }

    private TechnicianServiceAreas getDefaultServiceArea(String technicianId) {
        return technicianServiceAreasService.getOne(
            new LambdaQueryWrapper<TechnicianServiceAreas>()
                .eq(TechnicianServiceAreas::getTechnicianAccountId, technicianId)
                .eq(TechnicianServiceAreas::getIsActive, 1)
                .eq(TechnicianServiceAreas::getIsDelete, 0)
                .orderByDesc(TechnicianServiceAreas::getIsDefault)
                .orderByDesc(TechnicianServiceAreas::getUpdatedTime)
                .orderByDesc(TechnicianServiceAreas::getCreatedTime)
                .last("limit 1"),
            false
        );
    }

    private TechnicianVisitFeePolicies getLatestVisitPolicy(String technicianId, int serviceKind) {
        return technicianVisitFeePoliciesService.getOne(
            new LambdaQueryWrapper<TechnicianVisitFeePolicies>()
                .eq(TechnicianVisitFeePolicies::getTechnicianAccountId, technicianId)
                .eq(TechnicianVisitFeePolicies::getServiceKind, serviceKind)
                .eq(TechnicianVisitFeePolicies::getIsActive, 1)
                .eq(TechnicianVisitFeePolicies::getIsDelete, 0)
                .orderByDesc(TechnicianVisitFeePolicies::getEffectiveTime)
                .orderByDesc(TechnicianVisitFeePolicies::getCreatedTime)
                .last("limit 1"),
            false
        );
    }

    private String buildFeeFormula(FeeCalcResult fee) {
        if (fee == null) {
            return "";
        }
        return "距离 "
            + formatDecimal(fee.distanceKm, 1)
            + "km，基础半径 "
            + formatDecimal(fee.baseRadiusKm, 1)
            + "km，超出部分按 "
            + formatMoney(fee.extraFeePerKmSnapshot)
            + " 元/公里";
    }

    private String buildFeeRuleSnapshot(FeeCalcResult fee) {
        if (fee == null) {
            return "{}";
        }
        return "{"
            + "\"distanceKm\":" + formatDecimal(fee.distanceKm, 3) + ","
            + "\"baseRadiusKm\":" + formatDecimal(fee.baseRadiusKm, 3) + ","
            + "\"distanceOverKm\":" + formatDecimal(fee.distanceOverKm, 3) + ","
            + "\"minVisitFee\":" + formatMoney(fee.minVisitFeeSnapshot) + ","
            + "\"extraFeePerKm\":" + formatMoney(fee.extraFeePerKmSnapshot) + ","
            + "\"distanceCalcType\":" + fee.distanceCalcTypeSnapshot + ","
            + "\"roundingRule\":" + fee.roundingRuleSnapshot
            + "}";
    }

    private String mapWorkStatusText(Integer workStatus) {
        if (workStatus == null) {
            return "未知";
        }
        if (workStatus == TECHNICIAN_WORK_ONLINE) {
            return "在线";
        }
        if (workStatus == TECHNICIAN_WORK_BUSY) {
            return "忙碌";
        }
        if (workStatus == 3) {
            return "休息";
        }
        return "未知";
    }

    private String mapWorkStatusType(Integer workStatus) {
        if (workStatus == null) {
            return "default";
        }
        if (workStatus == TECHNICIAN_WORK_ONLINE) {
            return "success";
        }
        if (workStatus == TECHNICIAN_WORK_BUSY) {
            return "warning";
        }
        if (workStatus == 3) {
            return "primary";
        }
        return "default";
    }

    private BigDecimal calculateRecommendScore(BigDecimal rating, Integer orderCount) {
        BigDecimal ratingScore = defaultZero(rating).multiply(BigDecimal.valueOf(100));
        int order = orderCount == null ? 0 : Math.max(orderCount, 0);
        BigDecimal orderScore = BigDecimal.valueOf(Math.min(order, 500)).multiply(BigDecimal.valueOf(0.2));
        return ratingScore.add(orderScore).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDistanceKm(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return BIG_DECIMAL_ZERO;
        }
        double latitude1 = lat1.doubleValue();
        double longitude1 = lon1.doubleValue();
        double latitude2 = lat2.doubleValue();
        double longitude2 = lon2.doubleValue();
        double dLat = Math.toRadians(latitude2 - latitude1);
        double dLon = Math.toRadians(longitude2 - longitude1);
        double rLat1 = Math.toRadians(latitude1);
        double rLat2 = Math.toRadians(latitude2);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(rLat1) * Math.cos(rLat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = 6371.0088 * c;
        return BigDecimal.valueOf(distance).setScale(3, RoundingMode.HALF_UP);
    }

    private String buildAppointmentSlotId(LocalDate date, LocalTime startTime) {
        return "slot_" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + "_" + startTime.format(DateTimeFormatter.ofPattern("HHmmss"));
    }

    private String buildAppointmentLabel(LocalDate date, LocalTime startTime, LocalTime endTime, LocalDate today) {
        String prefix;
        if (date.equals(today)) {
            prefix = "今天";
        } else if (date.equals(today.plusDays(1))) {
            prefix = "明天";
        } else {
            prefix = date.format(DATE_SHORT_FORMATTER);
        }
        return prefix + " " + startTime.format(TIME_FORMATTER) + " - " + endTime.format(TIME_FORMATTER);
    }

    private int resolveAppointmentDays(Integer days) {
        int defaultDays = getDefaultAppointmentDays();
        int value = days == null ? defaultDays : days;
        if (value <= 0) {
            value = defaultDays;
        }
        return Math.min(value, getMaxAppointmentDays());
    }

    private int getDefaultAppointmentDays() {
        Integer value = systemConfigsService.getIntegerConfig("order.appointment.default_days", DEFAULT_APPOINTMENT_DAYS);
        return value == null || value <= 0 ? DEFAULT_APPOINTMENT_DAYS : value;
    }

    private int getMaxAppointmentDays() {
        Integer value = systemConfigsService.getIntegerConfig(
            "order.appointment.max_days",
            DEFAULT_MAX_APPOINTMENT_DAYS
        );
        int maxDays = value == null || value <= 0 ? DEFAULT_MAX_APPOINTMENT_DAYS : value;
        return Math.max(maxDays, getDefaultAppointmentDays());
    }

    private int getMinAppointmentLeadMinutes() {
        Integer value = systemConfigsService.getIntegerConfig(
            "order.appointment.min_lead_minutes",
            DEFAULT_MIN_APPOINTMENT_LEAD_MINUTES
        );
        return value == null || value < 0 ? DEFAULT_MIN_APPOINTMENT_LEAD_MINUTES : value;
    }

    private LocalTime toLocalTime(java.util.Date value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Time) {
            return ((Time) value).toLocalTime();
        }
        return Instant.ofEpochMilli(value.getTime()).atZone(ZoneId.systemDefault()).toLocalTime().withNano(0);
    }

    private Long parsePurchaseDate(String purchaseDate) {
        String normalized = trimToNull(purchaseDate);
        if (!StringUtils.hasText(normalized)
            || "未知".equals(normalized)
            || "不清楚".equals(normalized)
            || "未填写".equals(normalized)) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(normalized, DATE_FORMATTER);
            return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "purchaseDate 格式错误，应为 yyyy-MM-dd");
        }
    }

    private Integer normalizePaymentMethod(Integer paymentMethod) {
        if (paymentMethod == null) {
            return PAYMENT_METHOD_WECHAT;
        }
        if (paymentMethod != PAYMENT_METHOD_WECHAT
            && paymentMethod != PAYMENT_METHOD_ALIPAY
            && paymentMethod != PAYMENT_METHOD_WALLET) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "paymentMethod 仅支持 1-微信支付、2-支付宝支付、5-钱包支付");
        }
        return paymentMethod;
    }

    private void createPaymentRecord(
        String orderId,
        String orderNo,
        String accountId,
        Integer paymentMethod,
        BigDecimal paymentAmount,
        long now
    ) {
        PaymentRecords record = new PaymentRecords();
        record.setId(SnowflakeIdUtil.nextPaymentRecordId());
        record.setPaymentNo(buildPaymentNo(orderNo));
        record.setOrderId(orderId);
        record.setOrderType(1);
        record.setAccountId(accountId);
        record.setPaymentMethod(paymentMethod);
        record.setPaymentAmount(normalizeMoney(paymentAmount));
        record.setPaymentStatus(PAYMENT_RECORD_STATUS_SUCCESS);
        record.setThirdPartyNo(buildThirdPartyNo(orderNo, paymentMethod));
        record.setPaymentTime(now);
        record.setRefundAmount(BIG_DECIMAL_ZERO.setScale(2, RoundingMode.HALF_UP));
        record.setRemark(buildPaymentRemark(paymentMethod));
        record.setCreatedTime(now);
        record.setUpdatedTime(now);
        record.setIsDelete(0);
        if (!paymentRecordsService.save(record)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "閸掓稑缂撻弨顖欑帛鐠佹澘缍嶆径杈Е");
        }
    }

    private String buildPaymentNo(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            return "PAY" + System.currentTimeMillis();
        }
        return "PAY" + orderNo.replace("NO", "");
    }

    private String buildThirdPartyNo(String orderNo, Integer paymentMethod) {
        return safePaymentMethodCode(paymentMethod) + compactTradeNo(buildPaymentNo(orderNo));
    }

    private String compactTradeNo(String source) {
        String value = StringUtils.hasText(source) ? source.replaceAll("[^0-9A-Za-z]", "") : "";
        if (!StringUtils.hasText(value)) {
            value = String.valueOf(System.currentTimeMillis());
        }
        return value.length() > 28 ? value.substring(value.length() - 28) : value;
    }

    private String buildPaymentRemark(Integer paymentMethod) {
        int method = safeInt(paymentMethod);
        if (method == PAYMENT_METHOD_WALLET) {
            return "维修订单预付费用（钱包支付）";
        }
        if (method == PAYMENT_METHOD_ALIPAY) {
            return "维修订单预付费用（支付宝支付）";
        }
        return "维修订单预付费用（微信支付）";
    }

    private String safePaymentMethodCode(Integer paymentMethod) {
        int method = safeInt(paymentMethod);
        if (method == PAYMENT_METHOD_ALIPAY) {
            return "ALI";
        }
        if (method == PAYMENT_METHOD_WALLET) {
            return "WLT";
        }
        return "WX";
    }

    private void saveFaultDetails(
        String orderId,
        String serviceTypeId,
        String accountId,
        List<UserOrderFlowModel.SubmitFaultItem> faultList,
        long now
    ) {
        if (faultList == null || faultList.isEmpty()) {
            return;
        }
        List<FaultPhenomena> validFaultList = faultPhenomenaService.list(
            new LambdaQueryWrapper<FaultPhenomena>()
                .eq(FaultPhenomena::getServiceTypeId, serviceTypeId)
                .eq(FaultPhenomena::getIsActive, 1)
                .eq(FaultPhenomena::getIsDelete, 0)
        );
        Set<String> validFaultIdSet = validFaultList.stream()
            .map(FaultPhenomena::getId)
            .collect(Collectors.toCollection(HashSet::new));

        for (int i = 0; i < faultList.size(); i++) {
            UserOrderFlowModel.SubmitFaultItem fault = faultList.get(i);
            if (fault == null) {
                continue;
            }
            String faultId = trimToNull(fault.getFaultId());
            if (StringUtils.hasText(faultId) && !validFaultIdSet.contains(faultId)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "故障现象不存在或已失效");
            }

            List<UserOrderFlowModel.SubmitImageItem> images = fault.getImages() == null
                ? Collections.emptyList()
                : fault.getImages();
            if (images.size() > 3) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "每项故障图片最多上传3张");
            }

            RepairOrderFaults record = new RepairOrderFaults();
            String faultRecordId = SnowflakeIdUtil.nextRepairOrderFaultId();
            record.setId(faultRecordId);
            record.setRepairOrderId(orderId);
            record.setFaultPhenomenonId(faultId);
            record.setFaultDescription(resolveFaultDescription(fault));
            record.setCreatedTime(now);
            record.setUpdatedTime(now);
            record.setIsDelete(0);
            if (!repairOrderFaultsService.save(record)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存故障信息失败");
            }

            for (int imgIndex = 0; imgIndex < images.size(); imgIndex++) {
                saveFaultImage(images.get(imgIndex), faultRecordId, accountId, now, imgIndex);
            }
            if (fault.getVideo() != null) {
                saveFaultVideo(fault.getVideo(), faultRecordId, accountId, now);
            }
        }
    }

    private String resolveFaultDescription(UserOrderFlowModel.SubmitFaultItem fault) {
        String description = trimToNull(fault.getFaultDescription());
        if (StringUtils.hasText(description)) {
            return description;
        }
        String faultName = trimToNull(fault.getFaultName());
        if (StringUtils.hasText(faultName)) {
            return faultName;
        }
        return "";
    }

    private void saveFaultImage(
        UserOrderFlowModel.SubmitImageItem image,
        String faultRecordId,
        String accountId,
        long now,
        int index
    ) {
        if (image == null) {
            return;
        }
        String fileUrl = requireMediaUrl(image.getUrl());
        String fileName = resolveMediaName(image.getName(), "fault-image-" + (index + 1) + ".jpg");

        Images entity = new Images();
        entity.setId(SnowflakeIdUtil.nextImageId());
        entity.setOriginalName(fileName);
        entity.setFileName(fileName);
        entity.setFilePath(fileUrl);
        entity.setFileUrl(fileUrl);
        entity.setFileSize(image.getFileSize() == null ? 0L : image.getFileSize());
        entity.setMimeType(StringUtils.hasText(trimToNull(image.getMimeType())) ? image.getMimeType() : "image/jpeg");
        entity.setWidth(image.getWidth());
        entity.setHeight(image.getHeight());
        entity.setUploaderId(accountId);
        entity.setUploaderType(1);
        entity.setBusinessType("REPAIR_ORDER_FAULT");
        entity.setBusinessId(faultRecordId);
        entity.setCreatedTime(now);
        entity.setIsDelete(0);
        if (!imagesService.save(entity)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存故障图片失败");
        }
    }

    private void saveFaultVideo(
        UserOrderFlowModel.SubmitVideoItem video,
        String faultRecordId,
        String accountId,
        long now
    ) {
        String fileUrl = requireMediaUrl(video.getUrl());
        String fileName = resolveMediaName(video.getName(), "fault-video.mp4");

        Videos entity = new Videos();
        entity.setId(SnowflakeIdUtil.nextVideoId());
        entity.setOriginalName(fileName);
        entity.setFileName(fileName);
        entity.setFilePath(fileUrl);
        entity.setFileUrl(fileUrl);
        entity.setFileSize(video.getFileSize() == null ? 0L : video.getFileSize());
        entity.setMimeType(StringUtils.hasText(trimToNull(video.getMimeType())) ? video.getMimeType() : "video/mp4");
        entity.setDuration(video.getDuration());
        entity.setWidth(video.getWidth());
        entity.setHeight(video.getHeight());
        entity.setThumbnailUrl(trimToNull(video.getThumbnailUrl()));
        entity.setUploaderId(accountId);
        entity.setUploaderType(1);
        entity.setBusinessType("REPAIR_ORDER_FAULT");
        entity.setBusinessId(faultRecordId);
        entity.setCreatedTime(now);
        entity.setIsDelete(0);
        if (!videosService.save(entity)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存故障视频失败");
        }
    }

    /*
    private void createUserPrepayFundFlow(
        String accountId,
        String orderId,
        String orderNo,
        BigDecimal amount,
        long now
    ) {
        AccountBalances balance = accountBalancesService.getOne(
            new LambdaQueryWrapper<AccountBalances>()
                .eq(AccountBalances::getAccountId, accountId)
                .eq(AccountBalances::getAccountType, ACCOUNT_TYPE_USER)
                .eq(AccountBalances::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (balance == null) {
            balance = new AccountBalances();
            balance.setId(SnowflakeIdUtil.nextAccountBalanceId());
            balance.setAccountId(accountId);
            balance.setAccountType(ACCOUNT_TYPE_USER);
            balance.setBalance(BIG_DECIMAL_ZERO);
            balance.setFrozenBalance(BIG_DECIMAL_ZERO);
            balance.setTotalIncome(BIG_DECIMAL_ZERO);
            balance.setTotalExpense(BIG_DECIMAL_ZERO);
            balance.setCreatedTime(now);
            balance.setUpdatedTime(now);
            balance.setIsDelete(0);
            if (!accountBalancesService.save(balance)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化用户资金账户失败");
            }
        }

        BigDecimal balanceBefore = defaultZero(balance.getBalance()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal balanceAfter = balanceBefore.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        balance.setBalance(balanceAfter);
        balance.setTotalExpense(defaultZero(balance.getTotalExpense()).add(amount).setScale(2, RoundingMode.HALF_UP));
        balance.setUpdatedTime(now);
        if (!accountBalancesService.updateById(balance)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户资金账户失败");
        }

        FundFlows flow = new FundFlows();
        flow.setId(SnowflakeIdUtil.nextFundFlowId());
        flow.setAccountId(accountId);
        flow.setAccountType(ACCOUNT_TYPE_USER);
        flow.setFlowType(FLOW_TYPE_EXPENSE);
        flow.setAmount(amount);
        flow.setBalanceBefore(balanceBefore);
        flow.setBalanceAfter(balanceAfter);
        flow.setBusinessType("REPAIR_ORDER_PREPAY");
        flow.setBusinessId(orderId);
        flow.setDescription("维修订单预付费用");
        flow.setRemark("orderNo=" + orderNo);
        flow.setCreatedTime(now);
        flow.setIsDelete(0);
        if (!fundFlowsService.save(flow)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存资金流水失败");
        }
    }

    */
    private String buildOrderNo(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            return "RO" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(100, 1000);
        }
        return "NO" + orderId.replace("RO", "");
    }

    private String resolveUploadMediaType(String mediaType, String mimeType, String originalFilename) {
        String normalizedType = trimToNull(mediaType);
        if (StringUtils.hasText(normalizedType)) {
            String lower = normalizedType.toLowerCase();
            if ("image".equals(lower) || "video".equals(lower)) {
                return lower;
            }
            throw new BusinessException(ErrorCode.PARAM_ERROR, "mediaType 仅支持 image 或 video");
        }

        String normalizedMime = trimToNull(mimeType);
        if (StringUtils.hasText(normalizedMime)) {
            String lowerMime = normalizedMime.toLowerCase();
            if (lowerMime.startsWith("image/")) {
                return "image";
            }
            if (lowerMime.startsWith("video/")) {
                return "video";
            }
        }

        String filename = trimToNull(originalFilename);
        if (StringUtils.hasText(filename)) {
            String lowerName = filename.toLowerCase();
            if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") || lowerName.endsWith(".webp")) {
                return "image";
            }
            if (lowerName.endsWith(".mp4") || lowerName.endsWith(".mov") || lowerName.endsWith(".avi") || lowerName.endsWith(".m4v")) {
                return "video";
            }
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "无法识别上传文件类型");
    }

    private String resolveUploadExtension(String originalFilename, String mimeType, String mediaType) {
        String filename = trimToNull(originalFilename);
        if (StringUtils.hasText(filename)) {
            int index = filename.lastIndexOf('.');
            if (index >= 0 && index < filename.length() - 1) {
                String extension = filename.substring(index);
                if (extension.length() <= 10) {
                    return extension;
                }
            }
        }

        String normalizedMime = trimToNull(mimeType);
        if (StringUtils.hasText(normalizedMime)) {
            String lowerMime = normalizedMime.toLowerCase();
            if ("image/png".equals(lowerMime)) {
                return ".png";
            }
            if ("image/webp".equals(lowerMime)) {
                return ".webp";
            }
            if ("image/gif".equals(lowerMime)) {
                return ".gif";
            }
            if ("video/quicktime".equals(lowerMime)) {
                return ".mov";
            }
            if ("video/webm".equals(lowerMime)) {
                return ".webm";
            }
            if (lowerMime.startsWith("video/")) {
                return ".mp4";
            }
        }

        return "video".equals(mediaType) ? ".mp4" : ".jpg";
    }

    private String resolveUploadMimeType(String mimeType, String mediaType) {
        String normalized = trimToNull(mimeType);
        if (StringUtils.hasText(normalized)) {
            return normalized;
        }
        return "video".equals(mediaType) ? "video/mp4" : "image/jpeg";
    }

    private String requireMediaUrl(String value) {
        String normalized = trimToNull(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "媒体地址不能为空");
        }
        return normalized;
    }

    private String resolveMediaName(String value, String fallback) {
        String normalized = trimToNull(value);
        return StringUtils.hasText(normalized) ? normalized : fallback;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BIG_DECIMAL_ZERO : value;
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return defaultZero(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String formatMoney(BigDecimal value) {
        return defaultZero(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatDecimal(BigDecimal value, int scale) {
        return defaultZero(value).setScale(scale, RoundingMode.HALF_UP).toPlainString();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private LoginUserInfo requireUser() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.USER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问下单服务");
        }
        return user;
    }

    private static class TechnicianOptionWrapper {
        private UserOrderFlowModel.TechnicianItem item;
        private BigDecimal distanceKm = BIG_DECIMAL_ZERO;
        private BigDecimal recommendScore = BIG_DECIMAL_ZERO;
    }

    private static class FeeCalcResult {
        private BigDecimal distanceKm = BIG_DECIMAL_ZERO;
        private BigDecimal doorFee = BIG_DECIMAL_ZERO;
        private BigDecimal distanceFee = BIG_DECIMAL_ZERO;
        private BigDecimal totalAmount = BIG_DECIMAL_ZERO;
        private BigDecimal baseRadiusKm = BIG_DECIMAL_ZERO;
        private BigDecimal distanceOverKm = BIG_DECIMAL_ZERO;
        private BigDecimal minVisitFeeSnapshot = BIG_DECIMAL_ZERO;
        private BigDecimal extraFeePerKmSnapshot = BIG_DECIMAL_ZERO;
        private Integer distanceCalcTypeSnapshot = 1;
        private Integer roundingRuleSnapshot = 1;

        private static FeeCalcResult zero() {
            return new FeeCalcResult();
        }
    }

}

