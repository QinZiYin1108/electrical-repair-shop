package com.example.backend.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.AfterSalesApplications;
import com.example.backend.entity.ConversationSessions;
import com.example.backend.entity.FaultPhenomena;
import com.example.backend.entity.Images;
import com.example.backend.entity.OrderProgress;
import com.example.backend.entity.OrderDoorQrCodes;
import com.example.backend.entity.PaymentRecords;
import com.example.backend.entity.RepairOrderFaults;
import com.example.backend.entity.RepairOrderPayments;
import com.example.backend.entity.RepairOrders;
import com.example.backend.entity.Reviews;
import com.example.backend.entity.ServiceCategories;
import com.example.backend.entity.ServiceTypes;
import com.example.backend.entity.SystemMessages;
import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.entity.UserAddresses;
import com.example.backend.entity.Videos;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.user.UserAfterSalesApplicationDetailResponse;
import com.example.backend.model.user.UserAfterSalesApplicationSummary;
import com.example.backend.model.user.UserAfterSalesDetailResponse;
import com.example.backend.model.user.UserAfterSalesSubmitMediaItem;
import com.example.backend.model.user.UserOrderDetailResponse;
import com.example.backend.model.user.UserOrderDoorQrResponse;
import com.example.backend.model.user.UserOrderFaultItemResponse;
import com.example.backend.model.user.UserOrderFlowModel;
import com.example.backend.model.user.UserOrderListItemResponse;
import com.example.backend.model.user.UserOrderMediaItemResponse;
import com.example.backend.model.user.UserOrderProgressItemResponse;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.AfterSalesApplicationsService;
import com.example.backend.service.ConversationSessionsService;
import com.example.backend.service.FaultPhenomenaService;
import com.example.backend.service.ImagesService;
import com.example.backend.service.OrderProgressService;
import com.example.backend.service.OrderDoorQrService;
import com.example.backend.service.PaymentRecordsService;
import com.example.backend.service.RepairOrderFaultsService;
import com.example.backend.service.RepairOrderFundService;
import com.example.backend.service.RepairOrderPaymentsService;
import com.example.backend.service.RepairOrdersService;
import com.example.backend.service.ReviewsService;
import com.example.backend.service.ServiceCategoriesService;
import com.example.backend.service.ServiceTypesService;
import com.example.backend.service.SystemConfigsService;
import com.example.backend.service.SystemMessagesService;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.service.UserAddressesService;
import com.example.backend.service.UserOrderFlowService;
import com.example.backend.service.VideosService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import lombok.Data;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user/orders")
public class UserOrdersController {

    private static final String ORDER_FAULT_BUSINESS_TYPE = "REPAIR_ORDER_FAULT";
    private static final String ORDER_INSPECTION_BUSINESS_TYPE = "REPAIR_ORDER_INSPECTION";
    private static final String AFTER_SALES_BUSINESS_TYPE = "AFTER_SALES_APPLICATION";
    private static final String WORKER_ORDER_CANCEL_MESSAGE_TYPE = "USER_ORDER_CANCEL_NOTIFY_WORKER";
    private static final String WORKER_AFTER_SALES_MESSAGE_TYPE = "USER_AFTER_SALES_APPLY_NOTIFY_WORKER";
    private static final String INSPECTION_PROGRESS_TYPE = "inspection";
    private static final int PAYMENT_METHOD_WECHAT = 1;
    private static final int PAYMENT_METHOD_ALIPAY = 2;
    private static final int PAYMENT_METHOD_WALLET = 5;
    private static final int OPERATOR_TYPE_USER = 1;
    private static final int PAYMENT_RECORD_STATUS_SUCCESS = 3;
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
    private static final int DEFAULT_MAX_AFTER_SALES_IMAGE_COUNT = 5;
    private static final long DEFAULT_AFTER_SALES_VALID_DAYS = 7L;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RepairOrdersService repairOrdersService;
    private final RepairOrderPaymentsService repairOrderPaymentsService;
    private final RepairOrderFaultsService repairOrderFaultsService;
    private final ServiceTypesService serviceTypesService;
    private final ServiceCategoriesService serviceCategoriesService;
    private final TechnicianAccountsService technicianAccountsService;
    private final UserAddressesService userAddressesService;
    private final FaultPhenomenaService faultPhenomenaService;
    private final ImagesService imagesService;
    private final VideosService videosService;
    private final OrderProgressService orderProgressService;
    private final OrderDoorQrService orderDoorQrService;
    private final ConversationSessionsService conversationSessionsService;
    private final PaymentRecordsService paymentRecordsService;
    private final RepairOrderFundService repairOrderFundService;
    private final AfterSalesApplicationsService afterSalesApplicationsService;
    private final SystemMessagesService systemMessagesService;
    private final ReviewsService reviewsService;
    private final SystemConfigsService systemConfigsService;
    private final UserOrderFlowService userOrderFlowService;

    public UserOrdersController(
        RepairOrdersService repairOrdersService,
        RepairOrderPaymentsService repairOrderPaymentsService,
        RepairOrderFaultsService repairOrderFaultsService,
        ServiceTypesService serviceTypesService,
        ServiceCategoriesService serviceCategoriesService,
        TechnicianAccountsService technicianAccountsService,
        UserAddressesService userAddressesService,
        FaultPhenomenaService faultPhenomenaService,
        ImagesService imagesService,
        VideosService videosService,
        OrderProgressService orderProgressService,
        OrderDoorQrService orderDoorQrService,
        ConversationSessionsService conversationSessionsService,
        PaymentRecordsService paymentRecordsService,
        RepairOrderFundService repairOrderFundService,
        AfterSalesApplicationsService afterSalesApplicationsService,
        SystemMessagesService systemMessagesService,
        ReviewsService reviewsService,
        SystemConfigsService systemConfigsService,
        UserOrderFlowService userOrderFlowService
    ) {
        this.repairOrdersService = repairOrdersService;
        this.repairOrderPaymentsService = repairOrderPaymentsService;
        this.repairOrderFaultsService = repairOrderFaultsService;
        this.serviceTypesService = serviceTypesService;
        this.serviceCategoriesService = serviceCategoriesService;
        this.technicianAccountsService = technicianAccountsService;
        this.userAddressesService = userAddressesService;
        this.faultPhenomenaService = faultPhenomenaService;
        this.imagesService = imagesService;
        this.videosService = videosService;
        this.orderProgressService = orderProgressService;
        this.orderDoorQrService = orderDoorQrService;
        this.conversationSessionsService = conversationSessionsService;
        this.paymentRecordsService = paymentRecordsService;
        this.repairOrderFundService = repairOrderFundService;
        this.afterSalesApplicationsService = afterSalesApplicationsService;
        this.systemMessagesService = systemMessagesService;
        this.reviewsService = reviewsService;
        this.systemConfigsService = systemConfigsService;
        this.userOrderFlowService = userOrderFlowService;
    }

    @GetMapping("/list")
    public Result<List<UserOrderListItemResponse>> listOrders(
        @RequestParam(value = "tab", required = false) String tab
    ) {
        LoginUserInfo user = requireCurrentUser();
        List<RepairOrders> orders = repairOrdersService.list(buildListQuery(user.getAccountId(), normalizeTab(tab)));
        if (orders.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        Map<String, ServiceTypes> serviceTypeMap = listServiceTypeMap(orders);
        Map<String, ServiceCategories> categoryMap = listCategoryMap(serviceTypeMap);
        Map<String, TechnicianAccounts> technicianMap = listTechnicianMap(orders);
        Map<String, UserAddresses> addressMap = listAddressMap(orders);
        Map<String, List<RepairOrderFaults>> faultMap = listFaultMap(orders);
        Map<String, FaultPhenomena> phenomenonMap = listPhenomenonMap(faultMap);
        Map<String, RepairOrderPayments> paymentMap = listPaymentMap(orders);
        Map<String, AfterSalesApplications> afterSalesMap = listLatestAfterSalesMap(orders, user.getAccountId());
        Map<String, OrderDoorQrCodes> activeDoorQrMap = orderDoorQrService.getActiveCodeMap(
            orders.stream().map(RepairOrders::getId).collect(Collectors.toList())
        );

        List<UserOrderListItemResponse> items = new ArrayList<>();
        for (RepairOrders order : orders) {
            items.add(buildOrderItem(
                order,
                serviceTypeMap.get(order.getServiceTypeId()),
                categoryMap,
                technicianMap.get(order.getTechnicianAccountId()),
                addressMap.get(order.getServiceAddressId()),
                faultMap.get(order.getId()),
                phenomenonMap,
                paymentMap.get(order.getId()),
                afterSalesMap.get(order.getId()),
                activeDoorQrMap.get(order.getId()) != null
            ));
        }
        return Result.success(items);
    }

    @GetMapping("/door-qr")
    public Result<UserOrderDoorQrResponse> getDoorQr(@RequestParam("orderId") String orderId) {
        LoginUserInfo user = requireCurrentUser();
        return Result.success(orderDoorQrService.getUserDoorQr(orderId, user.getAccountId()));
    }

    @GetMapping("/detail")
    public Result<UserOrderDetailResponse> getOrderDetail(@RequestParam("orderId") String orderId) {
        LoginUserInfo user = requireCurrentUser();
        RepairOrders order = requireOwnedOrder(orderId, user.getAccountId());
        return Result.success(buildOrderDetail(order));
    }

    @PostMapping("/update")
    @Transactional(rollbackFor = Exception.class)
    public Result<UserOrderDetailResponse> updateOrder(@RequestBody(required = false) UserOrderUpdateRequest request) {
        LoginUserInfo user = requireCurrentUser();
        String orderId = request == null ? null : trimToNull(request.getOrderId());
        if (!StringUtils.hasText(orderId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单ID不能为空");
        }

        RepairOrders order = requireOwnedOrder(orderId, user.getAccountId());
        RepairOrderPayments payment = requireOrderPayment(orderId);
        ServiceTypes serviceType = requireOrderServiceType(order.getServiceTypeId());
        boolean appointmentOnly = request != null && Boolean.TRUE.equals(request.getAppointmentOnly());

        if (appointmentOnly) {
            if (!canUserModifyAppointment(order, serviceType)) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, buildModifyAppointmentUnavailableMessage(order, serviceType));
            }
        } else if (!canUserModifyOrder(order, payment)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, buildModifyOrderUnavailableMessage(order, payment));
        }

        long now = System.currentTimeMillis();
        Long oldAppointmentTime = order.getAppointmentTime();
        if (isOnsiteMode(serviceType)) {
            Long nextAppointmentTime = request == null ? null : request.getAppointmentTime();
            if (nextAppointmentTime == null || nextAppointmentTime <= 0L) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "请重新择预约时间");
            }
            boolean appointmentChanged = !safeEquals(oldAppointmentTime, nextAppointmentTime);
            if (appointmentChanged && !canUserModifyAppointment(order, serviceType)) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, buildModifyAppointmentUnavailableMessage(order, serviceType));
            }
            if (appointmentChanged) {
                userOrderFlowService.validateAppointmentTime(order.getTechnicianAccountId(), nextAppointmentTime, order.getId());
            }
            order.setAppointmentTime(nextAppointmentTime);
        }

        if (!appointmentOnly) {
            order.setApplianceBrand(trimToNull(request.getApplianceBrand()));
            order.setApplianceModel(trimToNull(request.getApplianceModel()));
            order.setPurchaseDate(parsePurchaseDate(request.getPurchaseDate()));
            replaceOrderFaultDetails(order, serviceType.getId(), user.getAccountId(), request.getFaultList(), now);
        }

        order.setUpdatedTime(now);
        if (!repairOrdersService.updateById(order)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新订单失败");
        }

        if (!safeEquals(oldAppointmentTime, order.getAppointmentTime())) {
            if (shouldRegenerateDoorQr(order, serviceType)) {
                orderDoorQrService.generateForAcceptedOrder(order, serviceType);
            } else {
                orderDoorQrService.invalidateCurrentCodes(order.getId());
            }
        }
        saveUserModifyProgress(order, appointmentOnly, now);
        return Result.success(buildOrderDetail(order));
    }

    @PostMapping("/pay-tail")
    @Transactional(rollbackFor = Exception.class)
    public Result<UserOrderDetailResponse> payTail(@RequestBody(required = false) UserOrderTailPayRequest request) {
        LoginUserInfo user = requireCurrentUser();
        String orderId = request == null ? null : trimToNull(request.getOrderId());
        if (!StringUtils.hasText(orderId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单ID不能为空");
        }

        RepairOrders order = requireOwnedOrder(orderId, user.getAccountId());
        if (safeInt(order.getStatus()) != 4) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前订单不在待支付状态");
        }

        RepairOrderPayments payment = requireOrderPayment(orderId);
        BigDecimal totalAmount = normalizeMoney(payment.getTotalAmount());
        BigDecimal paidAmount = normalizeMoney(payment.getActualAmount());
        BigDecimal remainingAmount = totalAmount.subtract(paidAmount).setScale(2, RoundingMode.HALF_UP);
        if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前订单暂无需要支付的尾款");
        }

        int paymentMethod = normalizePaymentMethod(request.getPaymentMethod());
        long now = System.currentTimeMillis();

        payment.setActualAmount(totalAmount);
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentTime(now);
        payment.setUpdatedTime(now);
        if (!repairOrderPaymentsService.updateById(payment)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新订单状态失败");
        }

        order.setPaymentStatus(2);
        order.setUpdatedTime(now);
        if (!repairOrdersService.updateById(order)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新订单状态失败");
        }

        createTailPaymentRecord(order, user.getAccountId(), paymentMethod, remainingAmount, now);
        repairOrderFundService.recordOrderTailPay(
            user.getAccountId(),
            order.getTechnicianAccountId(),
            order.getId(),
            order.getOrderNo(),
            paymentMethod,
            remainingAmount,
            now
        );
        saveTailPaymentProgress(order, remainingAmount, paymentMethod, now);

        return Result.success(buildOrderDetail(order));
    }

    @PostMapping("/confirm-completion")
    @Transactional(rollbackFor = Exception.class)
    public Result<UserOrderDetailResponse> confirmCompletion(
        @RequestBody(required = false) UserOrderConfirmCompletionRequest request
    ) {
        LoginUserInfo user = requireCurrentUser();
        String orderId = request == null ? null : trimToNull(request.getOrderId());
        if (!StringUtils.hasText(orderId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单ID不能为空");
        }

        RepairOrders order = requireOwnedOrder(orderId, user.getAccountId());
        RepairOrderPayments payment = requireOrderPayment(orderId);
        if (!canUserConfirmCompletion(order, payment)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, buildConfirmCompletionUnavailableMessage(order, payment));
        }

        long now = System.currentTimeMillis();
        order.setStatus(6);
        if (order.getEndTime() == null || order.getEndTime() <= 0L) {
            order.setEndTime(now);
        }
        order.setCompletionTime(now);
        order.setUpdatedTime(now);
        if (!repairOrdersService.updateById(order)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "订单完成失败");
        }

        if (payment != null) {
            repairOrderFundService.settleOnOrderCompleted(order, payment, now);
        }
        orderDoorQrService.invalidateCurrentCodes(orderId);
        closeConversationSession(order, now);
        saveUserConfirmCompletionProgress(order, now);
        return Result.success(buildOrderDetail(order));
    }

    @PostMapping("/cancel")
    @Transactional(rollbackFor = Exception.class)
    public Result<UserOrderDetailResponse> cancelOrder(@RequestBody(required = false) UserOrderCancelRequest request) {
        LoginUserInfo user = requireCurrentUser();
        String orderId = request == null ? null : trimToNull(request.getOrderId());
        String cancelReason = request == null ? null : trimToNull(request.getReason());
        if (!StringUtils.hasText(orderId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单ID不能为空");
        }
        if (!StringUtils.hasText(cancelReason)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "取消原因不能为空");
        }

        RepairOrders order = requireOwnedOrder(orderId, user.getAccountId());
        RepairOrderPayments payment = requireOrderPayment(orderId);
        ServiceTypes serviceType = requireOrderServiceType(order.getServiceTypeId());
        if (!canUserCancel(order, payment)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, buildCancelUnavailableMessage(order, payment));
        }

        long now = System.currentTimeMillis();
        BigDecimal refundAmount = resolveCancelRefundAmount(order, serviceType, payment);
        BigDecimal paidAmount = normalizeMoney(payment.getActualAmount());
        boolean shouldRefund = refundAmount.compareTo(BigDecimal.ZERO) > 0;

        order.setStatus(7);
        order.setCancelReason(cancelReason);
        order.setCancelTime(now);
        order.setUpdatedTime(now);
        order.setRefundAmount(refundAmount);
        if (shouldRefund) {
            order.setPaymentStatus(3);
            order.setRefundReason("上门前取消，已退还上门费");
            order.setRefundTime(now);
        } else {
            order.setRefundReason(buildCancelRefundMessage(order, serviceType, payment));
            order.setRefundTime(null);
        }
        if (!repairOrdersService.updateById(order)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新订单信息失败");
        }

        orderDoorQrService.invalidateCurrentCodes(orderId);
        closeConversationSession(order, now);
        if (shouldRefund) {
            repairOrderFundService.refundOnOrderClosed(order, payment, order.getRefundReason(), now);
        } else if (paidAmount.compareTo(BigDecimal.ZERO) > 0 && hasTechnicianArrived(order, serviceType)) {
            repairOrderFundService.settleRetainedAmountOnCancel(order, paidAmount, now);
        }
        saveUserCancelProgress(order, cancelReason, refundAmount, now);
        notifyWorkerOrderCanceled(order, serviceType, cancelReason, now);
        return Result.success(buildOrderDetail(order));
    }

    @PostMapping("/after-sales/apply")
    @Transactional(rollbackFor = Exception.class)
    public Result<UserOrderDetailResponse> applyAfterSales(
        @RequestBody(required = false) UserOrderAfterSalesApplyRequest request
    ) {
        LoginUserInfo user = requireCurrentUser();
        String orderId = request == null ? null : trimToNull(request.getOrderId());
        String reason = request == null ? null : trimToNull(request.getReason());
        String description = request == null ? null : trimToNull(request.getDescription());
        List<UserAfterSalesSubmitMediaItem> images = normalizeAfterSalesImages(request == null ? null : request.getImages());
        UserAfterSalesSubmitMediaItem video = normalizeAfterSalesVideo(request == null ? null : request.getVideo());
        if (!StringUtils.hasText(orderId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单ID不能为空");
        }
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "售后原因不能为空");
        }

        RepairOrders order = requireOwnedOrder(orderId, user.getAccountId());
        RepairOrderPayments payment = requireOrderPayment(orderId);
        AfterSalesApplications latestApplication = getLatestAfterSalesApplication(orderId, user.getAccountId());
        if (!canUserApplyAfterSales(order, payment, latestApplication)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, buildAfterSalesUnavailableMessage(order, payment, latestApplication));
        }

        UserAddresses address = getAddress(order.getServiceAddressId());
        long now = System.currentTimeMillis();

        AfterSalesApplications application = new AfterSalesApplications();
        application.setId(SnowflakeIdUtil.nextAfterSalesApplicationId());
        application.setOrderId(orderId);
        application.setOrderType(ORDER_TYPE_REPAIR);
        application.setAccountId(user.getAccountId());
        application.setApplicationType(AFTER_SALES_TYPE_REPAIR);
        application.setReason(reason);
        application.setDescription(description);
        application.setEvidenceImages(buildEvidenceImageSnapshot(images));
        application.setContactPhone(address == null ? "" : safe(address.getContactPhone()));
        application.setContactAddress(buildFullAddress(address));
        application.setRefundAmount(normalizeMoney(payment.getActualAmount()));
        application.setStatus(AFTER_SALES_STATUS_PENDING);
        application.setCreatedTime(now);
        application.setUpdatedTime(now);
        application.setIsDelete(0);
        if (!afterSalesApplicationsService.save(application)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建售后申请失败");
        }

        saveAfterSalesImages(images, application.getId(), user.getAccountId(), now);
        saveAfterSalesVideo(video, application.getId(), user.getAccountId(), now);
        ensureConversationSessionOpen(order, now);
        saveAfterSalesProgress(order, reason, now);
        notifyWorkerAfterSalesApplied(order, reason, now);
        return Result.success(buildOrderDetail(order));
    }

    @GetMapping("/after-sales/detail")
    public Result<UserAfterSalesDetailResponse> getAfterSalesDetail(@RequestParam("orderId") String orderId) {
        LoginUserInfo user = requireCurrentUser();
        String normalizedOrderId = trimToNull(orderId);
        if (!StringUtils.hasText(normalizedOrderId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单ID不能为空");
        }
        RepairOrders order = requireOwnedOrder(normalizedOrderId, user.getAccountId());
        RepairOrderPayments payment = requireOrderPayment(normalizedOrderId);
        AfterSalesApplications latestApplication = getLatestAfterSalesApplication(normalizedOrderId, user.getAccountId());
        return Result.success(buildAfterSalesDetail(order, payment, latestApplication));
    }

    @PostMapping("/after-sales/cancel")
    @Transactional(rollbackFor = Exception.class)
    public Result<UserAfterSalesDetailResponse> cancelAfterSales(
        @RequestBody(required = false) UserOrderAfterSalesCancelRequest request
    ) {
        LoginUserInfo user = requireCurrentUser();
        String applicationId = request == null ? null : trimToNull(request.getApplicationId());
        if (!StringUtils.hasText(applicationId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "售后申请ID不能为空");
        }

        AfterSalesApplications application = afterSalesApplicationsService.getOne(
            new LambdaQueryWrapper<AfterSalesApplications>()
                .eq(AfterSalesApplications::getId, applicationId)
                .eq(AfterSalesApplications::getAccountId, user.getAccountId())
                .eq(AfterSalesApplications::getOrderType, ORDER_TYPE_REPAIR)
                .eq(AfterSalesApplications::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (application == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "售后申请不存在");
        }
        if (!canUserCancelAfterSales(application)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前售后申请暂不可取消");
        }

        RepairOrders order = requireOwnedOrder(application.getOrderId(), user.getAccountId());
        RepairOrderPayments payment = requireOrderPayment(order.getId());
        long now = System.currentTimeMillis();

        application.setStatus(AFTER_SALES_STATUS_CANCELED);
        application.setUpdatedTime(now);
        application.setCompletedTime(now);
        if (!afterSalesApplicationsService.updateById(application)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "取消售后申请失败");
        }

        closeConversationSession(order, now);
        saveAfterSalesCancelledProgress(order, now);
        return Result.success(buildAfterSalesDetail(order, payment, application));
    }

    private LambdaQueryWrapper<RepairOrders> buildListQuery(String accountId, String tab) {
        LambdaQueryWrapper<RepairOrders> wrapper = new LambdaQueryWrapper<RepairOrders>()
            .eq(RepairOrders::getAccountId, accountId)
            .eq(RepairOrders::getIsDelete, 0);

        if ("waiting".equals(tab)) {
            wrapper.eq(RepairOrders::getStatus, 1);
        } else if ("processing".equals(tab)) {
            wrapper.in(RepairOrders::getStatus, 2, 3, 5);
        } else if ("to-pay".equals(tab)) {
            wrapper.eq(RepairOrders::getStatus, 4);
        } else if ("finished".equals(tab)) {
            wrapper.eq(RepairOrders::getStatus, 6);
        } else if ("closed".equals(tab)) {
            wrapper.in(RepairOrders::getStatus, 7, 8);
        }

        wrapper.orderByDesc(RepairOrders::getUpdatedTime)
            .orderByDesc(RepairOrders::getCreatedTime);
        return wrapper;
    }

    private UserOrderDetailResponse buildOrderDetail(RepairOrders order) {
        List<RepairOrders> orders = Collections.singletonList(order);
        Map<String, ServiceTypes> serviceTypeMap = listServiceTypeMap(orders);
        Map<String, ServiceCategories> categoryMap = listCategoryMap(serviceTypeMap);
        Map<String, TechnicianAccounts> technicianMap = listTechnicianMap(orders);
        Map<String, UserAddresses> addressMap = listAddressMap(orders);
        Map<String, List<RepairOrderFaults>> faultMap = listFaultMap(orders);
        Map<String, FaultPhenomena> phenomenonMap = listPhenomenonMap(faultMap);
        Map<String, RepairOrderPayments> paymentMap = listPaymentMap(orders);
        List<OrderProgress> progressList = listOrderProgress(order.getId());
        OrderProgress latestInspectionProgress = findLatestInspectionProgress(progressList);
        InspectionProgressSnapshot inspectionSnapshot = parseInspectionProgress(latestInspectionProgress);
        ServiceTypes serviceType = serviceTypeMap.get(order.getServiceTypeId());
        RepairOrderPayments payment = paymentMap.get(order.getId());
        AfterSalesApplications latestAfterSalesApplication = getLatestAfterSalesApplication(order.getId(), order.getAccountId());
        Reviews review = reviewsService.getUserOrderReviewEntity(order.getId(), order.getAccountId());

        UserOrderListItemResponse item = buildOrderItem(
            order,
            serviceType,
            categoryMap,
            technicianMap.get(order.getTechnicianAccountId()),
            addressMap.get(order.getServiceAddressId()),
            faultMap.get(order.getId()),
            phenomenonMap,
            payment,
            latestAfterSalesApplication,
            orderDoorQrService.getActiveCodeMap(Collections.singletonList(order.getId())).get(order.getId()) != null
        );

        UserOrderDetailResponse response = new UserOrderDetailResponse();
        copyListFields(item, response);
        response.setPurchaseDate(order.getPurchaseDate());
        response.setDoorFee(formatMoney(payment == null ? null : payment.getDoorFee()));
        response.setDistanceFee(formatMoney(payment == null ? null : payment.getDistanceFee()));
        response.setServiceFee(formatMoney(payment == null ? null : payment.getServiceFee()));
        response.setMaterialFee(formatMoney(payment == null ? null : payment.getMaterialFee()));
        response.setOvertimeFee(formatMoney(payment == null ? null : payment.getOvertimeFee()));
        response.setRemark(safe(order.getRemark()));
        response.setInspectionDiagnosis(inspectionSnapshot == null ? "" : inspectionSnapshot.getInspectionDiagnosis());
        response.setRepairPlan(inspectionSnapshot == null ? "" : inspectionSnapshot.getRepairPlan());
        response.setInspectionTime(latestInspectionProgress == null ? null : latestInspectionProgress.getCreatedTime());
        response.setCancelReason(safe(order.getCancelReason()));
        response.setCancelTime(order.getCancelTime());
        response.setRefundReason(safe(order.getRefundReason()));
        response.setRefundAmount(formatMoney(order.getRefundAmount()));
        response.setRefundTime(order.getRefundTime());
        response.setServiceAddressId(order.getServiceAddressId());
        response.setCanCancel(canUserCancel(order, payment));
        response.setCancelTip(buildCancelTip(order, serviceType, payment));
        response.setCancelRefundAmount(formatMoney(resolveCancelRefundAmount(order, serviceType, payment)));
        response.setCanModifyOrder(canUserModifyOrder(order, payment));
        response.setCanModifyAppointment(canUserModifyAppointment(order, serviceType));
        response.setCanConfirmCompletion(canUserConfirmCompletion(order, payment));
        response.setConfirmCompletionTip(buildConfirmCompletionTip(order, payment));
        response.setCanApplyAfterSales(canUserApplyAfterSales(order, payment, latestAfterSalesApplication));
        response.setAfterSalesTip(buildAfterSalesTip(order, payment, latestAfterSalesApplication));
        response.setCanReview(canUserReview(order, review));
        response.setHasReview(review != null);
        response.setReviewId(review == null ? "" : safe(review.getId()));
        response.setAfterSalesApplication(buildAfterSalesSummary(latestAfterSalesApplication));
        response.setFaultList(buildFaultItems(faultMap.get(order.getId())));
        response.setInspectionImages(listInspectionImages(latestInspectionProgress == null ? null : latestInspectionProgress.getId()));
        response.setInspectionVideos(listInspectionVideos(latestInspectionProgress == null ? null : latestInspectionProgress.getId()));
        response.setProgressList(buildProgressItems(order, progressList));
        return response;
    }

    private void copyListFields(UserOrderListItemResponse item, UserOrderDetailResponse response) {
        response.setId(item.getId());
        response.setOrderNo(item.getOrderNo());
        response.setStatus(item.getStatus());
        response.setStatusText(item.getStatusText());
        response.setPaymentStatus(item.getPaymentStatus());
        response.setPaymentStatusText(item.getPaymentStatusText());
        response.setServiceTypeId(item.getServiceTypeId());
        response.setServiceTypeName(item.getServiceTypeName());
        response.setServiceCategoryId(item.getServiceCategoryId());
        response.setServiceCategoryName(item.getServiceCategoryName());
        response.setServiceCategoryPath(item.getServiceCategoryPath());
        response.setServiceMode(item.getServiceMode());
        response.setServiceModeText(item.getServiceModeText());
        response.setTechnicianId(item.getTechnicianId());
        response.setTechnicianName(item.getTechnicianName());
        response.setTechnicianPhone(item.getTechnicianPhone());
        response.setServiceAddress(item.getServiceAddress());
        response.setServiceAddressShort(item.getServiceAddressShort());
        response.setContactName(item.getContactName());
        response.setContactPhone(item.getContactPhone());
        response.setApplianceBrand(item.getApplianceBrand());
        response.setApplianceModel(item.getApplianceModel());
        response.setFaultSummary(item.getFaultSummary());
        response.setTotalAmount(item.getTotalAmount());
        response.setPaidAmount(item.getPaidAmount());
        response.setAppointmentTime(item.getAppointmentTime());
        response.setCreatedTime(item.getCreatedTime());
        response.setUpdatedTime(item.getUpdatedTime());
        response.setHasDoorQr(item.getHasDoorQr());
    }

    private ServiceTypes requireOrderServiceType(String serviceTypeId) {
        ServiceTypes serviceType = serviceTypesService.getOne(
            new LambdaQueryWrapper<ServiceTypes>()
                .eq(ServiceTypes::getId, serviceTypeId)
                .eq(ServiceTypes::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (serviceType == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "服务类型不存在");
        }
        return serviceType;
    }

    private ServiceTypes getOrderServiceType(String serviceTypeId) {
        if (!StringUtils.hasText(serviceTypeId)) {
            return null;
        }
        return serviceTypesService.getOne(
            new LambdaQueryWrapper<ServiceTypes>()
                .eq(ServiceTypes::getId, serviceTypeId)
                .eq(ServiceTypes::getIsDelete, 0)
                .last("limit 1"),
            false
        );
    }

    private UserAddresses getAddress(String addressId) {
        if (!StringUtils.hasText(addressId)) {
            return null;
        }
        return userAddressesService.getOne(
            new LambdaQueryWrapper<UserAddresses>()
                .eq(UserAddresses::getId, addressId)
                .eq(UserAddresses::getIsDelete, 0)
                .last("limit 1"),
            false
        );
    }

    private ServiceCategories getCategory(String categoryId) {
        if (!StringUtils.hasText(categoryId)) {
            return null;
        }
        return serviceCategoriesService.getOne(
            new LambdaQueryWrapper<ServiceCategories>()
                .eq(ServiceCategories::getId, categoryId)
                .eq(ServiceCategories::getIsDelete, 0)
                .last("limit 1"),
            false
        );
    }

    private TechnicianAccounts getTechnician(String technicianId) {
        if (!StringUtils.hasText(technicianId)) {
            return null;
        }
        return technicianAccountsService.getOne(
            new LambdaQueryWrapper<TechnicianAccounts>()
                .eq(TechnicianAccounts::getId, technicianId)
                .eq(TechnicianAccounts::getIsDelete, 0)
                .last("limit 1"),
            false
        );
    }

    private AfterSalesApplications getLatestAfterSalesApplication(String orderId, String accountId) {
        if (!StringUtils.hasText(orderId) || !StringUtils.hasText(accountId)) {
            return null;
        }
        return afterSalesApplicationsService.getOne(
            new LambdaQueryWrapper<AfterSalesApplications>()
                .eq(AfterSalesApplications::getOrderId, orderId)
                .eq(AfterSalesApplications::getOrderType, ORDER_TYPE_REPAIR)
                .eq(AfterSalesApplications::getAccountId, accountId)
                .eq(AfterSalesApplications::getIsDelete, 0)
                .orderByDesc(AfterSalesApplications::getCreatedTime)
                .last("limit 1"),
            false
        );
    }

    private Map<String, AfterSalesApplications> listLatestAfterSalesMap(List<RepairOrders> orders, String accountId) {
        if (orders == null || orders.isEmpty() || !StringUtils.hasText(accountId)) {
            return Collections.emptyMap();
        }
        Set<String> orderIds = orders.stream()
            .map(RepairOrders::getId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, AfterSalesApplications> latestMap = new HashMap<>();
        for (AfterSalesApplications application : afterSalesApplicationsService.list(
            new LambdaQueryWrapper<AfterSalesApplications>()
                .in(AfterSalesApplications::getOrderId, orderIds)
                .eq(AfterSalesApplications::getOrderType, ORDER_TYPE_REPAIR)
                .eq(AfterSalesApplications::getAccountId, accountId)
                .eq(AfterSalesApplications::getIsDelete, 0)
                .orderByDesc(AfterSalesApplications::getCreatedTime)
        )) {
            if (!latestMap.containsKey(application.getOrderId())) {
                latestMap.put(application.getOrderId(), application);
            }
        }
        return latestMap;
    }

    private boolean canUserCancel(RepairOrders order, RepairOrderPayments payment) {
        int status = safeInt(order == null ? null : order.getStatus());
        if (status < 1 || status > 4) {
            return false;
        }
        return !isTailPaymentCompleted(payment);
    }

    private boolean canUserModifyOrder(RepairOrders order, RepairOrderPayments payment) {
        int status = safeInt(order == null ? null : order.getStatus());
        return status >= 1 && status <= 4 && !isTailPaymentCompleted(payment);
    }

    private boolean canUserModifyAppointment(RepairOrders order, ServiceTypes serviceType) {
        if (order == null || serviceType == null || !isOnsiteMode(serviceType)) {
            return false;
        }
        int status = safeInt(order.getStatus());
        return status >= 1 && status < 6 && !hasTechnicianArrived(order, serviceType);
    }

    private boolean canUserConfirmCompletion(RepairOrders order, RepairOrderPayments payment) {
        return isWaitingUserConfirmCompletion(order) || canUserForceConfirmCompletionAfterTailPaid(order, payment);
    }

    private boolean canUserApplyAfterSales(
        RepairOrders order,
        RepairOrderPayments payment,
        AfterSalesApplications latestApplication
    ) {
        if (isAfterSalesProcessing(latestApplication)) {
            return false;
        }
        int status = safeInt(order == null ? null : order.getStatus());
        if (status == 7 || status == 8) {
            return false;
        }
        if (status != 6) {
            return false;
        }
        if (!isAfterSalesWithinWindow(order)) {
            return false;
        }
        return normalizeMoney(payment == null ? null : payment.getActualAmount()).compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean isAfterSalesProcessing(AfterSalesApplications application) {
        int status = safeInt(application == null ? null : application.getStatus());
        return status == AFTER_SALES_STATUS_PENDING
            || status == AFTER_SALES_STATUS_APPROVED
            || status == AFTER_SALES_STATUS_PROCESSING;
    }

    private boolean canUserCancelAfterSales(AfterSalesApplications application) {
        int status = safeInt(application == null ? null : application.getStatus());
        return status == AFTER_SALES_STATUS_PENDING
            || status == AFTER_SALES_STATUS_APPROVED
            || status == AFTER_SALES_STATUS_PROCESSING;
    }

    private boolean isTailPaymentCompleted(RepairOrderPayments payment) {
        return payment != null && !isPrepaidOnly(payment) && isFullyPaid(payment);
    }

    private boolean isWaitingUserConfirmCompletion(RepairOrders order) {
        return order != null
            && safeInt(order.getStatus()) == 5
            && order.getEndTime() != null
            && order.getEndTime() > 0L
            && (order.getCompletionTime() == null || order.getCompletionTime() <= 0L);
    }

    private boolean canUserForceConfirmCompletionAfterTailPaid(RepairOrders order, RepairOrderPayments payment) {
        return order != null
            && safeInt(order.getStatus()) == 4
            && isTailPaymentCompleted(payment)
            && (order.getCompletionTime() == null || order.getCompletionTime() <= 0L);
    }

    private boolean isAfterSalesWithinWindow(RepairOrders order) {
        if (order == null || order.getCompletionTime() == null || order.getCompletionTime() <= 0L) {
            return false;
        }
        return System.currentTimeMillis() - order.getCompletionTime() <= getAfterSalesValidMillis();
    }

    private boolean hasTechnicianArrived(RepairOrders order, ServiceTypes serviceType) {
        if (order == null || serviceType == null) {
            return false;
        }
        int mode = safeInt(serviceType.getType());
        int status = safeInt(order.getStatus());
        if (mode == 1) {
            return status >= 3;
        }
        if (mode == 2) {
            return status >= 5 || order.getStartTime() != null;
        }
        return false;
    }

    private BigDecimal resolveCancelRefundAmount(RepairOrders order, ServiceTypes serviceType, RepairOrderPayments payment) {
        if (order == null || serviceType == null || payment == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        int mode = safeInt(serviceType.getType());
        if ((mode != 1 && mode != 2) || hasTechnicianArrived(order, serviceType)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return normalizeMoney(payment.getActualAmount());
    }

    private String buildCancelTip(RepairOrders order, ServiceTypes serviceType, RepairOrderPayments payment) {
        if (order == null) {
            return "";
        }
        int status = safeInt(order.getStatus());
        if (status == 7) {
            return "订单已取消";
        }
        if (status == 8) {
            return "订单已退款";
        }
        if (!canUserCancel(order, payment)) {
            return buildCancelUnavailableMessage(order, payment);
        }
        BigDecimal refundAmount = resolveCancelRefundAmount(order, serviceType, payment);
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            return "当前取消将退还已付上门费";
        }
        String refundMessage = buildCancelRefundMessage(order, serviceType, payment);
        return StringUtils.hasText(refundMessage) ? refundMessage : "当前取消后单将直接关闭";
    }

    private String buildCancelUnavailableMessage(RepairOrders order, RepairOrderPayments payment) {
        int status = safeInt(order == null ? null : order.getStatus());
        if (status == 7) {
            return "订单已取消";
        }
        if (status == 8) {
            return "订单已关闭";
        }
        if (isTailPaymentCompleted(payment)) {
            return "尾款已支付，当前订单不支持取消";
        }
        return "当前订单状态不支持取消";
    }

    private String buildModifyOrderUnavailableMessage(RepairOrders order, RepairOrderPayments payment) {
        if (canUserModifyOrder(order, payment)) {
            return "";
        }
        if (isTailPaymentCompleted(payment)) {
            return "尾款已支付，当前订单不支持修改订单信息";
        }
        int status = safeInt(order == null ? null : order.getStatus());
        if (status >= 6) {
            return "订单已完成，无法再修改订单信息";
        }
        return "当前订单状态不支持修改订单信息";
    }

    private String buildModifyAppointmentUnavailableMessage(RepairOrders order, ServiceTypes serviceType) {
        if (canUserModifyAppointment(order, serviceType)) {
            return "";
        }
        if (serviceType == null || !isOnsiteMode(serviceType)) {
            return "当前订单不是上门服务，无法修改预约信息";
        }
        int status = safeInt(order == null ? null : order.getStatus());
        if (status >= 6) {
            return "订单已完成，无法再修改预约信息";
        }
        if (hasTechnicianArrived(order, serviceType)) {
            return "工程师已出发或到达，无法再修改预约信息";
        }
        return "当前订单状态不支持修改预约信息";
    }

    private String buildConfirmCompletionTip(RepairOrders order, RepairOrderPayments payment) {
        if (isWaitingUserConfirmCompletion(order)) {
            return "服务已完成，请确认后结束订单";
        }
        if (canUserForceConfirmCompletionAfterTailPaid(order, payment)) {
            return "尾款已支付，如服务已完成可直接确认完成订单";
        }
        return buildConfirmCompletionUnavailableMessage(order, payment);
    }

    private String buildConfirmCompletionUnavailableMessage(RepairOrders order, RepairOrderPayments payment) {
        int status = safeInt(order == null ? null : order.getStatus());
        if (status == 6) {
            return "订单已完成";
        }
        if (status == 7 || status == 8) {
            return "订单已关闭";
        }
        if (status < 4) {
            return "服务尚未完成，暂不能确认完成";
        }
        if (status == 4) {
            return isTailPaymentCompleted(payment) ? "当前订单可确认完成" : "支付尾款后才可确认完成";
        }
        if (status == 5) {
            return "订单处理中，暂不能确认完成";
        }
        return "当前订单暂不能确认完成";
    }

    private String buildCancelRefundMessage(RepairOrders order, ServiceTypes serviceType, RepairOrderPayments payment) {
        if (order == null || serviceType == null || payment == null) {
            return "";
        }
        int mode = safeInt(serviceType.getType());
        if ((mode == 1 || mode == 2)
            && hasTechnicianArrived(order, serviceType)
            && normalizeMoney(payment.getActualAmount()).compareTo(BigDecimal.ZERO) > 0) {
            return "师傅已上门，取消订单不退上门费";
        }
        return "";
    }

    private String buildAfterSalesTip(
        RepairOrders order,
        RepairOrderPayments payment,
        AfterSalesApplications latestApplication
    ) {
        if (isAfterSalesProcessing(latestApplication)) {
            return "售后申请处理中，请等待管理员审核";
        }
        if (canUserApplyAfterSales(order, payment, latestApplication)) {
            return "订单完成后的" + getAfterSalesValidDays() + "天内可申请售后，提交后由管理员审核";
        }
        if (canUserCancel(order, payment)) {
            return "当前订单仍可取消，如需退款可先取消订单";
        }
        int status = safeInt(order == null ? null : order.getStatus());
        if (status == 7 || status == 8) {
            return "订单已关闭，无法申请售后";
        }
        if (status != 6) {
            return "订单完成后才能申请售后";
        }
        if (!isAfterSalesWithinWindow(order)) {
            return "订单完成已超过" + getAfterSalesValidDays() + "天，不能再申请售后";
        }
        return "当前订单暂不支持申请售后";
    }

    private String buildAfterSalesUnavailableMessage(
        RepairOrders order,
        RepairOrderPayments payment,
        AfterSalesApplications latestApplication
    ) {
        if (isAfterSalesProcessing(latestApplication)) {
            return "售后申请处理中，请勿重复提交";
        }
        if (canUserCancel(order, payment)) {
            return "当前订单仍可取消，如需退款可先取消订单";
        }
        int status = safeInt(order == null ? null : order.getStatus());
        if (status == 7 || status == 8) {
            return "订单已关闭，无法申请售后";
        }
        if (status != 6) {
            return "订单完成后的" + getAfterSalesValidDays() + "天内才可申请售后";
        }
        if (!isAfterSalesWithinWindow(order)) {
            return "订单完成已超过" + getAfterSalesValidDays() + "天，无法申请售后";
        }
        return "当前订单暂不支持申请售后";
    }

    private UserAfterSalesApplicationSummary buildAfterSalesSummary(AfterSalesApplications application) {
        if (application == null) {
            return null;
        }
        UserAfterSalesApplicationSummary summary = new UserAfterSalesApplicationSummary();
        summary.setId(application.getId());
        summary.setApplicationType(application.getApplicationType());
        summary.setApplicationTypeText(getAfterSalesApplicationTypeText(application.getApplicationType()));
        summary.setStatus(application.getStatus());
        summary.setStatusText(getAfterSalesStatusText(application.getStatus()));
        summary.setReason(safe(application.getReason()));
        summary.setDescription(safe(application.getDescription()));
        summary.setRefundAmount(formatMoney(application.getRefundAmount()));
        summary.setAdminRemark(safe(application.getAdminRemark()));
        summary.setCreatedTime(application.getCreatedTime());
        summary.setUpdatedTime(application.getUpdatedTime());
        return summary;
    }

    private UserAfterSalesDetailResponse buildAfterSalesDetail(
        RepairOrders order,
        RepairOrderPayments payment,
        AfterSalesApplications latestApplication
    ) {
        UserAfterSalesDetailResponse response = new UserAfterSalesDetailResponse();
        response.setOrderId(order == null ? "" : safe(order.getId()));
        response.setOrderNo(order == null ? "" : safe(order.getOrderNo()));
        response.setOrderStatus(order == null ? null : order.getStatus());
        response.setOrderStatusText(getDisplayStatusText(order));

        ServiceTypes serviceType = order == null ? null : requireOrderServiceType(order.getServiceTypeId());
        ServiceCategories category = serviceType == null ? null : getCategory(serviceType.getCategoryId());
        TechnicianAccounts technician = order == null ? null : getTechnician(order.getTechnicianAccountId());

        response.setServiceTypeName(serviceType == null ? "" : safe(serviceType.getName()));
        response.setServiceCategoryName(category == null ? "" : safe(category.getName()));
        response.setServiceModeText(serviceType == null ? "" : getServiceModeText(serviceType.getType()));
        response.setTechnicianName(technician == null ? "" : safe(technician.getUsername()));
        response.setCanApplyAfterSales(canUserApplyAfterSales(order, payment, latestApplication));
        response.setAfterSalesTip(buildAfterSalesTip(order, payment, latestApplication));
        response.setApplication(buildAfterSalesApplicationDetail(latestApplication));
        return response;
    }

    private UserAfterSalesApplicationDetailResponse buildAfterSalesApplicationDetail(AfterSalesApplications application) {
        if (application == null) {
            return null;
        }
        UserAfterSalesApplicationDetailResponse detail = new UserAfterSalesApplicationDetailResponse();
        detail.setId(application.getId());
        detail.setApplicationType(application.getApplicationType());
        detail.setApplicationTypeText(getAfterSalesApplicationTypeText(application.getApplicationType()));
        detail.setStatus(application.getStatus());
        detail.setStatusText(getAfterSalesStatusText(application.getStatus()));
        detail.setReason(safe(application.getReason()));
        detail.setDescription(safe(application.getDescription()));
        detail.setRefundAmount(formatMoney(application.getRefundAmount()));
        detail.setAdminRemark(safe(application.getAdminRemark()));
        detail.setContactPhone(safe(application.getContactPhone()));
        detail.setContactAddress(safe(application.getContactAddress()));
        detail.setCanCancel(canUserCancelAfterSales(application));
        detail.setCreatedTime(application.getCreatedTime());
        detail.setUpdatedTime(application.getUpdatedTime());
        detail.setProcessedTime(application.getProcessedTime());
        detail.setCompletedTime(application.getCompletedTime());
        detail.setEvidenceImages(listAfterSalesImages(application.getId()));
        detail.setEvidenceVideos(listAfterSalesVideos(application.getId()));
        return detail;
    }

    private String getAfterSalesApplicationTypeText(Integer applicationType) {
        int value = safeInt(applicationType);
        if (value == 1) {
            return "退款";
        }
        if (value == 2) {
            return "退货退款";
        }
        if (value == 3) {
            return "换货";
        }
        if (value == 4) {
            return "返修";
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

    private void saveUserCancelProgress(RepairOrders order, String reason, BigDecimal refundAmount, long now) {
        OrderProgress progress = new OrderProgress();
        progress.setId(SnowflakeIdUtil.nextOrderProgressId());
        progress.setOrderId(order.getId());
        progress.setStatus(7);
        progress.setStatusName("已取消");
        StringBuilder description = new StringBuilder("用户取消订单");
        if (StringUtils.hasText(reason)) {
            description.append("；取消原因：").append(reason);
        }
        if (refundAmount != null && refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            description.append("；退款金额：").append(formatMoney(refundAmount));
        } else if (StringUtils.hasText(order.getRefundReason())) {
            description.append("；退款说明：").append(order.getRefundReason());
        }
        progress.setDescription(description.toString());
        progress.setOperatorId(order.getAccountId());
        progress.setOperatorType(OPERATOR_TYPE_USER);
        progress.setOperatorName("用户");
        progress.setCreatedTime(now);
        progress.setIsDelete(0);
        if (!orderProgressService.save(progress)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存订单进度失败");
        }
    }

    private void saveUserModifyProgress(RepairOrders order, boolean appointmentOnly, long now) {
        OrderProgress progress = new OrderProgress();
        progress.setId(SnowflakeIdUtil.nextOrderProgressId());
        progress.setOrderId(order.getId());
        progress.setStatus(order.getStatus());
        progress.setStatusName(getStatusText(order.getStatus()));
        progress.setDescription(appointmentOnly ? "用户修改了预约信息" : "用户修改了订单信息");
        progress.setOperatorId(order.getAccountId());
        progress.setOperatorType(OPERATOR_TYPE_USER);
        progress.setOperatorName("用户");
        progress.setCreatedTime(now);
        progress.setIsDelete(0);
        if (!orderProgressService.save(progress)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存订单进度失败");
        }
    }

    private void saveAfterSalesProgress(RepairOrders order, String reason, long now) {
        OrderProgress progress = new OrderProgress();
        progress.setId(SnowflakeIdUtil.nextOrderProgressId());
        progress.setOrderId(order.getId());
        progress.setStatus(order.getStatus());
        progress.setStatusName(getStatusText(order.getStatus()));
        progress.setDescription("用户发起售后申请：" + safe(reason));
        progress.setOperatorId(order.getAccountId());
        progress.setOperatorType(OPERATOR_TYPE_USER);
        progress.setOperatorName("用户");
        progress.setCreatedTime(now);
        progress.setIsDelete(0);
        if (!orderProgressService.save(progress)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存订单进度失败");
        }
    }

    private void saveAfterSalesCancelledProgress(RepairOrders order, long now) {
        OrderProgress progress = new OrderProgress();
        progress.setId(SnowflakeIdUtil.nextOrderProgressId());
        progress.setOrderId(order.getId());
        progress.setStatus(order.getStatus());
        progress.setStatusName(getStatusText(order.getStatus()));
        progress.setDescription("用户取消售后申请");
        progress.setOperatorId(order.getAccountId());
        progress.setOperatorType(OPERATOR_TYPE_USER);
        progress.setOperatorName("用户");
        progress.setCreatedTime(now);
        progress.setIsDelete(0);
        if (!orderProgressService.save(progress)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存订单进度失败");
        }
    }

    private void saveUserConfirmCompletionProgress(RepairOrders order, long now) {
        OrderProgress progress = new OrderProgress();
        progress.setId(SnowflakeIdUtil.nextOrderProgressId());
        progress.setOrderId(order.getId());
        progress.setStatus(6);
        progress.setStatusName(getStatusText(6));
        progress.setDescription("用户确认服务完成");
        progress.setOperatorId(order.getAccountId());
        progress.setOperatorType(OPERATOR_TYPE_USER);
        progress.setOperatorName("用户");
        progress.setCreatedTime(now);
        progress.setIsDelete(0);
        if (!orderProgressService.save(progress)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存订单进度失败");
        }
    }

    private UserOrderListItemResponse buildOrderItem(
        RepairOrders order,
        ServiceTypes serviceType,
        Map<String, ServiceCategories> categoryMap,
        TechnicianAccounts technician,
        UserAddresses address,
        List<RepairOrderFaults> faults,
        Map<String, FaultPhenomena> phenomenonMap,
        RepairOrderPayments payment,
        AfterSalesApplications latestAfterSalesApplication,
        boolean hasDoorQr
    ) {
        UserOrderListItemResponse item = new UserOrderListItemResponse();
        item.setId(order.getId());
        item.setOrderNo(order.getOrderNo());
        item.setStatus(order.getStatus());
        item.setStatusText(getDisplayStatusText(order));
        item.setPaymentStatus(order.getPaymentStatus());
        item.setPaymentStatusText(getPaymentStatusText(order, payment));
        item.setServiceTypeId(order.getServiceTypeId());
        item.setServiceTypeName(serviceType == null ? "" : safe(serviceType.getName()));

        ServiceCategories category = serviceType == null ? null : categoryMap.get(serviceType.getCategoryId());
        item.setServiceCategoryId(category == null ? "" : safe(category.getId()));
        item.setServiceCategoryName(category == null ? "" : safe(category.getName()));
        item.setServiceCategoryPath(buildCategoryPath(category, categoryMap));

        Integer serviceMode = serviceType == null ? null : serviceType.getType();
        item.setServiceMode(serviceMode);
        item.setServiceModeText(getServiceModeText(serviceMode));

        item.setTechnicianId(order.getTechnicianAccountId());
        item.setTechnicianName(technician == null ? "暂未分配" : safe(technician.getUsername()));
        item.setTechnicianPhone(technician == null ? "" : safe(technician.getPhone()));

        item.setServiceAddress(buildFullAddress(address));
        item.setServiceAddressShort(buildShortAddress(address));
        item.setContactName(address == null ? "" : safe(address.getContactName()));
        item.setContactPhone(address == null ? "" : safe(address.getContactPhone()));

        item.setApplianceBrand(safe(order.getApplianceBrand()));
        item.setApplianceModel(safe(order.getApplianceModel()));
        item.setFaultSummary(buildFaultSummary(faults, phenomenonMap));
        item.setTotalAmount(formatMoney(payment == null ? null : payment.getTotalAmount()));
        item.setPaidAmount(formatMoney(resolvePaidAmount(order, payment)));
        item.setAppointmentTime(order.getAppointmentTime());
        item.setCreatedTime(order.getCreatedTime());
        item.setUpdatedTime(order.getUpdatedTime());
        item.setHasDoorQr(hasDoorQr);
        item.setCanConfirmCompletion(canUserConfirmCompletion(order, payment));
        item.setConfirmCompletionTip(buildConfirmCompletionTip(order, payment));
        item.setCanApplyAfterSales(canUserApplyAfterSales(order, payment, latestAfterSalesApplication));
        item.setHasAfterSalesEntry(Boolean.TRUE.equals(item.getCanApplyAfterSales()) || latestAfterSalesApplication != null);
        item.setAfterSalesTip(buildAfterSalesTip(order, payment, latestAfterSalesApplication));
        return item;
    }

    private List<UserOrderFaultItemResponse> buildFaultItems(List<RepairOrderFaults> faults) {
        if (faults == null || faults.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> phenomenonIds = faults.stream()
            .map(RepairOrderFaults::getFaultPhenomenonId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        Map<String, FaultPhenomena> phenomenonMap = phenomenonIds.isEmpty()
            ? new HashMap<>()
            : faultPhenomenaService.list(
                new LambdaQueryWrapper<FaultPhenomena>()
                    .in(FaultPhenomena::getId, phenomenonIds)
                    .eq(FaultPhenomena::getIsDelete, 0)
            ).stream().collect(Collectors.toMap(FaultPhenomena::getId, item -> item, (a, b) -> a));

        Set<String> faultIds = faults.stream()
            .map(RepairOrderFaults::getId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        Map<String, List<Images>> imageMap = faultIds.isEmpty()
            ? new HashMap<>()
            : imagesService.list(
                new LambdaQueryWrapper<Images>()
                    .eq(Images::getBusinessType, ORDER_FAULT_BUSINESS_TYPE)
                    .in(Images::getBusinessId, faultIds)
                    .eq(Images::getIsDelete, 0)
                    .orderByAsc(Images::getCreatedTime)
            ).stream().collect(Collectors.groupingBy(Images::getBusinessId));
        Map<String, List<Videos>> videoMap = faultIds.isEmpty()
            ? new HashMap<>()
            : videosService.list(
                new LambdaQueryWrapper<Videos>()
                    .eq(Videos::getBusinessType, ORDER_FAULT_BUSINESS_TYPE)
                    .in(Videos::getBusinessId, faultIds)
                    .eq(Videos::getIsDelete, 0)
                    .orderByAsc(Videos::getCreatedTime)
            ).stream().collect(Collectors.groupingBy(Videos::getBusinessId));

        List<UserOrderFaultItemResponse> items = new ArrayList<>();
        for (RepairOrderFaults fault : faults) {
            FaultPhenomena phenomenon = phenomenonMap.get(fault.getFaultPhenomenonId());
            UserOrderFaultItemResponse item = new UserOrderFaultItemResponse();
            item.setId(fault.getId());
            item.setFaultPhenomenonId(fault.getFaultPhenomenonId());
            item.setFaultPhenomenonName(phenomenon == null ? "" : safe(phenomenon.getName()));
            item.setFaultPhenomenonDescription(phenomenon == null ? "" : safe(phenomenon.getDescription()));
            item.setFaultDescription(safe(fault.getFaultDescription()));
            item.setImages(toImageItems(imageMap.get(fault.getId())));
            item.setVideos(toVideoItems(videoMap.get(fault.getId())));
            items.add(item);
        }
        return items;
    }

    private List<UserOrderProgressItemResponse> buildProgressItems(RepairOrders order, List<OrderProgress> progressList) {
        List<UserOrderProgressItemResponse> items = new ArrayList<>();
        items.add(buildSyntheticProgress(
            "INIT-" + order.getId(),
            1,
            "订单已提交，等待师傅接单",
            "系统",
            4,
            order.getCreatedTime()
        ));

        Set<Integer> existingStatuses = new HashSet<>();
        for (OrderProgress progress : progressList) {
            existingStatuses.add(safeInt(progress.getStatus()));
            UserOrderProgressItemResponse item = new UserOrderProgressItemResponse();
            item.setId(progress.getId());
            item.setStatus(progress.getStatus());
            item.setStatusText(StringUtils.hasText(progress.getStatusName())
                ? progress.getStatusName()
                : getStatusText(progress.getStatus()));
            item.setDescription(resolveProgressDescription(progress));
            item.setOperatorName(safe(progress.getOperatorName()));
            item.setOperatorType(progress.getOperatorType());
            item.setCreatedTime(progress.getCreatedTime());
            items.add(item);
        }

        int currentStatus = safeInt(order.getStatus());
        if (currentStatus == 6 && order.getCompletionTime() != null && !existingStatuses.contains(6)) {
            items.add(buildSyntheticProgress(
                "FINISH-" + order.getId(),
                6,
                "订单已完成",
                "系统",
                4,
                order.getCompletionTime()
            ));
        }
        if (currentStatus == 7 && order.getCancelTime() != null && !existingStatuses.contains(7)) {
            String description = StringUtils.hasText(order.getCancelReason())
                ? "订单已取消：" + order.getCancelReason()
                : "订单已取消";
            items.add(buildSyntheticProgress(
                "CANCEL-" + order.getId(),
                7,
                description,
                "系统",
                4,
                order.getCancelTime()
            ));
        }
        if (currentStatus == 8 && order.getRefundTime() != null && !existingStatuses.contains(8)) {
            String description = StringUtils.hasText(order.getRefundReason())
                ? "订单已退款：" + order.getRefundReason()
                : "订单已退款";
            items.add(buildSyntheticProgress(
                "REFUND-" + order.getId(),
                8,
                description,
                "系统",
                4,
                order.getRefundTime()
            ));
        }
        return items;
    }

    private UserOrderProgressItemResponse buildSyntheticProgress(
        String id,
        Integer status,
        String description,
        String operatorName,
        Integer operatorType,
        Long createdTime
    ) {
        UserOrderProgressItemResponse item = new UserOrderProgressItemResponse();
        item.setId(id);
        item.setStatus(status);
        item.setStatusText(getStatusText(status));
        item.setDescription(description);
        item.setOperatorName(operatorName);
        item.setOperatorType(operatorType);
        item.setCreatedTime(createdTime);
        return item;
    }

    private List<UserOrderMediaItemResponse> toImageItems(List<Images> images) {
        if (images == null || images.isEmpty()) {
            return Collections.emptyList();
        }
        List<UserOrderMediaItemResponse> items = new ArrayList<>();
        for (Images image : images) {
            UserOrderMediaItemResponse item = new UserOrderMediaItemResponse();
            item.setId(image.getId());
            item.setUrl(safe(image.getFileUrl()));
            item.setThumbnailUrl(safe(image.getFileUrl()));
            item.setName(StringUtils.hasText(image.getOriginalName()) ? image.getOriginalName() : safe(image.getFileName()));
            item.setMimeType(safe(image.getMimeType()));
            items.add(item);
        }
        return items;
    }

    private List<UserOrderMediaItemResponse> toVideoItems(List<Videos> videos) {
        if (videos == null || videos.isEmpty()) {
            return Collections.emptyList();
        }
        List<UserOrderMediaItemResponse> items = new ArrayList<>();
        for (Videos video : videos) {
            UserOrderMediaItemResponse item = new UserOrderMediaItemResponse();
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

    private List<UserOrderMediaItemResponse> listInspectionImages(String progressId) {
        if (!StringUtils.hasText(progressId)) {
            return Collections.emptyList();
        }
        List<Images> images = imagesService.list(
            new LambdaQueryWrapper<Images>()
                .eq(Images::getBusinessType, ORDER_INSPECTION_BUSINESS_TYPE)
                .eq(Images::getBusinessId, progressId)
                .eq(Images::getIsDelete, 0)
                .orderByAsc(Images::getCreatedTime)
        );
        return toImageItems(images);
    }

    private List<UserOrderMediaItemResponse> listInspectionVideos(String progressId) {
        if (!StringUtils.hasText(progressId)) {
            return Collections.emptyList();
        }
        List<Videos> videos = videosService.list(
            new LambdaQueryWrapper<Videos>()
                .eq(Videos::getBusinessType, ORDER_INSPECTION_BUSINESS_TYPE)
                .eq(Videos::getBusinessId, progressId)
                .eq(Videos::getIsDelete, 0)
                .orderByAsc(Videos::getCreatedTime)
        );
        return toVideoItems(videos);
    }

    private List<UserOrderMediaItemResponse> listAfterSalesImages(String applicationId) {
        if (!StringUtils.hasText(applicationId)) {
            return Collections.emptyList();
        }
        return toImageItems(
            imagesService.list(
                new LambdaQueryWrapper<Images>()
                    .eq(Images::getBusinessType, AFTER_SALES_BUSINESS_TYPE)
                    .eq(Images::getBusinessId, applicationId)
                    .eq(Images::getIsDelete, 0)
                    .orderByAsc(Images::getCreatedTime)
            )
        );
    }

    private List<UserOrderMediaItemResponse> listAfterSalesVideos(String applicationId) {
        if (!StringUtils.hasText(applicationId)) {
            return Collections.emptyList();
        }
        return toVideoItems(
            videosService.list(
                new LambdaQueryWrapper<Videos>()
                    .eq(Videos::getBusinessType, AFTER_SALES_BUSINESS_TYPE)
                    .eq(Videos::getBusinessId, applicationId)
                    .eq(Videos::getIsDelete, 0)
                    .orderByAsc(Videos::getCreatedTime)
            )
        );
    }

    private List<OrderProgress> listOrderProgress(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            return Collections.emptyList();
        }
        return orderProgressService.list(
            new LambdaQueryWrapper<OrderProgress>()
                .eq(OrderProgress::getOrderId, orderId)
                .eq(OrderProgress::getIsDelete, 0)
                .orderByAsc(OrderProgress::getCreatedTime)
        );
    }

    private OrderProgress findLatestInspectionProgress(List<OrderProgress> progressList) {
        if (progressList == null || progressList.isEmpty()) {
            return null;
        }
        for (int index = progressList.size() - 1; index >= 0; index--) {
            OrderProgress progress = progressList.get(index);
            if (parseInspectionProgress(progress) != null) {
                return progress;
            }
        }
        return null;
    }

    private String resolveProgressDescription(OrderProgress progress) {
        InspectionProgressSnapshot snapshot = parseInspectionProgress(progress);
        if (snapshot != null) {
            return buildInspectionProgressSummary(snapshot);
        }
        return safe(progress == null ? null : progress.getDescription());
    }

    private InspectionProgressSnapshot parseInspectionProgress(OrderProgress progress) {
        if (progress == null || !StringUtils.hasText(progress.getDescription())) {
            return null;
        }
        try {
            Map<String, Object> payload = OBJECT_MAPPER.readValue(progress.getDescription(), new TypeReference<Map<String, Object>>() {});
            if (!INSPECTION_PROGRESS_TYPE.equals(String.valueOf(payload.get("type")))) {
                return null;
            }
            InspectionProgressSnapshot snapshot = new InspectionProgressSnapshot();
            snapshot.setInspectionDiagnosis(stringValue(payload.get("inspectionDiagnosis")));
            snapshot.setRepairPlan(stringValue(payload.get("repairPlan")));
            snapshot.setServiceFee(stringValue(payload.get("serviceFee")));
            snapshot.setMaterialFee(stringValue(payload.get("materialFee")));
            return snapshot;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String buildInspectionProgressSummary(InspectionProgressSnapshot snapshot) {
        return "师傅已完成检查：问题=" + safe(snapshot.getInspectionDiagnosis())
            + "；维修建议=" + safe(snapshot.getRepairPlan())
            + "；服务费=" + safe(snapshot.getServiceFee())
            + "；材料费=" + safe(snapshot.getMaterialFee());
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private RepairOrders requireOwnedOrder(String orderId, String accountId) {
        RepairOrders order = repairOrdersService.getOne(
            new LambdaQueryWrapper<RepairOrders>()
                .eq(RepairOrders::getId, orderId)
                .eq(RepairOrders::getAccountId, accountId)
                .eq(RepairOrders::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private RepairOrderPayments requireOrderPayment(String orderId) {
        RepairOrderPayments payment = repairOrderPaymentsService.getOne(
            new LambdaQueryWrapper<RepairOrderPayments>()
                .eq(RepairOrderPayments::getRepairOrderId, orderId)
                .eq(RepairOrderPayments::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (payment == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "订单支付信息不存在");
        }
        return payment;
    }

    private void createTailPaymentRecord(
        RepairOrders order,
        String accountId,
        Integer paymentMethod,
        BigDecimal paymentAmount,
        long now
    ) {
        PaymentRecords record = new PaymentRecords();
        record.setId(SnowflakeIdUtil.nextPaymentRecordId());
        record.setPaymentNo(buildTailPaymentNo(record.getId()));
        record.setOrderId(order.getId());
        record.setOrderType(1);
        record.setAccountId(accountId);
        record.setPaymentMethod(paymentMethod);
        record.setPaymentAmount(normalizeMoney(paymentAmount));
        record.setPaymentStatus(PAYMENT_RECORD_STATUS_SUCCESS);
        record.setThirdPartyNo(buildThirdPartyNo(record.getPaymentNo(), paymentMethod));
        record.setPaymentTime(now);
        record.setRefundAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        record.setRemark(buildTailPaymentRemark(paymentMethod));
        record.setCreatedTime(now);
        record.setUpdatedTime(now);
        record.setIsDelete(0);
        if (!paymentRecordsService.save(record)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建尾款支付记录失败");
        }
    }

    private void saveTailPaymentProgress(
        RepairOrders order,
        BigDecimal paymentAmount,
        Integer paymentMethod,
        long now
    ) {
        OrderProgress progress = new OrderProgress();
        progress.setId(SnowflakeIdUtil.nextOrderProgressId());
        progress.setOrderId(order.getId());
        progress.setStatus(4);
        progress.setStatusName("待支付");
        progress.setDescription(
            "用户已支付尾款，支付方式："
                + getPaymentMethodText(paymentMethod)
                + "；支付金额："
                + formatMoney(normalizeMoney(paymentAmount))
        );
        progress.setOperatorId(order.getAccountId());
        progress.setOperatorType(OPERATOR_TYPE_USER);
        progress.setOperatorName("用户");
        progress.setCreatedTime(now);
        progress.setIsDelete(0);
        if (!orderProgressService.save(progress)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存订单进度失败");
        }
    }

    private int normalizePaymentMethod(Integer paymentMethod) {
        int value = paymentMethod == null ? PAYMENT_METHOD_WECHAT : paymentMethod;
        if (value != PAYMENT_METHOD_WECHAT && value != PAYMENT_METHOD_ALIPAY && value != PAYMENT_METHOD_WALLET) {
            throw new BusinessException(
                ErrorCode.PARAM_ERROR,
                "paymentMethod 仅支持 1-微信支付、2-支付宝支付、5-钱包支付"
            );
        }
        return value;
    }

    private String getPaymentMethodText(Integer paymentMethod) {
        int value = safeInt(paymentMethod);
        if (value == PAYMENT_METHOD_ALIPAY) {
            return "支付宝支付";
        }
        if (value == PAYMENT_METHOD_WALLET) {
            return "钱包支付";
        }
        return "微信支付";
    }

    private String buildTailPaymentRemark(Integer paymentMethod) {
        int value = safeInt(paymentMethod);
        if (value == PAYMENT_METHOD_WALLET) {
            return "维修订单尾款支付（钱包支付）";
        }
        if (value == PAYMENT_METHOD_ALIPAY) {
            return "维修订单尾款支付（支付宝支付）";
        }
        return "维修订单尾款支付（微信支付）";
    }

    private String buildTailPaymentNo(String paymentRecordId) {
        if (!StringUtils.hasText(paymentRecordId) || paymentRecordId.length() <= 2) {
            return "PAY" + System.currentTimeMillis();
        }
        return "PAY" + paymentRecordId.substring(2);
    }

    private String buildThirdPartyNo(String paymentNo, Integer paymentMethod) {
        int method = safeInt(paymentMethod);
        String prefix = method == PAYMENT_METHOD_ALIPAY ? "ALI" : method == PAYMENT_METHOD_WALLET ? "WLT" : "WX";
        return prefix + compactTradeNo(paymentNo);
    }

    private String compactTradeNo(String source) {
        String value = StringUtils.hasText(source) ? source.replaceAll("[^0-9A-Za-z]", "") : "";
        if (!StringUtils.hasText(value)) {
            value = String.valueOf(System.currentTimeMillis());
        }
        return value.length() > 28 ? value.substring(value.length() - 28) : value;
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean safeEquals(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }

    private boolean isOnsiteMode(ServiceTypes serviceType) {
        int mode = safeInt(serviceType == null ? null : serviceType.getType());
        return mode == 1 || mode == 2;
    }

    private boolean shouldRegenerateDoorQr(RepairOrders order, ServiceTypes serviceType) {
        return order != null
            && isOnsiteMode(serviceType)
            && safeInt(order.getStatus()) == 2;
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

    private void replaceOrderFaultDetails(
        RepairOrders order,
        String serviceTypeId,
        String accountId,
        List<UserOrderFlowModel.SubmitFaultItem> faultList,
        long now
    ) {
        if (order == null || !StringUtils.hasText(order.getId())) {
            return;
        }
        List<RepairOrderFaults> existingFaults = repairOrderFaultsService.list(
            new LambdaQueryWrapper<RepairOrderFaults>()
                .eq(RepairOrderFaults::getRepairOrderId, order.getId())
                .eq(RepairOrderFaults::getIsDelete, 0)
        );
        if (!existingFaults.isEmpty()) {
            List<String> faultIds = existingFaults.stream()
                .map(RepairOrderFaults::getId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
            if (!faultIds.isEmpty()) {
                imagesService.remove(
                    new LambdaQueryWrapper<Images>()
                        .eq(Images::getBusinessType, ORDER_FAULT_BUSINESS_TYPE)
                        .in(Images::getBusinessId, faultIds)
                );
                videosService.remove(
                    new LambdaQueryWrapper<Videos>()
                        .eq(Videos::getBusinessType, ORDER_FAULT_BUSINESS_TYPE)
                        .in(Videos::getBusinessId, faultIds)
                );
            }
            repairOrderFaultsService.remove(
                new LambdaQueryWrapper<RepairOrderFaults>()
                    .eq(RepairOrderFaults::getRepairOrderId, order.getId())
            );
        }
        saveOrderFaultDetails(order.getId(), serviceTypeId, accountId, faultList, now);
    }

    private void saveOrderFaultDetails(
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

        for (int index = 0; index < faultList.size(); index++) {
            UserOrderFlowModel.SubmitFaultItem fault = faultList.get(index);
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

            RepairOrderFaults entity = new RepairOrderFaults();
            String faultRecordId = SnowflakeIdUtil.nextRepairOrderFaultId();
            entity.setId(faultRecordId);
            entity.setRepairOrderId(orderId);
            entity.setFaultPhenomenonId(faultId);
            entity.setFaultDescription(resolveFaultDescription(fault));
            entity.setCreatedTime(now);
            entity.setUpdatedTime(now);
            entity.setIsDelete(0);
            if (!repairOrderFaultsService.save(entity)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存故障信息失败");
            }

            for (int imageIndex = 0; imageIndex < images.size(); imageIndex++) {
                saveOrderFaultImage(images.get(imageIndex), faultRecordId, accountId, now, imageIndex);
            }
            if (fault.getVideo() != null) {
                saveOrderFaultVideo(fault.getVideo(), faultRecordId, accountId, now);
            }
        }
    }

    private String resolveFaultDescription(UserOrderFlowModel.SubmitFaultItem fault) {
        String description = trimToNull(fault == null ? null : fault.getFaultDescription());
        if (StringUtils.hasText(description)) {
            return description;
        }
        String faultName = trimToNull(fault == null ? null : fault.getFaultName());
        return StringUtils.hasText(faultName) ? faultName : "";
    }

    private void saveOrderFaultImage(
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
        entity.setBusinessType(ORDER_FAULT_BUSINESS_TYPE);
        entity.setBusinessId(faultRecordId);
        entity.setCreatedTime(now);
        entity.setIsDelete(0);
        if (!imagesService.save(entity)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存故障图片失败");
        }
    }

    private void saveOrderFaultVideo(
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
        entity.setBusinessType(ORDER_FAULT_BUSINESS_TYPE);
        entity.setBusinessId(faultRecordId);
        entity.setCreatedTime(now);
        entity.setIsDelete(0);
        if (!videosService.save(entity)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存故障视失败");
        }
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

    private List<UserAfterSalesSubmitMediaItem> normalizeAfterSalesImages(List<UserAfterSalesSubmitMediaItem> images) {
        if (images == null || images.isEmpty()) {
            return Collections.emptyList();
        }
        List<UserAfterSalesSubmitMediaItem> normalized = images.stream()
            .filter(item -> item != null && StringUtils.hasText(trimToNull(item.getUrl())))
            .collect(Collectors.toList());
        int maxImageCount = getAfterSalesMaxImageCount();
        if (normalized.size() > maxImageCount) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "售后图片最多上传 " + maxImageCount + " 张");
        }
        return normalized;
    }

    private long getAfterSalesValidDays() {
        Long value = systemConfigsService.getLongConfig(
            "after_sales.valid_days",
            DEFAULT_AFTER_SALES_VALID_DAYS
        );
        return value == null || value <= 0L ? DEFAULT_AFTER_SALES_VALID_DAYS : value;
    }

    private long getAfterSalesValidMillis() {
        return getAfterSalesValidDays() * 24L * 60L * 60L * 1000L;
    }

    private int getAfterSalesMaxImageCount() {
        Integer value = systemConfigsService.getIntegerConfig(
            "after_sales.max_image_count",
            DEFAULT_MAX_AFTER_SALES_IMAGE_COUNT
        );
        return value == null || value <= 0 ? DEFAULT_MAX_AFTER_SALES_IMAGE_COUNT : value;
    }

    private UserAfterSalesSubmitMediaItem normalizeAfterSalesVideo(UserAfterSalesSubmitMediaItem video) {
        if (video == null || !StringUtils.hasText(trimToNull(video.getUrl()))) {
            return null;
        }
        return video;
    }

    private String buildEvidenceImageSnapshot(List<UserAfterSalesSubmitMediaItem> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        List<String> urls = images.stream()
            .map(UserAfterSalesSubmitMediaItem::getUrl)
            .filter(StringUtils::hasText)
            .collect(Collectors.toList());
        try {
            return urls.isEmpty() ? null : OBJECT_MAPPER.writeValueAsString(urls);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存唐图片信息失败");
        }
    }

    private void saveAfterSalesImages(
        List<UserAfterSalesSubmitMediaItem> images,
        String applicationId,
        String accountId,
        long now
    ) {
        if (images == null || images.isEmpty()) {
            return;
        }
        for (int index = 0; index < images.size(); index++) {
            UserAfterSalesSubmitMediaItem image = images.get(index);
            Images entity = new Images();
            String fileUrl = trimToNull(image.getUrl());
            String fileName = StringUtils.hasText(trimToNull(image.getName()))
                ? image.getName().trim()
                : ("after-sales-image-" + (index + 1) + ".jpg");
            entity.setId(SnowflakeIdUtil.nextImageId());
            entity.setOriginalName(fileName);
            entity.setFileName(fileName);
            entity.setFilePath(fileUrl);
            entity.setFileUrl(fileUrl);
            entity.setFileSize(image.getFileSize() == null ? 0L : image.getFileSize());
            entity.setMimeType(StringUtils.hasText(trimToNull(image.getMimeType())) ? image.getMimeType().trim() : "image/jpeg");
            entity.setWidth(image.getWidth());
            entity.setHeight(image.getHeight());
            entity.setUploaderId(accountId);
            entity.setUploaderType(1);
            entity.setBusinessType(AFTER_SALES_BUSINESS_TYPE);
            entity.setBusinessId(applicationId);
            entity.setCreatedTime(now);
            entity.setIsDelete(0);
            if (!imagesService.save(entity)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存唐图片失败");
            }
        }
    }

    private void saveAfterSalesVideo(
        UserAfterSalesSubmitMediaItem video,
        String applicationId,
        String accountId,
        long now
    ) {
        if (video == null || !StringUtils.hasText(trimToNull(video.getUrl()))) {
            return;
        }
        Videos entity = new Videos();
        String fileUrl = trimToNull(video.getUrl());
        String fileName = StringUtils.hasText(trimToNull(video.getName()))
            ? video.getName().trim()
            : "after-sales-video.mp4";
        entity.setId(SnowflakeIdUtil.nextVideoId());
        entity.setOriginalName(fileName);
        entity.setFileName(fileName);
        entity.setFilePath(fileUrl);
        entity.setFileUrl(fileUrl);
        entity.setFileSize(video.getFileSize() == null ? 0L : video.getFileSize());
        entity.setMimeType(StringUtils.hasText(trimToNull(video.getMimeType())) ? video.getMimeType().trim() : "video/mp4");
        entity.setDuration(video.getDuration());
        entity.setWidth(video.getWidth());
        entity.setHeight(video.getHeight());
        entity.setThumbnailUrl(trimToNull(video.getThumbnailUrl()));
        entity.setUploaderId(accountId);
        entity.setUploaderType(1);
        entity.setBusinessType(AFTER_SALES_BUSINESS_TYPE);
        entity.setBusinessId(applicationId);
        entity.setCreatedTime(now);
        entity.setIsDelete(0);
        if (!videosService.save(entity)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存唐视失败");
        }
    }

    private Map<String, ServiceTypes> listServiceTypeMap(List<RepairOrders> orders) {
        Set<String> ids = orders.stream()
            .map(RepairOrders::getServiceTypeId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        return serviceTypesService.list(
            new LambdaQueryWrapper<ServiceTypes>()
                .in(ServiceTypes::getId, ids)
                .eq(ServiceTypes::getIsDelete, 0)
        ).stream().collect(Collectors.toMap(ServiceTypes::getId, item -> item, (a, b) -> a));
    }

    private Map<String, ServiceCategories> listCategoryMap(Map<String, ServiceTypes> serviceTypeMap) {
        Set<String> directIds = serviceTypeMap.values().stream()
            .map(ServiceTypes::getCategoryId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        if (directIds.isEmpty()) {
            return new HashMap<>();
        }

        List<ServiceCategories> directCategories = serviceCategoriesService.list(
            new LambdaQueryWrapper<ServiceCategories>()
                .in(ServiceCategories::getId, directIds)
                .eq(ServiceCategories::getIsDelete, 0)
        );
        if (directCategories.isEmpty()) {
            return new HashMap<>();
        }

        Set<String> allIds = new LinkedHashSet<>(directIds);
        for (ServiceCategories category : directCategories) {
            allIds.addAll(parsePathIds(category.getPath()));
        }
        return serviceCategoriesService.list(
            new LambdaQueryWrapper<ServiceCategories>()
                .in(ServiceCategories::getId, allIds)
                .eq(ServiceCategories::getIsDelete, 0)
        ).stream().collect(Collectors.toMap(ServiceCategories::getId, item -> item, (a, b) -> a));
    }

    private Map<String, TechnicianAccounts> listTechnicianMap(List<RepairOrders> orders) {
        Set<String> ids = orders.stream()
            .map(RepairOrders::getTechnicianAccountId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        return technicianAccountsService.list(
            new LambdaQueryWrapper<TechnicianAccounts>()
                .in(TechnicianAccounts::getId, ids)
                .eq(TechnicianAccounts::getIsDelete, 0)
        ).stream().collect(Collectors.toMap(TechnicianAccounts::getId, item -> item, (a, b) -> a));
    }

    private Map<String, UserAddresses> listAddressMap(List<RepairOrders> orders) {
        Set<String> ids = orders.stream()
            .map(RepairOrders::getServiceAddressId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        return userAddressesService.list(
            new LambdaQueryWrapper<UserAddresses>()
                .in(UserAddresses::getId, ids)
                .eq(UserAddresses::getIsDelete, 0)
        ).stream().collect(Collectors.toMap(UserAddresses::getId, item -> item, (a, b) -> a));
    }

    private Map<String, List<RepairOrderFaults>> listFaultMap(List<RepairOrders> orders) {
        Set<String> ids = orders.stream()
            .map(RepairOrders::getId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        return repairOrderFaultsService.list(
            new LambdaQueryWrapper<RepairOrderFaults>()
                .in(RepairOrderFaults::getRepairOrderId, ids)
                .eq(RepairOrderFaults::getIsDelete, 0)
                .orderByAsc(RepairOrderFaults::getCreatedTime)
        ).stream().collect(Collectors.groupingBy(RepairOrderFaults::getRepairOrderId));
    }

    private Map<String, FaultPhenomena> listPhenomenonMap(Map<String, List<RepairOrderFaults>> faultMap) {
        Set<String> ids = faultMap.values().stream()
            .flatMap(List::stream)
            .map(RepairOrderFaults::getFaultPhenomenonId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        return faultPhenomenaService.list(
            new LambdaQueryWrapper<FaultPhenomena>()
                .in(FaultPhenomena::getId, ids)
                .eq(FaultPhenomena::getIsDelete, 0)
        ).stream().collect(Collectors.toMap(FaultPhenomena::getId, item -> item, (a, b) -> a));
    }

    private Map<String, RepairOrderPayments> listPaymentMap(List<RepairOrders> orders) {
        Set<String> ids = orders.stream()
            .map(RepairOrders::getId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        return repairOrderPaymentsService.list(
            new LambdaQueryWrapper<RepairOrderPayments>()
                .in(RepairOrderPayments::getRepairOrderId, ids)
                .eq(RepairOrderPayments::getIsDelete, 0)
                .orderByDesc(RepairOrderPayments::getCreatedTime)
        ).stream().collect(Collectors.toMap(RepairOrderPayments::getRepairOrderId, item -> item, (a, b) -> a));
    }

    private BigDecimal resolvePaidAmount(RepairOrders order, RepairOrderPayments payment) {
        if (payment == null) {
            return BigDecimal.ZERO;
        }
        Integer paymentStatus = order.getPaymentStatus();
        if (paymentStatus != null && (paymentStatus == 2 || paymentStatus == 3)) {
            return payment.getActualAmount();
        }
        return BigDecimal.ZERO;
    }

    private String buildFaultSummary(List<RepairOrderFaults> faults, Map<String, FaultPhenomena> phenomenonMap) {
        if (faults == null || faults.isEmpty()) {
            return "";
        }
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        for (RepairOrderFaults fault : faults) {
            FaultPhenomena phenomenon = phenomenonMap.get(fault.getFaultPhenomenonId());
            if (phenomenon != null && StringUtils.hasText(phenomenon.getName())) {
                parts.add(phenomenon.getName().trim());
            }
            if (StringUtils.hasText(fault.getFaultDescription())) {
                parts.add(fault.getFaultDescription().trim());
            }
        }
        return String.join("；", parts);
    }

    private String buildFullAddress(UserAddresses address) {
        if (address == null) {
            return "";
        }
        return safe(address.getProvince())
            + safe(address.getCity())
            + safe(address.getDistrict())
            + safe(address.getStreet())
            + safe(address.getDetailedAddress());
    }

    private String buildShortAddress(UserAddresses address) {
        if (address == null) {
            return "";
        }
        String shortAddress = safe(address.getStreet()) + safe(address.getDetailedAddress());
        return StringUtils.hasText(shortAddress) ? shortAddress : buildFullAddress(address);
    }

    private String buildCategoryPath(ServiceCategories category, Map<String, ServiceCategories> categoryMap) {
        if (category == null) {
            return "";
        }
        if (!StringUtils.hasText(category.getPath())) {
            return safe(category.getName());
        }
        List<String> names = new ArrayList<>();
        for (String id : parsePathIds(category.getPath())) {
            ServiceCategories current = categoryMap.get(id);
            if (current != null && StringUtils.hasText(current.getName())) {
                names.add(current.getName().trim());
            }
        }
        if (names.isEmpty()) {
            return safe(category.getName());
        }
        return String.join(" / ", names);
    }

    private List<String> parsePathIds(String path) {
        List<String> ids = new ArrayList<>();
        if (!StringUtils.hasText(path)) {
            return ids;
        }
        for (String part : path.split("/")) {
            if (StringUtils.hasText(part)) {
                ids.add(part.trim());
            }
        }
        return ids;
    }

    private String normalizeTab(String tab) {
        if (!StringUtils.hasText(tab)) {
            return "all";
        }
        String value = tab.trim();
        if ("waiting".equals(value)
            || "processing".equals(value)
            || "to-pay".equals(value)
            || "finished".equals(value)
            || "closed".equals(value)) {
            return value;
        }
        return "all";
    }

    private String getStatusText(Integer status) {
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

    private String getPaymentStatusText(RepairOrders order, RepairOrderPayments payment) {
        int value = safeInt(order == null ? null : order.getPaymentStatus());
        if (value == 2) {
            if (safeInt(order == null ? null : order.getStatus()) == 6) {
                return "已支付";
            }
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

    private boolean canUserReview(RepairOrders order, Reviews review) {
        return order != null && safeInt(order.getStatus()) == 6 && review == null;
    }

    private LoginUserInfo requireCurrentUser() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.USER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问用户订单");
        }
        return user;
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String getDisplayStatusText(RepairOrders order) {
        if (isWaitingUserConfirmCompletion(order)) {
            return "待用户确认";
        }
        return getStatusText(order == null ? null : order.getStatus());
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private void notifyWorkerOrderCanceled(RepairOrders order, ServiceTypes serviceType, String cancelReason, long now) {
        if (order == null || !StringUtils.hasText(order.getTechnicianAccountId())) {
            return;
        }
        String serviceName = serviceType == null ? "" : safe(serviceType.getName());
        String title = "订单取消通知";
        String content = new StringBuilder()
            .append("订单").append(safe(order.getOrderNo()))
            .append("已被用户取消")
            .append(StringUtils.hasText(serviceName) ? "，服务项目：" + serviceName : "")
            .append(StringUtils.hasText(cancelReason) ? "，取消原因：" + cancelReason : "")
            .toString();
        saveWorkerSystemMessage(
            order.getTechnicianAccountId(),
            title,
            content,
            WORKER_ORDER_CANCEL_MESSAGE_TYPE,
            order.getId(),
            now
        );
    }

    private void notifyWorkerAfterSalesApplied(RepairOrders order, String reason, long now) {
        if (order == null || !StringUtils.hasText(order.getTechnicianAccountId())) {
            return;
        }
        ServiceTypes serviceType = getOrderServiceType(order.getServiceTypeId());
        String serviceName = serviceType == null ? "" : safe(serviceType.getName());
        String title = "售后申请通知";
        String content = new StringBuilder()
            .append("订单").append(safe(order.getOrderNo()))
            .append("收到新的售后申请")
            .append(StringUtils.hasText(serviceName) ? "，服务项目：" + serviceName : "")
            .append(StringUtils.hasText(reason) ? "，申请原因：" + reason : "")
            .toString();
        saveWorkerSystemMessage(
            order.getTechnicianAccountId(),
            title,
            content,
            WORKER_AFTER_SALES_MESSAGE_TYPE,
            order.getId(),
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
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存维修师傅提醒消息失败");
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

    @Data
    private static final class UserOrderCancelRequest {

        private String orderId;
        private String reason;
    }

    @Data
    private static final class UserOrderUpdateRequest {

        private String orderId;
        private Boolean appointmentOnly;
        private Long appointmentTime;
        private String applianceBrand;
        private String applianceModel;
        private String purchaseDate;
        private List<UserOrderFlowModel.SubmitFaultItem> faultList = new ArrayList<>();
    }

    @Data
    private static final class UserOrderConfirmCompletionRequest {

        private String orderId;
    }

    @Data
    private static final class UserOrderAfterSalesApplyRequest {

        private String orderId;
        private String reason;
        private String description;
        private List<UserAfterSalesSubmitMediaItem> images = new ArrayList<>();
        private UserAfterSalesSubmitMediaItem video;
    }

    @Data
    private static final class UserOrderAfterSalesCancelRequest {

        private String applicationId;
    }

    @Data
    private static final class UserOrderTailPayRequest {

        private String orderId;
        private Integer paymentMethod;
    }

    private static final class InspectionProgressSnapshot {

        private String inspectionDiagnosis;
        private String repairPlan;
        private String serviceFee;
        private String materialFee;

        private String getInspectionDiagnosis() {
            return inspectionDiagnosis;
        }

        private void setInspectionDiagnosis(String inspectionDiagnosis) {
            this.inspectionDiagnosis = inspectionDiagnosis;
        }

        private String getRepairPlan() {
            return repairPlan;
        }

        private void setRepairPlan(String repairPlan) {
            this.repairPlan = repairPlan;
        }

        private String getServiceFee() {
            return serviceFee;
        }

        private void setServiceFee(String serviceFee) {
            this.serviceFee = serviceFee;
        }

        private String getMaterialFee() {
            return materialFee;
        }

        private void setMaterialFee(String materialFee) {
            this.materialFee = materialFee;
        }
    }
}

