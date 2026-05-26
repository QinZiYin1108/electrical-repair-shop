package com.example.backend.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.common.system.SystemConfigRegistry;
import com.example.backend.entity.AfterSalesApplications;
import com.example.backend.entity.Images;
import com.example.backend.entity.OrderItems;
import com.example.backend.entity.PaymentRecords;
import com.example.backend.entity.ProductOrders;
import com.example.backend.entity.Reviews;
import com.example.backend.entity.Videos;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.user.UserAfterSalesSubmitMediaItem;
import com.example.backend.model.user.UserOrderMediaItemResponse;
import com.example.backend.model.user.UserProductOrderModel;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.AfterSalesApplicationsService;
import com.example.backend.service.ImagesService;
import com.example.backend.service.OrderItemsService;
import com.example.backend.service.PaymentRecordsService;
import com.example.backend.service.ProductOrdersService;
import com.example.backend.service.ReviewsService;
import com.example.backend.service.SystemConfigsService;
import com.example.backend.service.VideosService;
import com.example.backend.utils.id.SnowflakeIdUtil;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user/product-orders")
public class UserProductOrderController {

    private static final String AFTER_SALES_BUSINESS_TYPE = "AFTER_SALES_APPLICATION";
    private static final int ORDER_TYPE_PRODUCT = 2;
    private static final int TARGET_TYPE_PRODUCT = 2;
    private static final int ORDER_STATUS_PENDING_PAYMENT = 1;
    private static final int ORDER_STATUS_PENDING_DELIVERY = 2;
    private static final int ORDER_STATUS_PENDING_RECEIPT = 3;
    private static final int ORDER_STATUS_PENDING_REVIEW = 4;
    private static final int ORDER_STATUS_COMPLETED = 5;
    private static final int ORDER_STATUS_CANCELED = 6;
    private static final int ORDER_STATUS_REFUNDED = 7;
    private static final int PAYMENT_STATUS_PENDING = 1;
    private static final int PAYMENT_STATUS_PAID = 2;
    private static final int PAYMENT_STATUS_REFUNDED = 3;
    private static final int DELIVERY_STATUS_PENDING = 1;
    private static final int DELIVERY_STATUS_SHIPPED = 2;
    private static final int DELIVERY_STATUS_IN_TRANSIT = 3;
    private static final int DELIVERY_STATUS_DELIVERED = 4;
    private static final int PAYMENT_METHOD_WECHAT = 1;
    private static final int PAYMENT_METHOD_ALIPAY = 2;
    private static final int PAYMENT_METHOD_WALLET = 5;
    private static final int AFTER_SALES_TYPE_REFUND = 1;
    private static final int AFTER_SALES_TYPE_RETURN_REFUND = 2;
    private static final int AFTER_SALES_STATUS_PENDING = 1;
    private static final int AFTER_SALES_STATUS_APPROVED = 2;
    private static final int AFTER_SALES_STATUS_REJECTED = 3;
    private static final int AFTER_SALES_STATUS_PROCESSING = 4;
    private static final int AFTER_SALES_STATUS_COMPLETED = 5;
    private static final int AFTER_SALES_STATUS_CANCELED = 6;
    private static final int DEFAULT_MAX_AFTER_SALES_IMAGE_COUNT = 5;
    private static final long DEFAULT_AFTER_SALES_VALID_DAYS = 7L;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ProductOrdersService productOrdersService;
    private final OrderItemsService orderItemsService;
    private final PaymentRecordsService paymentRecordsService;
    private final ReviewsService reviewsService;
    private final AfterSalesApplicationsService afterSalesApplicationsService;
    private final ImagesService imagesService;
    private final VideosService videosService;
    private final SystemConfigsService systemConfigsService;

    public UserProductOrderController(
        ProductOrdersService productOrdersService,
        OrderItemsService orderItemsService,
        PaymentRecordsService paymentRecordsService,
        ReviewsService reviewsService,
        AfterSalesApplicationsService afterSalesApplicationsService,
        ImagesService imagesService,
        VideosService videosService,
        SystemConfigsService systemConfigsService
    ) {
        this.productOrdersService = productOrdersService;
        this.orderItemsService = orderItemsService;
        this.paymentRecordsService = paymentRecordsService;
        this.reviewsService = reviewsService;
        this.afterSalesApplicationsService = afterSalesApplicationsService;
        this.imagesService = imagesService;
        this.videosService = videosService;
        this.systemConfigsService = systemConfigsService;
    }

    @GetMapping("/list")
    public Result<List<UserProductOrderModel.ListItemResponse>> listOrders(
        @RequestParam(value = "tab", required = false) String tab
    ) {
        LoginUserInfo user = requireCurrentUser();
        List<ProductOrders> orders = productOrdersService.list(buildListQuery(user.getAccountId(), normalizeTab(tab)));
        if (orders.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        Map<String, List<OrderItems>> orderItemMap = listOrderItemMap(orders);
        Map<String, Reviews> reviewMap = listReviewMap(orders, user.getAccountId());
        Map<String, AfterSalesApplications> afterSalesMap = listLatestAfterSalesMap(orders);
        List<UserProductOrderModel.ListItemResponse> items = new ArrayList<>();
        for (ProductOrders order : orders) {
            List<OrderItems> orderItems = orderItemMap.getOrDefault(order.getId(), Collections.emptyList());
            Reviews review = reviewMap.get(order.getId());
            AfterSalesApplications afterSalesApplication = afterSalesMap.get(order.getId());
            UserProductOrderModel.ListItemResponse item = new UserProductOrderModel.ListItemResponse();
            item.setId(order.getId());
            item.setOrderNo(safe(order.getOrderNo()));
            item.setOrderStatus(order.getOrderStatus());
            item.setOrderStatusText(getOrderStatusText(order.getOrderStatus()));
            item.setPaymentStatus(order.getPaymentStatus());
            item.setPaymentStatusText(getPaymentStatusText(order.getPaymentStatus()));
            item.setDeliveryStatus(order.getDeliveryStatus());
            item.setDeliveryStatusText(getDeliveryStatusText(order.getDeliveryStatus()));
            item.setFirstProductImage(orderItems.isEmpty() ? "" : safe(orderItems.get(0).getProductImage()));
            item.setProductSummary(buildProductSummary(orderItems));
            item.setItemCount(calculateItemCount(orderItems));
            item.setTotalAmount(formatMoney(order.getTotalAmount()));
            item.setActualAmount(formatMoney(order.getActualAmount()));
            item.setCreatedTime(order.getCreatedTime());
            item.setPaymentTime(order.getPaymentTime());
            item.setDeliveryTime(order.getDeliveryTime());
            item.setCanConfirmReceipt(canConfirmReceipt(order));
            item.setCanReview(canReview(order, review));
            item.setHasReview(review != null);
            item.setReviewId(review == null ? "" : safe(review.getId()));
            item.setCanApplyAfterSales(canUserApplyAfterSales(order, afterSalesApplication));
            item.setHasAfterSalesEntry(Boolean.TRUE.equals(item.getCanApplyAfterSales()) || afterSalesApplication != null);
            item.setAfterSalesTip(buildAfterSalesTip(order, afterSalesApplication));
            item.setAfterSalesApplication(buildAfterSalesSummary(afterSalesApplication));
            items.add(item);
        }
        return Result.success(items);
    }

    @GetMapping("/detail")
    public Result<UserProductOrderModel.DetailResponse> getDetail(@RequestParam("orderId") String orderId) {
        LoginUserInfo user = requireCurrentUser();
        return Result.success(buildDetailResponse(requireOwnedOrder(orderId, user.getAccountId())));
    }

    @GetMapping("/after-sales/detail")
    public Result<UserProductOrderModel.AfterSalesDetailResponse> getAfterSalesDetail(@RequestParam("orderId") String orderId) {
        LoginUserInfo user = requireCurrentUser();
        ProductOrders order = requireOwnedOrder(orderId, user.getAccountId());
        return Result.success(buildAfterSalesDetailResponse(order));
    }

    @PostMapping("/confirm-receipt")
    @Transactional(rollbackFor = Exception.class)
    public Result<UserProductOrderModel.DetailResponse> confirmReceipt(
        @RequestBody(required = false) UserProductOrderModel.ConfirmReceiptRequest request
    ) {
        LoginUserInfo user = requireCurrentUser();
        String orderId = request == null ? null : trimToNull(request.getOrderId());
        if (!StringUtils.hasText(orderId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单ID不能为空");
        }

        ProductOrders order = requireOwnedOrder(orderId, user.getAccountId());
        if (!canConfirmReceipt(order)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前订单暂不可确认收货");
        }

        long now = System.currentTimeMillis();
        order.setOrderStatus(ORDER_STATUS_COMPLETED);
        order.setDeliveryStatus(DELIVERY_STATUS_DELIVERED);
        order.setReceiveTime(now);
        order.setCompletionTime(now);
        order.setUpdatedTime(now);
        if (!productOrdersService.updateById(order)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "确认收货失败");
        }
        return Result.success(buildDetailResponse(requireOwnedOrder(orderId, user.getAccountId())));
    }

    @PostMapping("/after-sales/apply")
    @Transactional(rollbackFor = Exception.class)
    public Result<UserProductOrderModel.AfterSalesDetailResponse> applyAfterSales(
        @RequestBody(required = false) UserProductOrderModel.AfterSalesApplyRequest request
    ) {
        LoginUserInfo user = requireCurrentUser();
        String orderId = request == null ? null : trimToNull(request.getOrderId());
        if (!StringUtils.hasText(orderId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单ID不能为空");
        }

        ProductOrders order = requireOwnedOrder(orderId, user.getAccountId());
        AfterSalesApplications latestApplication = getLatestAfterSalesApplication(order.getId());
        if (!canUserApplyAfterSales(order, latestApplication)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, buildAfterSalesUnavailableMessage(order, latestApplication));
        }

        Integer applicationType = request == null ? null : request.getApplicationType();
        validateAfterSalesApplicationType(order, applicationType);
        String reason = trimToNull(request == null ? null : request.getReason());
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择售后原因");
        }

        List<UserAfterSalesSubmitMediaItem> images = normalizeAfterSalesImages(request == null ? null : request.getImages());
        UserAfterSalesSubmitMediaItem video = normalizeAfterSalesVideo(request == null ? null : request.getVideo());
        long now = System.currentTimeMillis();

        AfterSalesApplications application = new AfterSalesApplications();
        application.setId(SnowflakeIdUtil.nextAfterSalesApplicationId());
        application.setOrderId(order.getId());
        application.setOrderType(ORDER_TYPE_PRODUCT);
        application.setAccountId(user.getAccountId());
        application.setApplicationType(applicationType);
        application.setReason(reason);
        application.setDescription(trimToNull(request == null ? null : request.getDescription()));
        application.setEvidenceImages(buildEvidenceImageSnapshot(images));
        application.setContactPhone(trimToNull(order.getDeliveryPhone()));
        application.setContactAddress(trimToNull(order.getDeliveryAddress()));
        application.setRefundAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        application.setStatus(AFTER_SALES_STATUS_PENDING);
        application.setCreatedTime(now);
        application.setUpdatedTime(now);
        application.setVersion(0);
        application.setIsDelete(0);
        if (!afterSalesApplicationsService.save(application)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "提交售后申请失败");
        }

        saveAfterSalesImages(images, application.getId(), user.getAccountId(), now);
        saveAfterSalesVideo(video, application.getId(), user.getAccountId(), now);
        return Result.success(buildAfterSalesDetailResponse(requireOwnedOrder(orderId, user.getAccountId())));
    }

    @PostMapping("/after-sales/cancel")
    @Transactional(rollbackFor = Exception.class)
    public Result<UserProductOrderModel.AfterSalesDetailResponse> cancelAfterSales(
        @RequestBody(required = false) UserProductOrderModel.AfterSalesCancelRequest request
    ) {
        LoginUserInfo user = requireCurrentUser();
        String orderId = request == null ? null : trimToNull(request.getOrderId());
        if (!StringUtils.hasText(orderId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单ID不能为空");
        }

        ProductOrders order = requireOwnedOrder(orderId, user.getAccountId());
        AfterSalesApplications application = getLatestAfterSalesApplication(order.getId());
        if (application == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "售后申请不存在");
        }
        if (!canUserCancelAfterSales(application)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前售后申请暂不可取消");
        }

        long now = System.currentTimeMillis();
        application.setStatus(AFTER_SALES_STATUS_CANCELED);
        application.setCompletedTime(now);
        application.setUpdatedTime(now);
        if (!afterSalesApplicationsService.updateById(application)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "取消售后申请失败");
        }
        return Result.success(buildAfterSalesDetailResponse(requireOwnedOrder(orderId, user.getAccountId())));
    }

    private LambdaQueryWrapper<ProductOrders> buildListQuery(String accountId, String tab) {
        LambdaQueryWrapper<ProductOrders> wrapper = new LambdaQueryWrapper<ProductOrders>()
            .eq(ProductOrders::getAccountId, accountId)
            .eq(ProductOrders::getIsDelete, 0);
        if ("pending-delivery".equals(tab)) {
            wrapper.eq(ProductOrders::getOrderStatus, ORDER_STATUS_PENDING_DELIVERY);
        } else if ("pending-receipt".equals(tab)) {
            wrapper.eq(ProductOrders::getOrderStatus, ORDER_STATUS_PENDING_RECEIPT);
        } else if ("finished".equals(tab)) {
            wrapper.in(ProductOrders::getOrderStatus, ORDER_STATUS_PENDING_REVIEW, ORDER_STATUS_COMPLETED);
        } else if ("closed".equals(tab)) {
            wrapper.in(ProductOrders::getOrderStatus, ORDER_STATUS_CANCELED, ORDER_STATUS_REFUNDED);
        }
        return wrapper.orderByDesc(ProductOrders::getUpdatedTime)
            .orderByDesc(ProductOrders::getCreatedTime);
    }

    private UserProductOrderModel.DetailResponse buildDetailResponse(ProductOrders order) {
        List<ProductOrders> orders = Collections.singletonList(order);
        Map<String, List<OrderItems>> orderItemMap = listOrderItemMap(orders);
        Map<String, PaymentRecords> paymentMap = listPaymentMap(orders);
        Reviews review = reviewsService.getUserProductOrderReviewEntity(order.getId(), order.getAccountId());
        AfterSalesApplications afterSalesApplication = getLatestAfterSalesApplication(order.getId());
        List<OrderItems> orderItems = orderItemMap.getOrDefault(order.getId(), Collections.emptyList());
        PaymentRecords payment = paymentMap.get(order.getId());

        UserProductOrderModel.DetailResponse response = new UserProductOrderModel.DetailResponse();
        response.setId(order.getId());
        response.setOrderNo(safe(order.getOrderNo()));
        response.setOrderStatus(order.getOrderStatus());
        response.setOrderStatusText(getOrderStatusText(order.getOrderStatus()));
        response.setPaymentStatus(order.getPaymentStatus());
        response.setPaymentStatusText(getPaymentStatusText(order.getPaymentStatus()));
        response.setDeliveryStatus(order.getDeliveryStatus());
        response.setDeliveryStatusText(getDeliveryStatusText(order.getDeliveryStatus()));
        response.setDeliveryName(safe(order.getDeliveryName()));
        response.setDeliveryPhone(safe(order.getDeliveryPhone()));
        response.setDeliveryAddress(safe(order.getDeliveryAddress()));
        response.setDeliveryCompany(safe(order.getDeliveryCompany()));
        response.setDeliveryNo(safe(order.getDeliveryNo()));
        response.setItemCount(calculateItemCount(orderItems));
        response.setProductSummary(buildProductSummary(orderItems));
        response.setProductAmount(formatMoney(order.getProductAmount()));
        response.setShippingFee(formatMoney(order.getShippingFee()));
        response.setDiscountAmount(formatMoney(order.getDiscountAmount()));
        response.setTotalAmount(formatMoney(order.getTotalAmount()));
        response.setActualAmount(formatMoney(order.getActualAmount()));
        response.setPaymentMethod(order.getPaymentMethod());
        response.setPaymentMethodText(getPaymentMethodText(order.getPaymentMethod()));
        response.setRemark(safe(order.getRemark()));
        response.setCancelReason(safe(order.getCancelReason()));
        response.setCancelTime(order.getCancelTime());
        response.setRefundReason(safe(order.getRefundReason()));
        response.setRefundAmount(formatMoney(order.getRefundAmount()));
        response.setRefundTime(order.getRefundTime());
        response.setCreatedTime(order.getCreatedTime());
        response.setPaymentTime(order.getPaymentTime());
        response.setDeliveryTime(order.getDeliveryTime());
        response.setReceiveTime(order.getReceiveTime());
        response.setCompletionTime(order.getCompletionTime());
        response.setCanConfirmReceipt(canConfirmReceipt(order));
        response.setCanReview(canReview(order, review));
        response.setHasReview(review != null);
        response.setReviewId(review == null ? "" : safe(review.getId()));
        response.setCanApplyAfterSales(canUserApplyAfterSales(order, afterSalesApplication));
        response.setHasAfterSalesEntry(Boolean.TRUE.equals(response.getCanApplyAfterSales()) || afterSalesApplication != null);
        response.setAfterSalesTip(buildAfterSalesTip(order, afterSalesApplication));
        response.setAfterSalesApplication(buildAfterSalesSummary(afterSalesApplication));
        response.setItems(orderItems.stream().map(this::buildOrderItemResponse).collect(Collectors.toCollection(ArrayList::new)));
        if (payment != null) {
            response.setPaymentNo(safe(payment.getPaymentNo()));
            response.setThirdPartyNo(safe(payment.getThirdPartyNo()));
            response.setPaymentAmount(formatMoney(payment.getPaymentAmount()));
            response.setPaymentRemark(safe(payment.getRemark()));
            if (!StringUtils.hasText(response.getPaymentMethodText())) {
                response.setPaymentMethodText(getPaymentMethodText(payment.getPaymentMethod()));
            }
        }
        return response;
    }

    private UserProductOrderModel.AfterSalesDetailResponse buildAfterSalesDetailResponse(ProductOrders order) {
        AfterSalesApplications application = getLatestAfterSalesApplication(order.getId());
        List<OrderItems> orderItems = listOrderItemMap(Collections.singletonList(order)).getOrDefault(order.getId(), Collections.emptyList());

        UserProductOrderModel.AfterSalesDetailResponse response = new UserProductOrderModel.AfterSalesDetailResponse();
        response.setOrderId(order.getId());
        response.setOrderNo(safe(order.getOrderNo()));
        response.setOrderStatus(order.getOrderStatus());
        response.setOrderStatusText(getOrderStatusText(order.getOrderStatus()));
        response.setPaymentStatus(order.getPaymentStatus());
        response.setPaymentStatusText(getPaymentStatusText(order.getPaymentStatus()));
        response.setDeliveryStatus(order.getDeliveryStatus());
        response.setDeliveryStatusText(getDeliveryStatusText(order.getDeliveryStatus()));
        response.setProductSummary(buildProductSummary(orderItems));
        response.setItemCount(calculateItemCount(orderItems));
        response.setDeliveryName(safe(order.getDeliveryName()));
        response.setDeliveryPhone(safe(order.getDeliveryPhone()));
        response.setDeliveryAddress(safe(order.getDeliveryAddress()));
        response.setCanApplyAfterSales(canUserApplyAfterSales(order, application));
        response.setAfterSalesTip(buildAfterSalesTip(order, application));
        response.setApplicationTypeOptions(buildAfterSalesTypeOptions(order));
        response.setApplication(buildAfterSalesApplicationDetail(application));
        return response;
    }

    private UserProductOrderModel.OrderItemResponse buildOrderItemResponse(OrderItems item) {
        UserProductOrderModel.OrderItemResponse response = new UserProductOrderModel.OrderItemResponse();
        response.setId(item.getId());
        response.setProductId(safe(item.getProductId()));
        response.setProductName(safe(item.getProductName()));
        response.setProductImage(safe(item.getProductImage()));
        response.setProductPrice(formatMoney(item.getProductPrice()));
        response.setQuantity(safeInt(item.getQuantity()));
        response.setTotalPrice(formatMoney(item.getTotalPrice()));
        return response;
    }

    private UserProductOrderModel.AfterSalesSummary buildAfterSalesSummary(AfterSalesApplications application) {
        if (application == null) {
            return null;
        }
        UserProductOrderModel.AfterSalesSummary summary = new UserProductOrderModel.AfterSalesSummary();
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

    private UserProductOrderModel.AfterSalesApplicationDetailResponse buildAfterSalesApplicationDetail(AfterSalesApplications application) {
        if (application == null) {
            return null;
        }
        UserProductOrderModel.AfterSalesApplicationDetailResponse detail = new UserProductOrderModel.AfterSalesApplicationDetailResponse();
        detail.setId(application.getId());
        detail.setApplicationType(application.getApplicationType());
        detail.setApplicationTypeText(getAfterSalesApplicationTypeText(application.getApplicationType()));
        detail.setStatus(application.getStatus());
        detail.setStatusText(getAfterSalesStatusText(application.getStatus()));
        detail.setReason(safe(application.getReason()));
        detail.setDescription(safe(application.getDescription()));
        detail.setRefundAmount(formatMoney(application.getRefundAmount()));
        detail.setAdminRemark(safe(application.getAdminRemark()));
        detail.setCanCancel(canUserCancelAfterSales(application));
        detail.setCreatedTime(application.getCreatedTime());
        detail.setUpdatedTime(application.getUpdatedTime());
        detail.setProcessedTime(application.getProcessedTime());
        detail.setCompletedTime(application.getCompletedTime());
        detail.setEvidenceImages(listAfterSalesImages(application.getId()));
        detail.setEvidenceVideos(listAfterSalesVideos(application.getId()));
        return detail;
    }

    private List<UserProductOrderModel.AfterSalesTypeOption> buildAfterSalesTypeOptions(ProductOrders order) {
        List<UserProductOrderModel.AfterSalesTypeOption> options = new ArrayList<>();
        if (canApplyRefundOnly(order)) {
            UserProductOrderModel.AfterSalesTypeOption option = new UserProductOrderModel.AfterSalesTypeOption();
            option.setValue(AFTER_SALES_TYPE_REFUND);
            option.setLabel("仅退款");
            option.setDescription("订单还未发货，可直接申请退款");
            options.add(option);
        }
        if (canApplyReturnRefund(order)) {
            UserProductOrderModel.AfterSalesTypeOption option = new UserProductOrderModel.AfterSalesTypeOption();
            option.setValue(AFTER_SALES_TYPE_RETURN_REFUND);
            option.setLabel("退货退款");
            option.setDescription("商品已发货后，可提交退货退款申请");
            options.add(option);
        }
        return options;
    }

    private Map<String, List<OrderItems>> listOrderItemMap(List<ProductOrders> orders) {
        Set<String> orderIds = orders.stream()
            .map(ProductOrders::getId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        if (orderIds.isEmpty()) {
            return new HashMap<>();
        }
        return orderItemsService.list(
            new LambdaQueryWrapper<OrderItems>()
                .in(OrderItems::getOrderId, orderIds)
                .eq(OrderItems::getIsDelete, 0)
                .orderByAsc(OrderItems::getCreatedTime)
        ).stream().collect(Collectors.groupingBy(OrderItems::getOrderId, LinkedHashMap::new, Collectors.toList()));
    }

    private Map<String, PaymentRecords> listPaymentMap(List<ProductOrders> orders) {
        Set<String> orderIds = orders.stream()
            .map(ProductOrders::getId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        if (orderIds.isEmpty()) {
            return new HashMap<>();
        }
        return paymentRecordsService.list(
            new LambdaQueryWrapper<PaymentRecords>()
                .in(PaymentRecords::getOrderId, orderIds)
                .eq(PaymentRecords::getOrderType, ORDER_TYPE_PRODUCT)
                .eq(PaymentRecords::getIsDelete, 0)
                .orderByDesc(PaymentRecords::getCreatedTime)
        ).stream().collect(Collectors.toMap(PaymentRecords::getOrderId, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    private Map<String, Reviews> listReviewMap(List<ProductOrders> orders, String accountId) {
        Set<String> orderIds = orders.stream()
            .map(ProductOrders::getId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        if (orderIds.isEmpty() || !StringUtils.hasText(accountId)) {
            return new HashMap<>();
        }
        Map<String, Reviews> reviewMap = new LinkedHashMap<>();
        reviewsService.list(
            new LambdaQueryWrapper<Reviews>()
                .in(Reviews::getOrderId, orderIds)
                .eq(Reviews::getAccountId, accountId)
                .eq(Reviews::getOrderType, ORDER_TYPE_PRODUCT)
                .eq(Reviews::getTargetType, TARGET_TYPE_PRODUCT)
                .eq(Reviews::getIsDelete, 0)
                .orderByAsc(Reviews::getCreatedTime)
        ).forEach(review -> {
            if (review != null && StringUtils.hasText(review.getOrderId()) && !reviewMap.containsKey(review.getOrderId())) {
                reviewMap.put(review.getOrderId(), review);
            }
        });
        return reviewMap;
    }

    private Map<String, AfterSalesApplications> listLatestAfterSalesMap(List<ProductOrders> orders) {
        Set<String> orderIds = orders.stream()
            .map(ProductOrders::getId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (orderIds.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, AfterSalesApplications> result = new LinkedHashMap<>();
        afterSalesApplicationsService.list(
            new LambdaQueryWrapper<AfterSalesApplications>()
                .in(AfterSalesApplications::getOrderId, orderIds)
                .eq(AfterSalesApplications::getOrderType, ORDER_TYPE_PRODUCT)
                .eq(AfterSalesApplications::getIsDelete, 0)
                .orderByDesc(AfterSalesApplications::getUpdatedTime)
                .orderByDesc(AfterSalesApplications::getCreatedTime)
        ).forEach(item -> {
            if (item != null && StringUtils.hasText(item.getOrderId()) && !result.containsKey(item.getOrderId())) {
                result.put(item.getOrderId(), item);
            }
        });
        return result;
    }

    private AfterSalesApplications getLatestAfterSalesApplication(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            return null;
        }
        return afterSalesApplicationsService.getOne(
            new LambdaQueryWrapper<AfterSalesApplications>()
                .eq(AfterSalesApplications::getOrderId, orderId)
                .eq(AfterSalesApplications::getOrderType, ORDER_TYPE_PRODUCT)
                .eq(AfterSalesApplications::getIsDelete, 0)
                .orderByDesc(AfterSalesApplications::getUpdatedTime)
                .orderByDesc(AfterSalesApplications::getCreatedTime)
                .last("limit 1"),
            false
        );
    }

    private ProductOrders requireOwnedOrder(String orderId, String accountId) {
        ProductOrders order = productOrdersService.getOne(
            new LambdaQueryWrapper<ProductOrders>()
                .eq(ProductOrders::getId, orderId)
                .eq(ProductOrders::getAccountId, accountId)
                .eq(ProductOrders::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品订单不存在");
        }
        return order;
    }

    private LoginUserInfo requireCurrentUser() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.USER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问商品订单");
        }
        return user;
    }

    private boolean canConfirmReceipt(ProductOrders order) {
        return order != null
            && safeInt(order.getOrderStatus()) == ORDER_STATUS_PENDING_RECEIPT
            && safeInt(order.getPaymentStatus()) == PAYMENT_STATUS_PAID
            && safeInt(order.getDeliveryStatus()) >= DELIVERY_STATUS_SHIPPED;
    }

    private boolean canReview(ProductOrders order, Reviews review) {
        int orderStatus = safeInt(order == null ? null : order.getOrderStatus());
        return order != null
            && review == null
            && (orderStatus == ORDER_STATUS_PENDING_REVIEW || orderStatus == ORDER_STATUS_COMPLETED);
    }

    private boolean canUserApplyAfterSales(ProductOrders order, AfterSalesApplications latestApplication) {
        if (order == null || safeInt(order.getPaymentStatus()) != PAYMENT_STATUS_PAID) {
            return false;
        }
        if (isAfterSalesProcessing(latestApplication)) {
            return false;
        }
        int orderStatus = safeInt(order.getOrderStatus());
        if (orderStatus == ORDER_STATUS_REFUNDED || orderStatus == ORDER_STATUS_CANCELED) {
            return false;
        }
        return canApplyRefundOnly(order) || canApplyReturnRefund(order);
    }

    private boolean canApplyRefundOnly(ProductOrders order) {
        return order != null
            && safeInt(order.getPaymentStatus()) == PAYMENT_STATUS_PAID
            && safeInt(order.getOrderStatus()) == ORDER_STATUS_PENDING_DELIVERY;
    }

    private boolean canApplyReturnRefund(ProductOrders order) {
        if (order == null || safeInt(order.getPaymentStatus()) != PAYMENT_STATUS_PAID) {
            return false;
        }
        int orderStatus = safeInt(order.getOrderStatus());
        if (orderStatus == ORDER_STATUS_PENDING_RECEIPT) {
            return true;
        }
        if (orderStatus == ORDER_STATUS_PENDING_REVIEW || orderStatus == ORDER_STATUS_COMPLETED) {
            return isAfterSalesWithinWindow(order);
        }
        return false;
    }

    private boolean isAfterSalesProcessing(AfterSalesApplications application) {
        int status = safeInt(application == null ? null : application.getStatus());
        return status == AFTER_SALES_STATUS_PENDING
            || status == AFTER_SALES_STATUS_APPROVED
            || status == AFTER_SALES_STATUS_PROCESSING;
    }

    private boolean canUserCancelAfterSales(AfterSalesApplications application) {
        return safeInt(application == null ? null : application.getStatus()) == AFTER_SALES_STATUS_PENDING;
    }

    private boolean isAfterSalesWithinWindow(ProductOrders order) {
        if (order == null) {
            return false;
        }
        long baseTime = order.getCompletionTime() == null || order.getCompletionTime() <= 0L
            ? (order.getReceiveTime() == null ? 0L : order.getReceiveTime())
            : order.getCompletionTime();
        if (baseTime <= 0L) {
            return false;
        }
        return System.currentTimeMillis() - baseTime <= getAfterSalesValidMillis();
    }

    private void validateAfterSalesApplicationType(ProductOrders order, Integer applicationType) {
        int type = safeInt(applicationType);
        if (type != AFTER_SALES_TYPE_REFUND && type != AFTER_SALES_TYPE_RETURN_REFUND) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择售后类型");
        }
        if (type == AFTER_SALES_TYPE_REFUND && !canApplyRefundOnly(order)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前订单仅支持申请退货退款");
        }
        if (type == AFTER_SALES_TYPE_RETURN_REFUND && !canApplyReturnRefund(order)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前订单不支持申请退货退款");
        }
    }

    private int calculateItemCount(List<OrderItems> orderItems) {
        return (orderItems == null ? Collections.<OrderItems>emptyList() : orderItems).stream()
            .map(OrderItems::getQuantity)
            .filter(item -> item != null && item > 0)
            .reduce(0, Integer::sum);
    }

    private String buildProductSummary(List<OrderItems> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return "";
        }
        OrderItems first = orderItems.get(0);
        String firstName = safe(first.getProductName());
        int size = orderItems.size();
        if (size <= 1) {
            return firstName;
        }
        return firstName + " 等" + size + "件商品";
    }

    private String buildAfterSalesTip(ProductOrders order, AfterSalesApplications latestApplication) {
        if (isAfterSalesProcessing(latestApplication)) {
            return "当前已有商品售后申请处理中，请耐心等待";
        }
        if (canApplyRefundOnly(order)) {
            return "订单待发货时可申请仅退款，提交后由管理员审核处理";
        }
        if (canApplyReturnRefund(order)) {
            int orderStatus = safeInt(order == null ? null : order.getOrderStatus());
            if (orderStatus == ORDER_STATUS_PENDING_RECEIPT) {
                return "商品已发货，可提交退货退款申请，审核通过后退款将退回账户余额";
            }
            return "订单完成后的" + getAfterSalesValidDays() + "天内可申请退货退款，提交后由管理员审核处理";
        }
        return buildAfterSalesUnavailableMessage(order, latestApplication);
    }

    private String buildAfterSalesUnavailableMessage(ProductOrders order, AfterSalesApplications latestApplication) {
        if (isAfterSalesProcessing(latestApplication)) {
            return "当前已有商品售后申请处理中";
        }
        if (order == null) {
            return "当前订单暂不支持申请售后";
        }
        if (safeInt(order.getPaymentStatus()) != PAYMENT_STATUS_PAID) {
            return "订单支付完成后才可申请售后";
        }
        int status = safeInt(order.getOrderStatus());
        if (status == ORDER_STATUS_REFUNDED) {
            return "订单已退款，无法再次申请售后";
        }
        if (status == ORDER_STATUS_CANCELED) {
            return "订单已关闭，无法申请售后";
        }
        if (status == ORDER_STATUS_PENDING_PAYMENT) {
            return "待支付订单暂不支持申请售后";
        }
        if (status == ORDER_STATUS_PENDING_DELIVERY) {
            return "当前订单仅支持申请仅退款";
        }
        if (status == ORDER_STATUS_PENDING_RECEIPT) {
            return "当前订单可申请退货退款";
        }
        if ((status == ORDER_STATUS_PENDING_REVIEW || status == ORDER_STATUS_COMPLETED) && !isAfterSalesWithinWindow(order)) {
            return "订单完成已超过" + getAfterSalesValidDays() + "天，无法申请售后";
        }
        return "当前订单暂不支持申请售后";
    }

    private String normalizeTab(String tab) {
        if (!StringUtils.hasText(tab)) {
            return "all";
        }
        String value = tab.trim();
        if ("pending-delivery".equals(value)
            || "pending-receipt".equals(value)
            || "finished".equals(value)
            || "closed".equals(value)) {
            return value;
        }
        return "all";
    }

    private String getOrderStatusText(Integer value) {
        int status = safeInt(value);
        if (status == ORDER_STATUS_PENDING_PAYMENT) {
            return "待支付";
        }
        if (status == ORDER_STATUS_PENDING_DELIVERY) {
            return "待发货";
        }
        if (status == ORDER_STATUS_PENDING_RECEIPT) {
            return "待收货";
        }
        if (status == ORDER_STATUS_PENDING_REVIEW) {
            return "待评价";
        }
        if (status == ORDER_STATUS_COMPLETED) {
            return "已完成";
        }
        if (status == ORDER_STATUS_CANCELED) {
            return "已取消";
        }
        if (status == ORDER_STATUS_REFUNDED) {
            return "已退款";
        }
        return "未知状态";
    }

    private String getPaymentStatusText(Integer value) {
        int status = safeInt(value);
        if (status == PAYMENT_STATUS_PENDING) {
            return "待支付";
        }
        if (status == PAYMENT_STATUS_PAID) {
            return "已支付";
        }
        if (status == PAYMENT_STATUS_REFUNDED) {
            return "已退款";
        }
        return "未知状态";
    }

    private String getDeliveryStatusText(Integer value) {
        int status = safeInt(value);
        if (status == DELIVERY_STATUS_PENDING) {
            return "待发货";
        }
        if (status == DELIVERY_STATUS_SHIPPED) {
            return "已发货";
        }
        if (status == DELIVERY_STATUS_IN_TRANSIT) {
            return "配送中";
        }
        if (status == DELIVERY_STATUS_DELIVERED) {
            return "已送达";
        }
        return "未知状态";
    }

    private String getPaymentMethodText(Integer value) {
        int paymentMethod = safeInt(value);
        if (paymentMethod == PAYMENT_METHOD_WECHAT) {
            return "微信支付";
        }
        if (paymentMethod == PAYMENT_METHOD_ALIPAY) {
            return "支付宝";
        }
        if (paymentMethod == PAYMENT_METHOD_WALLET) {
            return "钱包支付";
        }
        return "";
    }

    private String getAfterSalesApplicationTypeText(Integer value) {
        int type = safeInt(value);
        if (type == AFTER_SALES_TYPE_REFUND) {
            return "仅退款";
        }
        if (type == AFTER_SALES_TYPE_RETURN_REFUND) {
            return "退货退款";
        }
        return "售后";
    }

    private String getAfterSalesStatusText(Integer value) {
        int status = safeInt(value);
        if (status == AFTER_SALES_STATUS_PENDING) {
            return "待审核";
        }
        if (status == AFTER_SALES_STATUS_APPROVED) {
            return "审核通过";
        }
        if (status == AFTER_SALES_STATUS_REJECTED) {
            return "审核拒绝";
        }
        if (status == AFTER_SALES_STATUS_PROCESSING) {
            return "处理中";
        }
        if (status == AFTER_SALES_STATUS_COMPLETED) {
            return "已完成";
        }
        if (status == AFTER_SALES_STATUS_CANCELED) {
            return "已取消";
        }
        return "未知状态";
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
            throw new BusinessException(ErrorCode.PARAM_ERROR, "售后图片最多上传" + maxImageCount + "张");
        }
        return normalized;
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
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存售后图片信息失败");
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
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存售后图片失败");
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
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存售后视频失败");
        }
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

    private long getAfterSalesValidDays() {
        Long value = systemConfigsService.getLongConfig(
            SystemConfigRegistry.AFTER_SALES_VALID_DAYS,
            DEFAULT_AFTER_SALES_VALID_DAYS
        );
        return value == null || value <= 0L ? DEFAULT_AFTER_SALES_VALID_DAYS : value;
    }

    private long getAfterSalesValidMillis() {
        return getAfterSalesValidDays() * 24L * 60L * 60L * 1000L;
    }

    private int getAfterSalesMaxImageCount() {
        Integer value = systemConfigsService.getIntegerConfig(
            SystemConfigRegistry.AFTER_SALES_MAX_IMAGE_COUNT,
            DEFAULT_MAX_AFTER_SALES_IMAGE_COUNT
        );
        return value == null || value <= 0 ? DEFAULT_MAX_AFTER_SALES_IMAGE_COUNT : value;
    }

    private String formatMoney(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
