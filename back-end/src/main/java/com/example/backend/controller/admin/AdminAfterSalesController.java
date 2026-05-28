
package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.AfterSalesApplications;
import com.example.backend.entity.ConversationSessions;
import com.example.backend.entity.Images;
import com.example.backend.entity.OrderProgress;
import com.example.backend.entity.RepairOrderPayments;
import com.example.backend.entity.RepairOrders;
import com.example.backend.entity.ServiceCategories;
import com.example.backend.entity.ServiceTypes;
import com.example.backend.entity.SystemMessages;
import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.entity.UserAccounts;
import com.example.backend.entity.UserAddresses;
import com.example.backend.entity.Videos;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.admin.AdminAfterSalesDetailResponse;
import com.example.backend.model.admin.AdminAfterSalesListItemResponse;
import com.example.backend.model.admin.AdminAfterSalesMediaItemResponse;
import com.example.backend.model.admin.AdminAfterSalesProcessRequest;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.AfterSalesApplicationsService;
import com.example.backend.service.ConversationSessionsService;
import com.example.backend.service.ImagesService;
import com.example.backend.service.OrderDoorQrService;
import com.example.backend.service.OrderProgressService;
import com.example.backend.service.RepairOrderFundService;
import com.example.backend.service.RepairOrderPaymentsService;
import com.example.backend.service.RepairOrdersService;
import com.example.backend.service.ServiceCategoriesService;
import com.example.backend.service.ServiceTypesService;
import com.example.backend.service.SystemMessagesService;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.service.UserAccountsService;
import com.example.backend.service.UserAddressesService;
import com.example.backend.service.VideosService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/after-sales/requests")
public class AdminAfterSalesController {

    private static final String AFTER_SALES_BUSINESS_TYPE = "AFTER_SALES_APPLICATION";
    private static final int ORDER_TYPE_REPAIR = 1;
    private static final int SYSTEM_MESSAGE_FOR_WORKER = 2;
    private static final int SYSTEM_MESSAGE_TYPE_ORDER = 2;
    private static final int SYSTEM_MESSAGE_PRIORITY_HIGH = 1;
    private static final int AFTER_SALES_TYPE_REPAIR = 4;
    private static final int AFTER_SALES_STATUS_PENDING = 1;
    private static final int AFTER_SALES_STATUS_APPROVED = 2;
    private static final int AFTER_SALES_STATUS_REJECTED = 3;
    private static final int AFTER_SALES_STATUS_PROCESSING = 4;
    private static final int AFTER_SALES_STATUS_COMPLETED = 5;
    private static final int AFTER_SALES_STATUS_CANCELED = 6;
    private static final int OPERATOR_TYPE_ADMIN = 3;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final String ACTION_REFUND = "refund";
    private static final String ACTION_REJECT = "reject";
    private static final String WORKER_AFTER_SALES_PROCESSED_MESSAGE_TYPE = "ADMIN_AFTER_SALES_PROCESS_NOTIFY_WORKER";

    private final AfterSalesApplicationsService afterSalesApplicationsService;
    private final RepairOrdersService repairOrdersService;
    private final RepairOrderPaymentsService repairOrderPaymentsService;
    private final ServiceTypesService serviceTypesService;
    private final ServiceCategoriesService serviceCategoriesService;
    private final UserAccountsService userAccountsService;
    private final TechnicianAccountsService technicianAccountsService;
    private final UserAddressesService userAddressesService;
    private final ImagesService imagesService;
    private final VideosService videosService;
    private final OrderProgressService orderProgressService;
    private final RepairOrderFundService repairOrderFundService;
    private final OrderDoorQrService orderDoorQrService;
    private final ConversationSessionsService conversationSessionsService;
    private final SystemMessagesService systemMessagesService;

    public AdminAfterSalesController(
        AfterSalesApplicationsService afterSalesApplicationsService,
        RepairOrdersService repairOrdersService,
        RepairOrderPaymentsService repairOrderPaymentsService,
        ServiceTypesService serviceTypesService,
        ServiceCategoriesService serviceCategoriesService,
        UserAccountsService userAccountsService,
        TechnicianAccountsService technicianAccountsService,
        UserAddressesService userAddressesService,
        ImagesService imagesService,
        VideosService videosService,
        OrderProgressService orderProgressService,
        RepairOrderFundService repairOrderFundService,
        OrderDoorQrService orderDoorQrService,
        ConversationSessionsService conversationSessionsService,
        SystemMessagesService systemMessagesService
    ) {
        this.afterSalesApplicationsService = afterSalesApplicationsService;
        this.repairOrdersService = repairOrdersService;
        this.repairOrderPaymentsService = repairOrderPaymentsService;
        this.serviceTypesService = serviceTypesService;
        this.serviceCategoriesService = serviceCategoriesService;
        this.userAccountsService = userAccountsService;
        this.technicianAccountsService = technicianAccountsService;
        this.userAddressesService = userAddressesService;
        this.imagesService = imagesService;
        this.videosService = videosService;
        this.orderProgressService = orderProgressService;
        this.repairOrderFundService = repairOrderFundService;
        this.orderDoorQrService = orderDoorQrService;
        this.conversationSessionsService = conversationSessionsService;
        this.systemMessagesService = systemMessagesService;
    }

    @GetMapping
    public Result<Page<AdminAfterSalesListItemResponse>> listRequests(
        @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
        @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) Integer status
    ) {
        LoginUserInfo admin = requireAdmin();
        long currentPage = pageNum <= 0 ? 1 : pageNum;
        long currentSize = pageSize <= 0 ? 10 : pageSize;

        LambdaQueryWrapper<AfterSalesApplications> wrapper = new LambdaQueryWrapper<AfterSalesApplications>()
            .eq(AfterSalesApplications::getOrderType, ORDER_TYPE_REPAIR)
            .eq(AfterSalesApplications::getIsDelete, 0);

        // 门店管理员：仅查看本门店师傅的售后
        applyStoreFilter(admin, wrapper);
        if (StringUtils.hasText(keyword)) {
            String normalizedKeyword = keyword.trim();
            wrapper.and(q -> q.like(AfterSalesApplications::getOrderId, normalizedKeyword)
                .or().like(AfterSalesApplications::getReason, normalizedKeyword)
                .or().like(AfterSalesApplications::getDescription, normalizedKeyword));
        }
        if (status != null) {
            wrapper.eq(AfterSalesApplications::getStatus, status);
        }
        wrapper.orderByAsc(AfterSalesApplications::getStatus)
            .orderByDesc(AfterSalesApplications::getCreatedTime);

        Page<AfterSalesApplications> page = afterSalesApplicationsService.page(
            new Page<>(currentPage, currentSize),
            wrapper
        );
        Page<AdminAfterSalesListItemResponse> responsePage = new Page<>(
            page.getCurrent(),
            page.getSize(),
            page.getTotal()
        );
        responsePage.setRecords(buildListItems(page.getRecords()));
        return Result.success(responsePage);
    }

    @GetMapping("/{id}")
    public Result<AdminAfterSalesDetailResponse> getRequestDetail(@PathVariable("id") String id) {
        requireAdmin();
        return Result.success(buildDetailResponse(requireAfterSalesApplication(id)));
    }

    @PostMapping("/{id}/process")
    @Transactional(rollbackFor = Exception.class)
    public Result<AdminAfterSalesDetailResponse> processRequest(
        @PathVariable("id") String id,
        @RequestBody(required = false) AdminAfterSalesProcessRequest request
    ) {
        LoginUserInfo admin = requireAdmin();
        AfterSalesApplications application = requireAfterSalesApplication(id);
        if (safeInt(application.getStatus()) != AFTER_SALES_STATUS_PENDING) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前售后申请已处理");
        }

        String action = trimToNull(request == null ? null : request.getAction());
        String adminRemark = trimToNull(request == null ? null : request.getAdminRemark());
        if (!StringUtils.hasText(action)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择处理动作");
        }

        RepairOrders order = requireRepairOrder(application.getOrderId());
        long now = System.currentTimeMillis();
        if (ACTION_REFUND.equals(action)) {
            handleRefund(application, order, requireLatestPayment(order.getId()), admin, adminRemark, now);
        } else if (ACTION_REJECT.equals(action)) {
            handleReject(application, order, admin, adminRemark, now);
        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的处理动作");
        }

        return Result.success(buildDetailResponse(requireAfterSalesApplication(id)));
    }

    private void handleRefund(
        AfterSalesApplications application,
        RepairOrders order,
        RepairOrderPayments payment,
        LoginUserInfo admin,
        String adminRemark,
        long now
    ) {
        BigDecimal refundAmount = normalizeMoney(payment == null ? null : payment.getActualAmount());

        application.setStatus(AFTER_SALES_STATUS_COMPLETED);
        application.setAdminId(admin.getAccountId());
        application.setAdminRemark(adminRemark);
        application.setRefundAmount(refundAmount);
        application.setProcessedTime(now);
        application.setCompletedTime(now);
        application.setUpdatedTime(now);
        if (!afterSalesApplicationsService.updateById(application)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新售后申请失败");
        }

        order.setStatus(8);
        order.setPaymentStatus(3);
        order.setRefundAmount(refundAmount);
        order.setRefundReason(StringUtils.hasText(adminRemark) ? adminRemark : "管理员同意售后退款");
        order.setRefundTime(now);
        order.setUpdatedTime(now);
        if (order.getCompletionTime() == null) {
            order.setCompletionTime(now);
        }
        if (order.getEndTime() == null) {
            order.setEndTime(now);
        }
        if (!repairOrdersService.updateById(order)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新订单状态失败");
        }

        repairOrderFundService.refundOnOrderClosed(order, payment, order.getRefundReason(), now);
        orderDoorQrService.invalidateCurrentCodes(order.getId());
        closeConversationSession(order, now);
        saveAdminProgress(order, admin, 8, "管理员已同意售后退款，订单已关闭", now);
        notifyWorkerAfterSalesProcessed(application, order, true, adminRemark, now);
    }

    private void handleReject(
        AfterSalesApplications application,
        RepairOrders order,
        LoginUserInfo admin,
        String adminRemark,
        long now
    ) {
        application.setStatus(AFTER_SALES_STATUS_REJECTED);
        application.setAdminId(admin.getAccountId());
        application.setAdminRemark(adminRemark);
        application.setRefundAmount(ZERO);
        application.setProcessedTime(now);
        application.setCompletedTime(now);
        application.setUpdatedTime(now);
        if (!afterSalesApplicationsService.updateById(application)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新售后申请失败");
        }
        closeConversationSession(order, now);
        saveAdminProgress(order, admin, safeInt(order.getStatus()), "管理员已驳回售后申请", now);
        notifyWorkerAfterSalesProcessed(application, order, false, adminRemark, now);
    }
    private List<AdminAfterSalesListItemResponse> buildListItems(List<AfterSalesApplications> applications) {
        if (applications == null || applications.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, RepairOrders> orderMap = listOrderMap(applications);
        Map<String, UserAccounts> userMap = listUserMap(applications);
        Map<String, TechnicianAccounts> technicianMap = listTechnicianMap(new ArrayList<>(orderMap.values()));
        Map<String, ServiceTypes> serviceTypeMap = listServiceTypeMap(new ArrayList<>(orderMap.values()));
        Map<String, ServiceCategories> categoryMap = listCategoryMap(serviceTypeMap);

        List<AdminAfterSalesListItemResponse> items = new ArrayList<>();
        for (AfterSalesApplications application : applications) {
            RepairOrders order = orderMap.get(application.getOrderId());
            UserAccounts user = userMap.get(application.getAccountId());
            TechnicianAccounts technician = order == null ? null : technicianMap.get(order.getTechnicianAccountId());
            ServiceTypes serviceType = order == null ? null : serviceTypeMap.get(order.getServiceTypeId());
            ServiceCategories category = serviceType == null ? null : categoryMap.get(serviceType.getCategoryId());

            AdminAfterSalesListItemResponse item = new AdminAfterSalesListItemResponse();
            item.setId(application.getId());
            item.setOrderId(application.getOrderId());
            item.setOrderNo(order == null ? "" : safe(order.getOrderNo()));
            item.setStatus(application.getStatus());
            item.setStatusText(getAfterSalesStatusText(application.getStatus()));
            item.setApplicationType(application.getApplicationType());
            item.setApplicationTypeText(getAfterSalesApplicationTypeText(application.getApplicationType()));
            item.setReason(safe(application.getReason()));
            item.setUserId(application.getAccountId());
            item.setUserName(user == null ? "" : safe(user.getUsername()));
            item.setUserPhone(user == null ? "" : safe(user.getPhone()));
            item.setTechnicianId(order == null ? "" : safe(order.getTechnicianAccountId()));
            item.setTechnicianName(technician == null ? "" : safe(technician.getUsername()));
            item.setServiceTypeName(serviceType == null ? "" : safe(serviceType.getName()));
            item.setServiceCategoryName(category == null ? "" : safe(category.getName()));
            item.setCreatedTime(application.getCreatedTime());
            item.setUpdatedTime(application.getUpdatedTime());
            items.add(item);
        }
        return items;
    }

    private AdminAfterSalesDetailResponse buildDetailResponse(AfterSalesApplications application) {
        RepairOrders order = requireRepairOrder(application.getOrderId());
        RepairOrderPayments payment = requireLatestPayment(order.getId());
        ServiceTypes serviceType = requireServiceType(order.getServiceTypeId());
        ServiceCategories category = getCategory(serviceType == null ? null : serviceType.getCategoryId());
        UserAccounts user = getUserAccount(application.getAccountId());
        TechnicianAccounts technician = getTechnicianAccount(order.getTechnicianAccountId());
        UserAddresses address = getAddress(order.getServiceAddressId());

        AdminAfterSalesDetailResponse detail = new AdminAfterSalesDetailResponse();
        detail.setId(application.getId());
        detail.setOrderId(application.getOrderId());
        detail.setOrderNo(safe(order.getOrderNo()));
        detail.setStatus(application.getStatus());
        detail.setStatusText(getAfterSalesStatusText(application.getStatus()));
        detail.setApplicationType(application.getApplicationType());
        detail.setApplicationTypeText(getAfterSalesApplicationTypeText(application.getApplicationType()));
        detail.setReason(safe(application.getReason()));
        detail.setDescription(safe(application.getDescription()));
        detail.setRefundAmount(formatMoney(application.getRefundAmount()));
        detail.setAdminRemark(safe(application.getAdminRemark()));
        detail.setContactPhone(safe(application.getContactPhone()));
        detail.setContactAddress(safe(application.getContactAddress()));
        detail.setCreatedTime(application.getCreatedTime());
        detail.setUpdatedTime(application.getUpdatedTime());
        detail.setProcessedTime(application.getProcessedTime());
        detail.setCompletedTime(application.getCompletedTime());
        detail.setUserId(application.getAccountId());
        detail.setUserName(user == null ? "" : safe(user.getUsername()));
        detail.setUserPhone(user == null ? "" : safe(user.getPhone()));
        detail.setTechnicianId(safe(order.getTechnicianAccountId()));
        detail.setTechnicianName(technician == null ? "" : safe(technician.getUsername()));
        detail.setOrderStatus(order.getStatus());
        detail.setOrderStatusText(getOrderStatusText(order.getStatus()));
        detail.setPaymentStatusText(getPaymentStatusText(order.getPaymentStatus(), payment));
        detail.setServiceTypeName(serviceType == null ? "" : safe(serviceType.getName()));
        detail.setServiceCategoryName(category == null ? "" : safe(category.getName()));
        detail.setServiceModeText(getServiceModeText(serviceType == null ? null : serviceType.getType()));
        detail.setServiceAddress(buildAddressText(address));
        detail.setTotalAmount(formatMoney(payment == null ? null : payment.getTotalAmount()));
        detail.setPaidAmount(formatMoney(payment == null ? null : payment.getActualAmount()));
        detail.setCanProcess(safeInt(application.getStatus()) == AFTER_SALES_STATUS_PENDING);
        detail.setEvidenceImages(listAfterSalesImages(application.getId()));
        detail.setEvidenceVideos(listAfterSalesVideos(application.getId()));
        return detail;
    }

    private List<AdminAfterSalesMediaItemResponse> listAfterSalesImages(String applicationId) {
        if (!StringUtils.hasText(applicationId)) {
            return Collections.emptyList();
        }
        List<Images> images = imagesService.list(
            new LambdaQueryWrapper<Images>()
                .eq(Images::getBusinessType, AFTER_SALES_BUSINESS_TYPE)
                .eq(Images::getBusinessId, applicationId)
                .eq(Images::getIsDelete, 0)
                .orderByAsc(Images::getCreatedTime)
        );
        if (images == null || images.isEmpty()) {
            return Collections.emptyList();
        }

        List<AdminAfterSalesMediaItemResponse> items = new ArrayList<>();
        for (Images image : images) {
            AdminAfterSalesMediaItemResponse item = new AdminAfterSalesMediaItemResponse();
            item.setId(image.getId());
            item.setUrl(safe(image.getFileUrl()));
            item.setThumbnailUrl(safe(image.getFileUrl()));
            item.setName(StringUtils.hasText(image.getOriginalName()) ? image.getOriginalName() : safe(image.getFileName()));
            item.setMimeType(safe(image.getMimeType()));
            items.add(item);
        }
        return items;
    }

    private List<AdminAfterSalesMediaItemResponse> listAfterSalesVideos(String applicationId) {
        if (!StringUtils.hasText(applicationId)) {
            return Collections.emptyList();
        }
        List<Videos> videos = videosService.list(
            new LambdaQueryWrapper<Videos>()
                .eq(Videos::getBusinessType, AFTER_SALES_BUSINESS_TYPE)
                .eq(Videos::getBusinessId, applicationId)
                .eq(Videos::getIsDelete, 0)
                .orderByAsc(Videos::getCreatedTime)
        );
        if (videos == null || videos.isEmpty()) {
            return Collections.emptyList();
        }

        List<AdminAfterSalesMediaItemResponse> items = new ArrayList<>();
        for (Videos video : videos) {
            AdminAfterSalesMediaItemResponse item = new AdminAfterSalesMediaItemResponse();
            item.setId(video.getId());
            item.setUrl(safe(video.getFileUrl()));
            item.setThumbnailUrl(safe(video.getThumbnailUrl()));
            item.setName(StringUtils.hasText(video.getOriginalName()) ? video.getOriginalName() : safe(video.getFileName()));
            item.setMimeType(safe(video.getMimeType()));
            item.setDuration(video.getDuration());
            items.add(item);
        }
        return items;
    }

    private void saveAdminProgress(
        RepairOrders order,
        LoginUserInfo admin,
        Integer status,
        String description,
        long now
    ) {
        OrderProgress progress = new OrderProgress();
        progress.setId(SnowflakeIdUtil.nextOrderProgressId());
        progress.setOrderId(order.getId());
        progress.setStatus(status);
        progress.setStatusName(getOrderStatusText(status));
        progress.setDescription(description);
        progress.setOperatorId(admin.getAccountId());
        progress.setOperatorType(OPERATOR_TYPE_ADMIN);
        progress.setOperatorName("管理员");
        progress.setCreatedTime(now);
        progress.setIsDelete(0);
        if (!orderProgressService.save(progress)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存订单进度失败");
        }
    }

    private void ensureConversationSessionOpen(RepairOrders order, long now) {
        if (order == null
            || !StringUtils.hasText(order.getId())
            || !StringUtils.hasText(order.getAccountId())
            || !StringUtils.hasText(order.getTechnicianAccountId())) {
            return;
        }
        ConversationSessions session = findConversationSession(order);
        if (session == null) {
            session = new ConversationSessions();
            session.setId(SnowflakeIdUtil.nextConversationSessionId());
            session.setUserAccountId(order.getAccountId());
            session.setTechnicianAccountId(order.getTechnicianAccountId());
            session.setRepairOrderId(order.getId());
            session.setUserUnreadCount(0);
            session.setTechnicianUnreadCount(0);
            session.setStatus(1);
            session.setCreatedTime(now);
            session.setUpdatedTime(now);
            session.setVersion(0);
            session.setIsDelete(0);
            conversationSessionsService.save(session);
            return;
        }
        if (safeInt(session.getStatus()) != 1 || !order.getId().equals(session.getRepairOrderId())) {
            session.setUserAccountId(order.getAccountId());
            session.setTechnicianAccountId(order.getTechnicianAccountId());
            session.setRepairOrderId(order.getId());
            session.setStatus(1);
            session.setUpdatedTime(now);
            conversationSessionsService.updateById(session);
        }
    }

    private void closeConversationSession(RepairOrders order, long now) {
        ConversationSessions session = findConversationSession(order);
        if (session == null) {
            return;
        }
        String referenceOrderId = resolveConversationReferenceOrderId(order);
        session.setRepairOrderId(StringUtils.hasText(referenceOrderId) ? referenceOrderId : order.getId());
        session.setStatus(StringUtils.hasText(referenceOrderId) ? 1 : 2);
        session.setUpdatedTime(now);
        conversationSessionsService.updateById(session);
    }

    private ConversationSessions findConversationSession(RepairOrders order) {
        if (order == null
            || !StringUtils.hasText(order.getAccountId())
            || !StringUtils.hasText(order.getTechnicianAccountId())) {
            return null;
        }
        return conversationSessionsService.getOne(
            new LambdaQueryWrapper<ConversationSessions>()
                .eq(ConversationSessions::getUserAccountId, order.getAccountId())
                .eq(ConversationSessions::getTechnicianAccountId, order.getTechnicianAccountId())
                .eq(ConversationSessions::getIsDelete, 0)
                .orderByDesc(ConversationSessions::getUpdatedTime)
                .orderByDesc(ConversationSessions::getCreatedTime)
                .last("limit 1"),
            false
        );
    }

    private String resolveConversationReferenceOrderId(RepairOrders order) {
        if (order == null
            || !StringUtils.hasText(order.getAccountId())
            || !StringUtils.hasText(order.getTechnicianAccountId())) {
            return null;
        }
        RepairOrders activeOrder = repairOrdersService.getOne(
            new LambdaQueryWrapper<RepairOrders>()
                .eq(RepairOrders::getAccountId, order.getAccountId())
                .eq(RepairOrders::getTechnicianAccountId, order.getTechnicianAccountId())
                .eq(RepairOrders::getIsDelete, 0)
                .in(RepairOrders::getStatus, 2, 3, 4, 5)
                .orderByDesc(RepairOrders::getUpdatedTime)
                .orderByDesc(RepairOrders::getCreatedTime)
                .last("limit 1"),
            false
        );
        if (activeOrder != null) {
            return activeOrder.getId();
        }

        List<String> relatedOrderIds = repairOrdersService.list(
            new LambdaQueryWrapper<RepairOrders>()
                .eq(RepairOrders::getAccountId, order.getAccountId())
                .eq(RepairOrders::getTechnicianAccountId, order.getTechnicianAccountId())
                .eq(RepairOrders::getIsDelete, 0)
                .orderByDesc(RepairOrders::getUpdatedTime)
                .orderByDesc(RepairOrders::getCreatedTime)
        ).stream().map(RepairOrders::getId).filter(StringUtils::hasText).collect(Collectors.toList());
        if (relatedOrderIds.isEmpty()) {
            return null;
        }

        AfterSalesApplications activeApplication = afterSalesApplicationsService.getOne(
            new LambdaQueryWrapper<AfterSalesApplications>()
                .in(AfterSalesApplications::getOrderId, relatedOrderIds)
                .eq(AfterSalesApplications::getOrderType, ORDER_TYPE_REPAIR)
                .in(
                    AfterSalesApplications::getStatus,
                    AFTER_SALES_STATUS_PENDING,
                    AFTER_SALES_STATUS_APPROVED,
                    AFTER_SALES_STATUS_PROCESSING
                )
                .eq(AfterSalesApplications::getIsDelete, 0)
                .orderByDesc(AfterSalesApplications::getUpdatedTime)
                .orderByDesc(AfterSalesApplications::getCreatedTime)
                .last("limit 1"),
            false
        );
        return activeApplication == null ? null : activeApplication.getOrderId();
    }
    private int resolveReRepairStatus(Integer serviceMode) {
        return safeInt(serviceMode) == 3 ? 3 : 2;
    }

    private Map<String, RepairOrders> listOrderMap(List<AfterSalesApplications> applications) {
        if (applications == null || applications.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<String> orderIds = applications.stream()
            .map(AfterSalesApplications::getOrderId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return repairOrdersService.list(
            new LambdaQueryWrapper<RepairOrders>()
                .in(RepairOrders::getId, orderIds)
                .eq(RepairOrders::getIsDelete, 0)
        ).stream().collect(Collectors.toMap(RepairOrders::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    private Map<String, UserAccounts> listUserMap(List<AfterSalesApplications> applications) {
        if (applications == null || applications.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<String> userIds = applications.stream()
            .map(AfterSalesApplications::getAccountId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userAccountsService.list(
            new LambdaQueryWrapper<UserAccounts>()
                .in(UserAccounts::getId, userIds)
                .eq(UserAccounts::getIsDelete, 0)
        ).stream().collect(Collectors.toMap(UserAccounts::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    private Map<String, TechnicianAccounts> listTechnicianMap(List<RepairOrders> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<String> technicianIds = orders.stream()
            .map(RepairOrders::getTechnicianAccountId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        if (technicianIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return technicianAccountsService.list(
            new LambdaQueryWrapper<TechnicianAccounts>()
                .in(TechnicianAccounts::getId, technicianIds)
                .eq(TechnicianAccounts::getIsDelete, 0)
        ).stream().collect(Collectors.toMap(TechnicianAccounts::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    private Map<String, ServiceTypes> listServiceTypeMap(List<RepairOrders> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<String> serviceTypeIds = orders.stream()
            .map(RepairOrders::getServiceTypeId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        if (serviceTypeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return serviceTypesService.list(
            new LambdaQueryWrapper<ServiceTypes>()
                .in(ServiceTypes::getId, serviceTypeIds)
                .eq(ServiceTypes::getIsDelete, 0)
        ).stream().collect(Collectors.toMap(ServiceTypes::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    private Map<String, ServiceCategories> listCategoryMap(Map<String, ServiceTypes> serviceTypeMap) {
        if (serviceTypeMap == null || serviceTypeMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<String> categoryIds = serviceTypeMap.values().stream()
            .map(ServiceTypes::getCategoryId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return serviceCategoriesService.list(
            new LambdaQueryWrapper<ServiceCategories>()
                .in(ServiceCategories::getId, categoryIds)
                .eq(ServiceCategories::getIsDelete, 0)
        ).stream().collect(Collectors.toMap(ServiceCategories::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    private AfterSalesApplications requireAfterSalesApplication(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "售后申请ID不能为空");
        }
        AfterSalesApplications application = afterSalesApplicationsService.getOne(
            new LambdaQueryWrapper<AfterSalesApplications>()
                .eq(AfterSalesApplications::getId, id)
                .eq(AfterSalesApplications::getOrderType, ORDER_TYPE_REPAIR)
                .eq(AfterSalesApplications::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (application == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "售后申请不存在");
        }
        return application;
    }

    private RepairOrders requireRepairOrder(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单ID不能为空");
        }
        RepairOrders order = repairOrdersService.getOne(
            new LambdaQueryWrapper<RepairOrders>()
                .eq(RepairOrders::getId, orderId)
                .eq(RepairOrders::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private RepairOrderPayments requireLatestPayment(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单ID不能为空");
        }
        RepairOrderPayments payment = repairOrderPaymentsService.getOne(
            new LambdaQueryWrapper<RepairOrderPayments>()
                .eq(RepairOrderPayments::getRepairOrderId, orderId)
                .eq(RepairOrderPayments::getIsDelete, 0)
                .orderByDesc(RepairOrderPayments::getCreatedTime)
                .last("limit 1"),
            false
        );
        if (payment == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "订单支付信息不存在");
        }
        return payment;
    }

    private ServiceTypes requireServiceType(String serviceTypeId) {
        if (!StringUtils.hasText(serviceTypeId)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "订单服务类型不存在");
        }
        ServiceTypes serviceType = serviceTypesService.getById(serviceTypeId);
        if (serviceType == null || safeInt(serviceType.getIsDelete()) == 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "订单服务类型不存在");
        }
        return serviceType;
    }

    private ServiceCategories getCategory(String categoryId) {
        if (!StringUtils.hasText(categoryId)) {
            return null;
        }
        ServiceCategories category = serviceCategoriesService.getById(categoryId);
        if (category == null || safeInt(category.getIsDelete()) == 1) {
            return null;
        }
        return category;
    }

    private UserAccounts getUserAccount(String accountId) {
        if (!StringUtils.hasText(accountId)) {
            return null;
        }
        UserAccounts account = userAccountsService.getById(accountId);
        if (account == null || safeInt(account.getIsDelete()) == 1) {
            return null;
        }
        return account;
    }

    private TechnicianAccounts getTechnicianAccount(String accountId) {
        if (!StringUtils.hasText(accountId)) {
            return null;
        }
        TechnicianAccounts account = technicianAccountsService.getById(accountId);
        if (account == null || safeInt(account.getIsDelete()) == 1) {
            return null;
        }
        return account;
    }

    private UserAddresses getAddress(String addressId) {
        if (!StringUtils.hasText(addressId)) {
            return null;
        }
        UserAddresses address = userAddressesService.getById(addressId);
        if (address == null || safeInt(address.getIsDelete()) == 1) {
            return null;
        }
        return address;
    }

    private String buildAddressText(UserAddresses address) {
        if (address == null) {
            return "";
        }
        return safe(address.getProvince())
            + safe(address.getCity())
            + safe(address.getDistrict())
            + safe(address.getStreet())
            + safe(address.getDetailedAddress());
    }
    private String getAfterSalesApplicationTypeText(Integer applicationType) {
        int value = safeInt(applicationType);
        if (value == 1) {
            return "退款";
        }
        if (value == 2) {
            return "退货";
        }
        if (value == 3) {
            return "换货";
        }
        if (value == AFTER_SALES_TYPE_REPAIR) {
            return "售后";
        }
        return "售后";
    }

    private String getAfterSalesStatusText(Integer status) {
        int value = safeInt(status);
        if (value == AFTER_SALES_STATUS_PENDING) {
            return "待审核";
        }
        if (value == AFTER_SALES_STATUS_APPROVED) {
            return "审核通过";
        }
        if (value == AFTER_SALES_STATUS_REJECTED) {
            return "审核拒绝";
        }
        if (value == AFTER_SALES_STATUS_PROCESSING) {
            return "处理中";
        }
        if (value == AFTER_SALES_STATUS_COMPLETED) {
            return "已完成";
        }
        if (value == AFTER_SALES_STATUS_CANCELED) {
            return "已取消";
        }
        return "未知";
    }

    private String getOrderStatusText(Integer status) {
        int value = safeInt(status);
        if (value == 1) {
            return "待接单";
        }
        if (value == 2) {
            return "待上门";
        }
        if (value == 3) {
            return "待检查";
        }
        if (value == 4) {
            return "待支付";
        }
        if (value == 5) {
            return "服务中";
        }
        if (value == 6) {
            return "已完成";
        }
        if (value == 7) {
            return "已取消";
        }
        if (value == 8) {
            return "已退款";
        }
        return "未知状态";
    }

    private String getPaymentStatusText(Integer paymentStatus, RepairOrderPayments payment) {
        int value = safeInt(paymentStatus);
        if (value == 2) {
            if (isPrepaidOnly(payment)) {
                return "已预付";
            }
            if (isFullyPaid(payment)) {
                return "已支付";
            }
            return "待补尾款";
        }
        if (value == 1) {
            return "待支付";
        }
        if (value == 3) {
            return "已退款";
        }
        return "未知";
    }

    private String getServiceModeText(Integer serviceMode) {
        int value = safeInt(serviceMode);
        if (value == 1) {
            return "上门维修";
        }
        if (value == 2) {
            return "上门安装";
        }
        if (value == 3) {
            return "线下维修";
        }
        return "未知服务";
    }

    private boolean isPrepaidOnly(RepairOrderPayments payment) {
        if (payment == null) {
            return false;
        }
        return isZero(payment.getServiceFee())
            && isZero(payment.getMaterialFee())
            && isZero(payment.getOvertimeFee());
    }

    private boolean isFullyPaid(RepairOrderPayments payment) {
        if (payment == null) {
            return false;
        }
        BigDecimal totalAmount = payment.getTotalAmount();
        BigDecimal actualAmount = payment.getActualAmount();
        if (totalAmount == null || actualAmount == null) {
            return false;
        }
        return actualAmount.compareTo(totalAmount) >= 0;
    }

    private boolean isZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String formatMoney(BigDecimal value) {
        return normalizeMoney(value).toPlainString();
    }

    private void notifyWorkerAfterSalesProcessed(
        AfterSalesApplications application,
        RepairOrders order,
        boolean approved,
        String adminRemark,
        long now
    ) {
        if (order == null || !StringUtils.hasText(order.getTechnicianAccountId())) {
            return;
        }
        ServiceTypes serviceType = StringUtils.hasText(order.getServiceTypeId())
            ? serviceTypesService.getById(order.getServiceTypeId())
            : null;
        String serviceName = serviceType == null ? "" : safe(serviceType.getName());
        String resultText = approved ? "已同意退款" : "已驳回申请";
        String title = approved ? "售后结果：同意退款" : "售后结果：已驳回";
        StringBuilder content = new StringBuilder()
            .append("订单").append(safe(order.getOrderNo()))
            .append("的售后申请")
            .append(resultText);
        if (StringUtils.hasText(serviceName)) {
            content.append("，服务项目：").append(serviceName);
        }
        if (StringUtils.hasText(application == null ? null : application.getReason())) {
            content.append("，售后原因：").append(application.getReason());
        }
        if (StringUtils.hasText(adminRemark)) {
            content.append("，处理备注：").append(adminRemark);
        }
        saveWorkerSystemMessage(
            order.getTechnicianAccountId(),
            title,
            content.toString(),
            WORKER_AFTER_SALES_PROCESSED_MESSAGE_TYPE,
            application == null ? order.getId() : application.getId(),
            now
        );
    }

    private void saveWorkerSystemMessage(
        String workerId,
        String title,
        String content,
        String businessType,
        String businessId,
        long now
    ) {
        if (!StringUtils.hasText(workerId) || !StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            return;
        }
        SystemMessages message = new SystemMessages();
        message.setId(SnowflakeIdUtil.nextSystemMessageId());
        message.setReceiverId(workerId);
        message.setReceiverType(SYSTEM_MESSAGE_FOR_WORKER);
        message.setTitle(title);
        message.setContent(content);
        message.setMessageType(SYSTEM_MESSAGE_TYPE_ORDER);
        message.setBusinessType(businessType);
        message.setBusinessId(businessId);
        message.setPriority(SYSTEM_MESSAGE_PRIORITY_HIGH);
        message.setIsRead(0);
        message.setCreatedTime(now);
        message.setUpdatedTime(now);
        message.setVersion(0);
        message.setIsDelete(0);
        if (!systemMessagesService.save(message)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存维修师傅售后处理消息失败");
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 门店管理员过滤：仅查看本门店师傅的售后申请
     */
    private void applyStoreFilter(LoginUserInfo admin, LambdaQueryWrapper<AfterSalesApplications> wrapper) {
        if (admin == null || !admin.isStoreAdmin() || !StringUtils.hasText(admin.getStoreId())) {
            return;
        }
        List<TechnicianAccounts> storeTechs = technicianAccountsService.list(
            new LambdaQueryWrapper<TechnicianAccounts>()
                .eq(TechnicianAccounts::getStoreId, admin.getStoreId())
                .eq(TechnicianAccounts::getIsDelete, 0)
        );
        if (storeTechs.isEmpty()) {
            wrapper.eq(AfterSalesApplications::getId, "-1");
            return;
        }
        Set<String> techIds = storeTechs.stream().map(TechnicianAccounts::getId).collect(Collectors.toSet());
        List<RepairOrders> orders = repairOrdersService.list(
            new LambdaQueryWrapper<RepairOrders>()
                .in(RepairOrders::getTechnicianAccountId, techIds)
                .eq(RepairOrders::getIsDelete, 0)
        );
        if (orders.isEmpty()) {
            wrapper.eq(AfterSalesApplications::getId, "-1");
            return;
        }
        Set<String> orderIds = orders.stream().map(RepairOrders::getId).collect(Collectors.toSet());
        wrapper.in(AfterSalesApplications::getOrderId, orderIds);
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
}
