package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.Announcements;
import com.example.backend.entity.Images;
import com.example.backend.entity.RepairOrders;
import com.example.backend.entity.ServiceCategories;
import com.example.backend.entity.ServiceTypes;
import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.entity.TechnicianSkills;
import com.example.backend.entity.UserFollowTechnicians;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.user.UserHomePrivateResponse;
import com.example.backend.model.user.UserHomePublicResponse;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.AnnouncementsService;
import com.example.backend.service.ImagesService;
import com.example.backend.service.RepairOrdersService;
import com.example.backend.service.ServiceCategoriesService;
import com.example.backend.service.ServiceTypesService;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.service.TechnicianSkillsService;
import com.example.backend.service.UserFollowTechniciansService;
import com.example.backend.service.UserHomeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserHomeServiceImpl implements UserHomeService {

    private static final int ANNOUNCEMENT_CHANNEL_BANNER = 1;
    private static final int ANNOUNCEMENT_CHANNEL_NOTICE = 2;
    private static final int ANNOUNCEMENT_CONTENT_TYPE_IMAGE = 1;

    private static final String ANNOUNCEMENT_IMAGE_BUSINESS_TYPE = "ANNOUNCEMENT";
    private static final String SERVICE_CATEGORY_IMAGE_BUSINESS_TYPE = "SERVERCATEGORY";
    private static final String AVATAR_BUSINESS_TYPE = "AVATAR";

    private final AnnouncementsService announcementsService;
    private final ImagesService imagesService;
    private final ServiceCategoriesService serviceCategoriesService;
    private final ServiceTypesService serviceTypesService;
    private final RepairOrdersService repairOrdersService;
    private final UserFollowTechniciansService userFollowTechniciansService;
    private final TechnicianAccountsService technicianAccountsService;
    private final TechnicianSkillsService technicianSkillsService;

    public UserHomeServiceImpl(
        AnnouncementsService announcementsService,
        ImagesService imagesService,
        ServiceCategoriesService serviceCategoriesService,
        ServiceTypesService serviceTypesService,
        RepairOrdersService repairOrdersService,
        UserFollowTechniciansService userFollowTechniciansService,
        TechnicianAccountsService technicianAccountsService,
        TechnicianSkillsService technicianSkillsService
    ) {
        this.announcementsService = announcementsService;
        this.imagesService = imagesService;
        this.serviceCategoriesService = serviceCategoriesService;
        this.serviceTypesService = serviceTypesService;
        this.repairOrdersService = repairOrdersService;
        this.userFollowTechniciansService = userFollowTechniciansService;
        this.technicianAccountsService = technicianAccountsService;
        this.technicianSkillsService = technicianSkillsService;
    }

    @Override
    public UserHomePublicResponse getPublicHomeData() {
        UserHomePublicResponse response = new UserHomePublicResponse();
        long now = System.currentTimeMillis();

        List<Announcements> banners = listActiveAnnouncements(ANNOUNCEMENT_CHANNEL_BANNER, now, 8);
        Map<String, String> bannerImageMap = loadLatestImageUrlMap(
            banners.stream()
                .filter(item -> Objects.equals(item.getContentType(), ANNOUNCEMENT_CONTENT_TYPE_IMAGE))
                .map(Announcements::getId)
                .collect(Collectors.toList()),
            ANNOUNCEMENT_IMAGE_BUSINESS_TYPE
        );

        List<UserHomePublicResponse.BannerItem> bannerItems = new ArrayList<>();
        for (Announcements banner : banners) {
            boolean isImageBanner = Objects.equals(banner.getContentType(), ANNOUNCEMENT_CONTENT_TYPE_IMAGE);
            String imageUrl = bannerImageMap.get(banner.getId());
            if (isImageBanner && !StringUtils.hasText(imageUrl)) {
                continue;
            }

            UserHomePublicResponse.BannerItem item = new UserHomePublicResponse.BannerItem();
            item.setId(banner.getId());
            item.setContentType(banner.getContentType());
            item.setTag(isImageBanner ? "图片公告" : "文字公告");
            item.setImageUrl(imageUrl);
            item.setEmoji(banner.getEmoji());

            if (isImageBanner) {
                item.setTitle("");
                item.setSubtitle("");
            } else {
                item.setTitle(defaultText(banner.getTitle(), "平台公告"));
                item.setSubtitle(defaultText(firstNonBlank(banner.getSubtitle(), banner.getContent()), ""));
            }
            bannerItems.add(item);
        }
        response.setBanners(bannerItems);

        List<Announcements> notices = listActiveAnnouncements(ANNOUNCEMENT_CHANNEL_NOTICE, now, 10);
        List<UserHomePublicResponse.NoticeItem> noticeItems = new ArrayList<>();
        for (Announcements notice : notices) {
            UserHomePublicResponse.NoticeItem item = new UserHomePublicResponse.NoticeItem();
            item.setId(notice.getId());
            item.setText(buildNoticeText(notice));
            noticeItems.add(item);
        }
        response.setNotices(noticeItems);

        response.setHotCategories(buildHotCategories());
        return response;
    }

    @Override
    public UserHomePrivateResponse getCurrentUserPrivateHomeData() {
        LoginUserInfo loginUserInfo = requireCurrentUser();
        String accountId = loginUserInfo.getAccountId();
        UserHomePrivateResponse response = new UserHomePrivateResponse();

        List<UserHomePrivateResponse.OrderSummaryItem> summaryItems = new ArrayList<>();
        summaryItems.add(buildSummaryItem("waiting", "待接单", countOrders(accountId, 1)));
        summaryItems.add(buildSummaryItem("processing", "进行中", countOrders(accountId, 2, 3, 5)));
        summaryItems.add(buildSummaryItem("to-pay", "待支付", countOrders(accountId, 4)));
        summaryItems.add(buildSummaryItem("finished", "已完成", countOrders(accountId, 6)));
        response.setOrderSummary(summaryItems);

        RepairOrders latestOrder = findLatestHomeOrder(accountId);
        response.setLatestOrder(buildLatestOrder(latestOrder));
        response.setFollowedWorkers(buildFollowedWorkers(accountId));
        return response;
    }

    private RepairOrders findLatestHomeOrder(String accountId) {
        RepairOrders acceptedOrder = repairOrdersService.getOne(
            new LambdaQueryWrapper<RepairOrders>()
                .eq(RepairOrders::getAccountId, accountId)
                .eq(RepairOrders::getIsDelete, 0)
                .in(RepairOrders::getStatus, 2, 3, 4, 5)
                .orderByDesc(RepairOrders::getUpdatedTime)
                .orderByDesc(RepairOrders::getCreatedTime)
                .last("limit 1"),
            false
        );
        if (acceptedOrder != null) {
            return acceptedOrder;
        }

        return repairOrdersService.getOne(
            new LambdaQueryWrapper<RepairOrders>()
                .eq(RepairOrders::getAccountId, accountId)
                .eq(RepairOrders::getIsDelete, 0)
                .eq(RepairOrders::getStatus, 1)
                .orderByDesc(RepairOrders::getUpdatedTime)
                .orderByDesc(RepairOrders::getCreatedTime)
                .last("limit 1"),
            false
        );
    }

    private List<Announcements> listActiveAnnouncements(int channel, long now, int limit) {
        return announcementsService.list(
            new LambdaQueryWrapper<Announcements>()
                .eq(Announcements::getChannel, channel)
                .eq(Announcements::getIsActive, 1)
                .and(wrapper -> wrapper.isNull(Announcements::getStartTime).or().le(Announcements::getStartTime, now))
                .and(wrapper -> wrapper.isNull(Announcements::getEndTime).or().ge(Announcements::getEndTime, now))
                .orderByAsc(Announcements::getSortOrder)
                .orderByDesc(Announcements::getCreatedTime)
                .last("limit " + limit)
        );
    }

    private List<UserHomePublicResponse.HotCategoryItem> buildHotCategories() {
        List<ServiceCategories> categories = serviceCategoriesService.list(
            new LambdaQueryWrapper<ServiceCategories>()
                .eq(ServiceCategories::getLevel, 3)
                .eq(ServiceCategories::getIsActive, 1)
                .orderByAsc(ServiceCategories::getSortOrder)
                .orderByDesc(ServiceCategories::getCreatedTime)
                .last("limit 12")
        );
        if (categories.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> categoryIds = categories.stream().map(ServiceCategories::getId).collect(Collectors.toList());
        Map<String, String> categoryIconMap = loadLatestImageUrlMap(categoryIds, SERVICE_CATEGORY_IMAGE_BUSINESS_TYPE);

        List<UserHomePublicResponse.HotCategoryItem> result = new ArrayList<>();
        for (ServiceCategories category : categories) {
            UserHomePublicResponse.HotCategoryItem item = new UserHomePublicResponse.HotCategoryItem();
            item.setId(category.getId());
            item.setName(category.getName());
            item.setDesc(defaultText(category.getDescription(), "专业维修服务"));
            item.setIconUrl(categoryIconMap.get(category.getId()));
            result.add(item);
        }
        return result;
    }

    private List<UserHomePrivateResponse.FollowedWorkerItem> buildFollowedWorkers(String accountId) {
        List<UserFollowTechnicians> follows = userFollowTechniciansService.list(
            new LambdaQueryWrapper<UserFollowTechnicians>()
                .eq(UserFollowTechnicians::getAccountId, accountId)
                .orderByDesc(UserFollowTechnicians::getCreatedTime)
                .last("limit 20")
        );
        if (follows.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> technicianIds = follows.stream()
            .map(UserFollowTechnicians::getTechnicianAccountId)
            .distinct()
            .collect(Collectors.toList());

        List<TechnicianAccounts> technicians = technicianAccountsService.list(
            new LambdaQueryWrapper<TechnicianAccounts>()
                .in(TechnicianAccounts::getId, technicianIds)
        );
        Map<String, TechnicianAccounts> technicianMap = technicians.stream()
            .collect(Collectors.toMap(TechnicianAccounts::getId, item -> item, (a, b) -> a));

        List<TechnicianSkills> skills = technicianSkillsService.list(
            new LambdaQueryWrapper<TechnicianSkills>()
                .in(TechnicianSkills::getTechnicianAccountId, technicianIds)
                .eq(TechnicianSkills::getIsActive, 1)
                .orderByDesc(TechnicianSkills::getSkillLevel)
                .orderByDesc(TechnicianSkills::getCreatedTime)
        );
        Map<String, String> technicianServiceTypeIdMap = new LinkedHashMap<>();
        for (TechnicianSkills skill : skills) {
            technicianServiceTypeIdMap.putIfAbsent(skill.getTechnicianAccountId(), skill.getServiceTypeId());
        }

        List<String> serviceTypeIds = technicianServiceTypeIdMap.values().stream()
            .filter(StringUtils::hasText)
            .distinct()
            .collect(Collectors.toList());
        Map<String, String> serviceTypeNameMap = new HashMap<>();
        if (!serviceTypeIds.isEmpty()) {
            List<ServiceTypes> serviceTypes = serviceTypesService.list(
                new LambdaQueryWrapper<ServiceTypes>()
                    .in(ServiceTypes::getId, serviceTypeIds)
            );
            for (ServiceTypes serviceType : serviceTypes) {
                serviceTypeNameMap.put(serviceType.getId(), serviceType.getName());
            }
        }

        Map<String, String> avatarMap = loadLatestImageUrlMap(technicianIds, AVATAR_BUSINESS_TYPE);

        List<UserHomePrivateResponse.FollowedWorkerItem> result = new ArrayList<>();
        for (UserFollowTechnicians follow : follows) {
            TechnicianAccounts technician = technicianMap.get(follow.getTechnicianAccountId());
            if (technician == null) {
                continue;
            }

            UserHomePrivateResponse.FollowedWorkerItem item = new UserHomePrivateResponse.FollowedWorkerItem();
            item.setId(technician.getId());
            item.setName(defaultText(technician.getUsername(), "维修师傅"));
            item.setInitial(item.getName().substring(0, 1));

            String serviceTypeId = technicianServiceTypeIdMap.get(technician.getId());
            String serviceTypeName = serviceTypeNameMap.get(serviceTypeId);
            item.setSkill(defaultText(serviceTypeName, "电器维修"));

            item.setScore(formatScore(technician.getRating()));
            item.setAccountStatus(technician.getAccountStatus());
            item.setWorkStatus(technician.getWorkStatus());
            item.setAvatarUrl(avatarMap.get(technician.getId()));
            fillWorkerStatusText(item, technician.getAccountStatus(), technician.getWorkStatus());
            result.add(item);
        }
        return result;
    }

    private UserHomePrivateResponse.LatestOrder buildLatestOrder(RepairOrders order) {
        if (order == null) {
            return null;
        }

        UserHomePrivateResponse.LatestOrder latestOrder = new UserHomePrivateResponse.LatestOrder();
        latestOrder.setOrderId(order.getId());
        latestOrder.setOrderNo(order.getOrderNo());
        latestOrder.setStatusText(getOrderStatusText(order.getStatus()));
        latestOrder.setStepActive(getOrderStepActive(order.getStatus()));

        String appliance = joinWithSpace(order.getApplianceBrand(), order.getApplianceModel());
        if (!StringUtils.hasText(appliance) && StringUtils.hasText(order.getServiceTypeId())) {
            ServiceTypes serviceType = serviceTypesService.getById(order.getServiceTypeId());
            if (serviceType != null) {
                appliance = serviceType.getName();
            }
        }
        latestOrder.setAppliance(defaultText(appliance, "维修订单"));

        List<UserHomePrivateResponse.StepItem> steps = new ArrayList<>();
        for (String stepText : Arrays.asList(
            "待接单",
            "待上门",
            "待检查",
            "待支付",
            "服务中",
            "已完成"
        )) {
            UserHomePrivateResponse.StepItem stepItem = new UserHomePrivateResponse.StepItem();
            stepItem.setText(stepText);
            steps.add(stepItem);
        }
        latestOrder.setSteps(steps);
        return latestOrder;
    }

    private UserHomePrivateResponse.OrderSummaryItem buildSummaryItem(String key, String label, int count) {
        UserHomePrivateResponse.OrderSummaryItem item = new UserHomePrivateResponse.OrderSummaryItem();
        item.setKey(key);
        item.setLabel(label);
        item.setCount(count);
        return item;
    }

    private int countOrders(String accountId, Integer... statuses) {
        LambdaQueryWrapper<RepairOrders> wrapper = new LambdaQueryWrapper<RepairOrders>()
            .eq(RepairOrders::getAccountId, accountId)
            .eq(RepairOrders::getIsDelete, 0);
        if (statuses != null && statuses.length == 1) {
            wrapper.eq(RepairOrders::getStatus, statuses[0]);
        } else if (statuses != null && statuses.length > 1) {
            wrapper.in(RepairOrders::getStatus, Arrays.asList(statuses));
        }
        Long count = repairOrdersService.count(wrapper);
        return count == null ? 0 : count.intValue();
    }

    private Map<String, String> loadLatestImageUrlMap(List<String> businessIds, String businessType) {
        if (businessIds == null || businessIds.isEmpty()) {
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
            if (!result.containsKey(image.getBusinessId()) && StringUtils.hasText(image.getFileUrl())) {
                result.put(image.getBusinessId(), image.getFileUrl());
            }
        }
        return result;
    }

    private String buildNoticeText(Announcements notice) {
        String title = defaultText(notice.getTitle(), "");
        String content = defaultText(firstNonBlank(notice.getContent(), notice.getSubtitle()), "");
        if (!StringUtils.hasText(title)) {
            return content;
        }
        if (!StringUtils.hasText(content)) {
            return title;
        }
        return title + "：" + content;
    }

    private void fillWorkerStatusText(UserHomePrivateResponse.FollowedWorkerItem item, Integer accountStatus, Integer workStatus) {
        if (!Objects.equals(accountStatus, 1)) {
            if (Objects.equals(accountStatus, 2)) {
                item.setStatusText("未认证");
                item.setStatusType("warning");
                return;
            }
            if (Objects.equals(accountStatus, 3)) {
                item.setStatusText("已冻结");
                item.setStatusType("danger");
                return;
            }
            if (Objects.equals(accountStatus, 4)) {
                item.setStatusText("已离职");
                item.setStatusType("primary");
                return;
            }
            item.setStatusText("状态未知");
            item.setStatusType("primary");
            return;
        }

        if (Objects.equals(workStatus, 1)) {
            item.setStatusText("在线接单");
            item.setStatusType("success");
            return;
        }
        if (Objects.equals(workStatus, 2)) {
            item.setStatusText("忙碌中");
            item.setStatusType("warning");
            return;
        }
        if (Objects.equals(workStatus, 3)) {
            item.setStatusText("休息中");
            item.setStatusType("primary");
            return;
        }
        item.setStatusText("离线");
        item.setStatusType("primary");
    }

    private String getOrderStatusText(Integer status) {
        if (Objects.equals(status, 1)) {
            return "待接单";
        }
        if (Objects.equals(status, 2)) {
            return "待上门";
        }
        if (Objects.equals(status, 3)) {
            return "待检查";
        }
        if (Objects.equals(status, 4)) {
            return "待支付";
        }
        if (Objects.equals(status, 5)) {
            return "服务中";
        }
        if (Objects.equals(status, 6)) {
            return "已完成";
        }
        if (Objects.equals(status, 7)) {
            return "已取消";
        }
        if (Objects.equals(status, 8)) {
            return "已退款";
        }
        return "未知状态";
    }

    private int getOrderStepActive(Integer status) {
        if (Objects.equals(status, 1)) {
            return 0;
        }
        if (Objects.equals(status, 2)) {
            return 1;
        }
        if (Objects.equals(status, 3)) {
            return 2;
        }
        if (Objects.equals(status, 4)) {
            return 3;
        }
        if (Objects.equals(status, 5)) {
            return 4;
        }
        if (Objects.equals(status, 6) || Objects.equals(status, 8)) {
            return 5;
        }
        return 0;
    }

    private LoginUserInfo requireCurrentUser() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.USER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问用户首页数据");
        }
        return user;
    }

    private String formatScore(BigDecimal score) {
        if (score == null) {
            return "--";
        }
        return score.stripTrailingZeros().toPlainString();
    }

    private static String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        return second;
    }

    private static String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private static String joinWithSpace(String first, String second) {
        String a = StringUtils.hasText(first) ? first.trim() : "";
        String b = StringUtils.hasText(second) ? second.trim() : "";
        if (!StringUtils.hasText(a) && !StringUtils.hasText(b)) {
            return "";
        }
        if (!StringUtils.hasText(a)) {
            return b;
        }
        if (!StringUtils.hasText(b)) {
            return a;
        }
        return a + " " + b;
    }
}
