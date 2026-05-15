package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.AccountBalances;
import com.example.backend.entity.AfterSalesApplications;
import com.example.backend.entity.FundFlows;
import com.example.backend.entity.Images;
import com.example.backend.entity.OrderItems;
import com.example.backend.entity.PaymentRecords;
import com.example.backend.entity.ProductOrders;
import com.example.backend.entity.UserAccounts;
import com.example.backend.entity.Videos;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.admin.AdminAfterSalesMediaItemResponse;
import com.example.backend.model.admin.AdminAfterSalesProcessRequest;
import com.example.backend.model.admin.AdminProductAfterSalesModel;
import com.example.backend.model.admin.AdminProductOrderModel;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.AccountBalancesService;
import com.example.backend.service.AfterSalesApplicationsService;
import com.example.backend.service.FundFlowsService;
import com.example.backend.service.ImagesService;
import com.example.backend.service.OrderItemsService;
import com.example.backend.service.PaymentRecordsService;
import com.example.backend.service.ProductOrdersService;
import com.example.backend.service.UserAccountsService;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/product-after-sales/requests")
public class AdminProductAfterSalesController {

    private static final String ACTION_APPROVE = "approve";
    private static final String ACTION_REJECT = "reject";
    private static final String ACTION_REFUND = "refund";
    private static final String AFTER_SALES_BUSINESS_TYPE = "AFTER_SALES_APPLICATION";
    private static final String PRODUCT_ORDER_REFUND_BUSINESS_TYPE = "PRODUCT_ORDER_REFUND";
    private static final int ACCOUNT_TYPE_USER = 1;
    private static final int FLOW_TYPE_INCOME = 1;
    private static final int ORDER_TYPE_PRODUCT = 2;
    private static final int ORDER_STATUS_PENDING_DELIVERY = 2;
    private static final int ORDER_STATUS_PENDING_RECEIPT = 3;
    private static final int ORDER_STATUS_PENDING_REVIEW = 4;
    private static final int ORDER_STATUS_COMPLETED = 5;
    private static final int ORDER_STATUS_REFUNDED = 7;
    private static final int PAYMENT_METHOD_WECHAT = 1;
    private static final int PAYMENT_METHOD_ALIPAY = 2;
    private static final int PAYMENT_METHOD_WALLET = 5;
    private static final int PAYMENT_STATUS_PAID = 2;
    private static final int PAYMENT_STATUS_REFUNDED = 3;
    private static final int PAYMENT_RECORD_STATUS_REFUNDED = 5;
    private static final int AFTER_SALES_TYPE_REFUND = 1;
    private static final int AFTER_SALES_TYPE_RETURN_REFUND = 2;
    private static final int AFTER_SALES_STATUS_PENDING = 1;
    private static final int AFTER_SALES_STATUS_APPROVED = 2;
    private static final int AFTER_SALES_STATUS_REJECTED = 3;
    private static final int AFTER_SALES_STATUS_COMPLETED = 5;
    private static final int AFTER_SALES_STATUS_CANCELED = 6;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final AfterSalesApplicationsService afterSalesApplicationsService;
    private final ProductOrdersService productOrdersService;
    private final OrderItemsService orderItemsService;
    private final UserAccountsService userAccountsService;
    private final AccountBalancesService accountBalancesService;
    private final FundFlowsService fundFlowsService;
    private final PaymentRecordsService paymentRecordsService;
    private final ImagesService imagesService;
    private final VideosService videosService;

    public AdminProductAfterSalesController(
        AfterSalesApplicationsService afterSalesApplicationsService,
        ProductOrdersService productOrdersService,
        OrderItemsService orderItemsService,
        UserAccountsService userAccountsService,
        AccountBalancesService accountBalancesService,
        FundFlowsService fundFlowsService,
        PaymentRecordsService paymentRecordsService,
        ImagesService imagesService,
        VideosService videosService
    ) {
        this.afterSalesApplicationsService = afterSalesApplicationsService;
        this.productOrdersService = productOrdersService;
        this.orderItemsService = orderItemsService;
        this.userAccountsService = userAccountsService;
        this.accountBalancesService = accountBalancesService;
        this.fundFlowsService = fundFlowsService;
        this.paymentRecordsService = paymentRecordsService;
        this.imagesService = imagesService;
        this.videosService = videosService;
    }

    @GetMapping
    public Result<Page<AdminProductAfterSalesModel.ListItemResponse>> listRequests(
        @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
        @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) Integer status,
        @RequestParam(value = "applicationType", required = false) Integer applicationType
    ) {
        requireAdmin();
        long currentPage = pageNum <= 0 ? 1 : pageNum;
        long currentSize = pageSize <= 0 ? 10 : pageSize;

        LambdaQueryWrapper<AfterSalesApplications> wrapper = new LambdaQueryWrapper<AfterSalesApplications>()
            .eq(AfterSalesApplications::getOrderType, ORDER_TYPE_PRODUCT)
            .eq(AfterSalesApplications::getIsDelete, 0);
        if (status != null) {
            wrapper.eq(AfterSalesApplications::getStatus, status);
        }
        if (applicationType != null) {
            wrapper.eq(AfterSalesApplications::getApplicationType, applicationType);
        }
        applyKeywordFilter(wrapper, keyword);
        wrapper.orderByAsc(AfterSalesApplications::getStatus)
            .orderByDesc(AfterSalesApplications::getCreatedTime);

        Page<AfterSalesApplications> page = afterSalesApplicationsService.page(new Page<>(currentPage, currentSize), wrapper);
        Page<AdminProductAfterSalesModel.ListItemResponse> responsePage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        responsePage.setRecords(buildListItems(page.getRecords()));
        return Result.success(responsePage);
    }

    @GetMapping("/{id}")
    public Result<AdminProductAfterSalesModel.DetailResponse> getDetail(@PathVariable("id") String id) {
        requireAdmin();
        return Result.success(buildDetailResponse(requireApplication(id)));
    }

    @PostMapping("/{id}/process")
    @Transactional(rollbackFor = Exception.class)
    public Result<AdminProductAfterSalesModel.DetailResponse> process(
        @PathVariable("id") String id,
        @RequestBody(required = false) AdminAfterSalesProcessRequest request
    ) {
        LoginUserInfo admin = requireAdmin();
        AfterSalesApplications application = requireApplication(id);
        ProductOrders order = requireOrder(application.getOrderId());
        String action = trimToNull(request == null ? null : request.getAction());
        String adminRemark = trimToNull(request == null ? null : request.getAdminRemark());
        if (!StringUtils.hasText(action)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择处理动作");
        }

        long now = System.currentTimeMillis();
        if (ACTION_APPROVE.equals(action)) {
            handleApprove(application, order, admin, adminRemark, now);
        } else if (ACTION_REJECT.equals(action)) {
            handleReject(application, admin, adminRemark, now);
        } else if (ACTION_REFUND.equals(action)) {
            handleRefund(application, order, admin, adminRemark, now);
        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的处理动作");
        }
        return Result.success(buildDetailResponse(requireApplication(id)));
    }

    private void applyKeywordFilter(LambdaQueryWrapper<AfterSalesApplications> wrapper, String keyword) {
        String normalizedKeyword = trimToNull(keyword);
        if (!StringUtils.hasText(normalizedKeyword)) {
            return;
        }

        Set<String> accountIds = userAccountsService.list(
            new LambdaQueryWrapper<UserAccounts>()
                .eq(UserAccounts::getIsDelete, 0)
                .and(q -> q.like(UserAccounts::getUsername, normalizedKeyword)
                    .or().like(UserAccounts::getPhone, normalizedKeyword))
        ).stream().map(UserAccounts::getId).filter(StringUtils::hasText).collect(Collectors.toSet());

        Set<String> orderIds = orderItemsService.list(
            new LambdaQueryWrapper<OrderItems>()
                .eq(OrderItems::getIsDelete, 0)
                .and(q -> q.like(OrderItems::getProductName, normalizedKeyword)
                    .or().like(OrderItems::getProductId, normalizedKeyword))
        ).stream().map(OrderItems::getOrderId).filter(StringUtils::hasText).collect(Collectors.toSet());

        orderIds.addAll(productOrdersService.list(
            new LambdaQueryWrapper<ProductOrders>()
                .eq(ProductOrders::getIsDelete, 0)
                .and(q -> q.like(ProductOrders::getOrderNo, normalizedKeyword)
                    .or().like(ProductOrders::getDeliveryName, normalizedKeyword)
                    .or().like(ProductOrders::getDeliveryPhone, normalizedKeyword)
                    .or().like(ProductOrders::getDeliveryAddress, normalizedKeyword))
        ).stream().map(ProductOrders::getId).filter(StringUtils::hasText).collect(Collectors.toSet()));

        Set<String> matchedApplicationIds = afterSalesApplicationsService.list(
            new LambdaQueryWrapper<AfterSalesApplications>()
                .eq(AfterSalesApplications::getOrderType, ORDER_TYPE_PRODUCT)
                .eq(AfterSalesApplications::getIsDelete, 0)
                .and(q -> q.like(AfterSalesApplications::getReason, normalizedKeyword)
                    .or().like(AfterSalesApplications::getDescription, normalizedKeyword))
        ).stream().map(AfterSalesApplications::getId).filter(StringUtils::hasText).collect(Collectors.toSet());

        wrapper.and(q -> {
            q.like(AfterSalesApplications::getOrderId, normalizedKeyword);
            if (!matchedApplicationIds.isEmpty()) {
                q.or().in(AfterSalesApplications::getId, matchedApplicationIds);
            }
            if (!accountIds.isEmpty()) {
                q.or().in(AfterSalesApplications::getAccountId, accountIds);
            }
            if (!orderIds.isEmpty()) {
                q.or().in(AfterSalesApplications::getOrderId, orderIds);
            }
        });
    }

    private List<AdminProductAfterSalesModel.ListItemResponse> buildListItems(List<AfterSalesApplications> applications) {
        if (applications == null || applications.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, ProductOrders> orderMap = listOrderMap(applications);
        Map<String, UserAccounts> userMap = listUserMap(applications);
        Map<String, List<OrderItems>> orderItemMap = listOrderItemMap(orderMap.values().stream().collect(Collectors.toList()));
        List<AdminProductAfterSalesModel.ListItemResponse> result = new ArrayList<>();
        for (AfterSalesApplications application : applications) {
            ProductOrders order = orderMap.get(application.getOrderId());
            List<OrderItems> orderItems = orderItemMap.getOrDefault(application.getOrderId(), Collections.emptyList());
            UserAccounts user = userMap.get(application.getAccountId());
            String productSummary = buildProductSummary(orderItems);

            AdminProductAfterSalesModel.ListItemResponse item = new AdminProductAfterSalesModel.ListItemResponse();
            item.setId(application.getId());
            item.setOrderId(application.getOrderId());
            item.setOrderNo(safe(order == null ? null : order.getOrderNo()));
            item.setStatus(application.getStatus());
            item.setStatusText(getAfterSalesStatusText(application.getStatus()));
            item.setApplicationType(application.getApplicationType());
            item.setApplicationTypeText(getAfterSalesApplicationTypeText(application.getApplicationType()));
            item.setReason(safe(application.getReason()));
            item.setUserId(application.getAccountId());
            item.setUserName(safe(user == null ? null : user.getUsername()));
            item.setUserPhone(safe(user == null ? null : user.getPhone()));
            item.setProductSummary(productSummary);
            item.setItemCount(calculateItemCount(orderItems));
            item.setCreatedTime(application.getCreatedTime());
            item.setUpdatedTime(application.getUpdatedTime());
            result.add(item);
        }
        return result;
    }

    private AdminProductAfterSalesModel.DetailResponse buildDetailResponse(AfterSalesApplications application) {
        ProductOrders order = requireOrder(application.getOrderId());
        List<OrderItems> orderItems = listOrderItems(order.getId());
        UserAccounts user = getUserAccount(application.getAccountId());
        PaymentRecords payment = getLatestPayment(order.getId());

        AdminProductAfterSalesModel.DetailResponse detail = new AdminProductAfterSalesModel.DetailResponse();
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
        detail.setUserName(safe(user == null ? null : user.getUsername()));
        detail.setUserPhone(safe(user == null ? null : user.getPhone()));
        detail.setOrderStatus(order.getOrderStatus());
        detail.setOrderStatusText(getOrderStatusText(order.getOrderStatus()));
        detail.setPaymentStatusText(getPaymentStatusText(order.getPaymentStatus()));
        detail.setDeliveryStatusText(getDeliveryStatusText(order.getDeliveryStatus()));
        detail.setProductSummary(buildProductSummary(orderItems));
        detail.setItemCount(calculateItemCount(orderItems));
        detail.setDeliveryName(safe(order.getDeliveryName()));
        detail.setDeliveryPhone(safe(order.getDeliveryPhone()));
        detail.setDeliveryAddress(safe(order.getDeliveryAddress()));
        detail.setDeliveryCompany(safe(order.getDeliveryCompany()));
        detail.setDeliveryNo(safe(order.getDeliveryNo()));
        detail.setTotalAmount(formatMoney(order.getTotalAmount()));
        detail.setPaidAmount(formatMoney(payment == null ? order.getActualAmount() : payment.getPaymentAmount()));
        detail.setCanApprove(canApprove(application));
        detail.setCanReject(canReject(application));
        detail.setCanRefund(canRefund(application));
        detail.setItems(orderItems.stream().map(this::buildOrderItemResponse).collect(Collectors.toCollection(ArrayList::new)));
        detail.setEvidenceImages(listAfterSalesImages(application.getId()));
        detail.setEvidenceVideos(listAfterSalesVideos(application.getId()));
        return detail;
    }

    private void handleApprove(
        AfterSalesApplications application,
        ProductOrders order,
        LoginUserInfo admin,
        String adminRemark,
        long now
    ) {
        if (!canApprove(application)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前售后申请暂不可审核通过");
        }
        if (safeInt(application.getApplicationType()) == AFTER_SALES_TYPE_REFUND) {
            doRefund(application, order, admin, adminRemark, now);
            return;
        }

        application.setStatus(AFTER_SALES_STATUS_APPROVED);
        application.setAdminId(admin.getAccountId());
        application.setAdminRemark(adminRemark);
        application.setProcessedTime(now);
        application.setUpdatedTime(now);
        if (!afterSalesApplicationsService.updateById(application)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新售后申请失败");
        }
    }

    private void handleReject(AfterSalesApplications application, LoginUserInfo admin, String adminRemark, long now) {
        if (!canReject(application)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前售后申请暂不可驳回");
        }
        if (!StringUtils.hasText(adminRemark)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请填写驳回原因");
        }
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
    }

    private void handleRefund(
        AfterSalesApplications application,
        ProductOrders order,
        LoginUserInfo admin,
        String adminRemark,
        long now
    ) {
        if (!canRefund(application)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前售后申请暂不可确认退款");
        }
        doRefund(application, order, admin, adminRemark, now);
    }

    private void doRefund(
        AfterSalesApplications application,
        ProductOrders order,
        LoginUserInfo admin,
        String adminRemark,
        long now
    ) {
        PaymentRecords payment = getLatestPayment(order.getId());
        BigDecimal refundAmount = normalizeMoney(payment == null ? order.getActualAmount() : payment.getPaymentAmount());
        String refundReason = StringUtils.hasText(adminRemark) ? adminRemark : "管理员同意商品售后退款";
        int previousOrderStatus = safeInt(order.getOrderStatus());

        application.setStatus(AFTER_SALES_STATUS_COMPLETED);
        application.setAdminId(admin.getAccountId());
        application.setAdminRemark(adminRemark);
        application.setRefundAmount(refundAmount);
        application.setProcessedTime(application.getProcessedTime() == null ? now : application.getProcessedTime());
        application.setCompletedTime(now);
        application.setUpdatedTime(now);
        if (!afterSalesApplicationsService.updateById(application)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新售后申请失败");
        }

        order.setOrderStatus(ORDER_STATUS_REFUNDED);
        order.setPaymentStatus(PAYMENT_STATUS_REFUNDED);
        order.setRefundAmount(refundAmount);
        order.setRefundReason(refundReason);
        order.setRefundTime(now);
        order.setUpdatedTime(now);
        if (previousOrderStatus == ORDER_STATUS_PENDING_RECEIPT && order.getReceiveTime() == null) {
            order.setReceiveTime(now);
        }
        if ((previousOrderStatus == ORDER_STATUS_PENDING_REVIEW || previousOrderStatus == ORDER_STATUS_COMPLETED)
            && order.getCompletionTime() == null) {
            order.setCompletionTime(now);
        }
        if (!productOrdersService.updateById(order)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新订单状态失败");
        }

        refundToUserBalance(
            order,
            refundAmount,
            payment == null ? order.getPaymentMethod() : payment.getPaymentMethod(),
            payment == null ? null : payment.getPaymentNo(),
            payment == null ? null : payment.getThirdPartyNo(),
            now
        );

        if (payment != null) {
            payment.setPaymentStatus(PAYMENT_RECORD_STATUS_REFUNDED);
            payment.setRefundAmount(refundAmount);
            payment.setRefundReason(refundReason);
            payment.setRefundTime(now);
            payment.setUpdatedTime(now);
            if (!paymentRecordsService.updateById(payment)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新支付记录失败");
            }
        }
    }

    private void refundToUserBalance(
        ProductOrders order,
        BigDecimal refundAmount,
        Integer paymentMethod,
        String paymentNo,
        String thirdPartyNo,
        long now
    ) {
        if (order == null || !StringUtils.hasText(order.getAccountId())) {
            return;
        }
        BigDecimal amount = normalizeMoney(refundAmount);
        if (amount.compareTo(ZERO) <= 0) {
            return;
        }

        FundFlows existingFlow = fundFlowsService.getOne(
            new LambdaQueryWrapper<FundFlows>()
                .eq(FundFlows::getAccountId, order.getAccountId())
                .eq(FundFlows::getAccountType, ACCOUNT_TYPE_USER)
                .eq(FundFlows::getBusinessType, PRODUCT_ORDER_REFUND_BUSINESS_TYPE)
                .eq(FundFlows::getBusinessId, order.getId())
                .eq(FundFlows::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (existingFlow != null) {
            return;
        }

        AccountBalances balance = ensureUserBalance(order.getAccountId(), now);
        BigDecimal balanceBefore = normalizeMoney(balance.getBalance());
        BigDecimal balanceAfter = balanceBefore.add(amount).setScale(2, RoundingMode.HALF_UP);

        balance.setBalance(balanceAfter);
        balance.setTotalIncome(normalizeMoney(balance.getTotalIncome()).add(amount).setScale(2, RoundingMode.HALF_UP));
        balance.setUpdatedTime(now);
        if (!accountBalancesService.updateById(balance)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户资金统计失败");
        }

        FundFlows flow = new FundFlows();
        flow.setId(SnowflakeIdUtil.nextFundFlowId());
        flow.setAccountId(order.getAccountId());
        flow.setAccountType(ACCOUNT_TYPE_USER);
        flow.setFlowType(FLOW_TYPE_INCOME);
        flow.setAmount(amount);
        flow.setBalanceBefore(balanceBefore);
        flow.setBalanceAfter(balanceAfter);
        flow.setBusinessType(PRODUCT_ORDER_REFUND_BUSINESS_TYPE);
        flow.setBusinessId(order.getId());
        flow.setDescription("商品售后退款（退回账户余额）");
        flow.setRemark(buildRefundFlowRemark(order, paymentMethod, paymentNo, thirdPartyNo));
        flow.setCreatedTime(now);
        flow.setVersion(0);
        flow.setIsDelete(0);
        if (!fundFlowsService.save(flow)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存资金流水失败");
        }
    }

    private AccountBalances ensureUserBalance(String accountId, long now) {
        AccountBalances balance = accountBalancesService.getOne(
            new LambdaQueryWrapper<AccountBalances>()
                .eq(AccountBalances::getAccountId, accountId)
                .eq(AccountBalances::getAccountType, ACCOUNT_TYPE_USER)
                .eq(AccountBalances::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (balance != null) {
            return balance;
        }

        AccountBalances created = new AccountBalances();
        created.setId(SnowflakeIdUtil.nextAccountBalanceId());
        created.setAccountId(accountId);
        created.setAccountType(ACCOUNT_TYPE_USER);
        created.setBalance(ZERO);
        created.setFrozenBalance(ZERO);
        created.setTotalIncome(ZERO);
        created.setTotalExpense(ZERO);
        created.setCreatedTime(now);
        created.setUpdatedTime(now);
        created.setVersion(0);
        created.setIsDelete(0);
        if (!accountBalancesService.save(created)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化用户账户余额失败");
        }
        return created;
    }

    private String buildRefundFlowRemark(
        ProductOrders order,
        Integer paymentMethod,
        String paymentNo,
        String thirdPartyNo
    ) {
        StringBuilder remark = new StringBuilder();
        remark.append("orderNo=").append(safe(order == null ? null : order.getOrderNo()));
        remark.append(",sourcePaymentMethod=").append(getPaymentMethodText(paymentMethod));
        if (StringUtils.hasText(paymentNo)) {
            remark.append(",paymentNo=").append(paymentNo);
        }
        if (StringUtils.hasText(thirdPartyNo)) {
            remark.append(",thirdPartyNo=").append(thirdPartyNo);
        }
        return remark.toString();
    }

    private boolean canApprove(AfterSalesApplications application) {
        return safeInt(application == null ? null : application.getStatus()) == AFTER_SALES_STATUS_PENDING;
    }

    private boolean canReject(AfterSalesApplications application) {
        return safeInt(application == null ? null : application.getStatus()) == AFTER_SALES_STATUS_PENDING;
    }

    private boolean canRefund(AfterSalesApplications application) {
        return application != null
            && safeInt(application.getApplicationType()) == AFTER_SALES_TYPE_RETURN_REFUND
            && safeInt(application.getStatus()) == AFTER_SALES_STATUS_APPROVED;
    }

    private Map<String, ProductOrders> listOrderMap(List<AfterSalesApplications> applications) {
        Set<String> orderIds = applications.stream()
            .map(AfterSalesApplications::getOrderId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        if (orderIds.isEmpty()) {
            return new HashMap<>();
        }
        return productOrdersService.list(
            new LambdaQueryWrapper<ProductOrders>()
                .in(ProductOrders::getId, orderIds)
                .eq(ProductOrders::getIsDelete, 0)
        ).stream().collect(Collectors.toMap(ProductOrders::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    private Map<String, UserAccounts> listUserMap(List<AfterSalesApplications> applications) {
        Set<String> userIds = applications.stream()
            .map(AfterSalesApplications::getAccountId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return new HashMap<>();
        }
        return userAccountsService.list(
            new LambdaQueryWrapper<UserAccounts>()
                .in(UserAccounts::getId, userIds)
                .eq(UserAccounts::getIsDelete, 0)
        ).stream().collect(Collectors.toMap(UserAccounts::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
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

    private List<OrderItems> listOrderItems(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            return Collections.emptyList();
        }
        return orderItemsService.list(
            new LambdaQueryWrapper<OrderItems>()
                .eq(OrderItems::getOrderId, orderId)
                .eq(OrderItems::getIsDelete, 0)
                .orderByAsc(OrderItems::getCreatedTime)
        );
    }

    private ProductOrders requireOrder(String orderId) {
        ProductOrders order = productOrdersService.getOne(
            new LambdaQueryWrapper<ProductOrders>()
                .eq(ProductOrders::getId, orderId)
                .eq(ProductOrders::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品订单不存在");
        }
        return order;
    }

    private AfterSalesApplications requireApplication(String id) {
        AfterSalesApplications application = afterSalesApplicationsService.getOne(
            new LambdaQueryWrapper<AfterSalesApplications>()
                .eq(AfterSalesApplications::getId, id)
                .eq(AfterSalesApplications::getOrderType, ORDER_TYPE_PRODUCT)
                .eq(AfterSalesApplications::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (application == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品售后申请不存在");
        }
        return application;
    }

    private UserAccounts getUserAccount(String userId) {
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        return userAccountsService.getById(userId);
    }

    private PaymentRecords getLatestPayment(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            return null;
        }
        return paymentRecordsService.getOne(
            new LambdaQueryWrapper<PaymentRecords>()
                .eq(PaymentRecords::getOrderId, orderId)
                .eq(PaymentRecords::getOrderType, ORDER_TYPE_PRODUCT)
                .eq(PaymentRecords::getIsDelete, 0)
                .orderByDesc(PaymentRecords::getCreatedTime)
                .last("limit 1"),
            false
        );
    }

    private AdminProductOrderModel.OrderItemResponse buildOrderItemResponse(OrderItems item) {
        AdminProductOrderModel.OrderItemResponse response = new AdminProductOrderModel.OrderItemResponse();
        response.setId(item.getId());
        response.setProductId(safe(item.getProductId()));
        response.setProductName(safe(item.getProductName()));
        response.setProductImage(safe(item.getProductImage()));
        response.setProductPrice(formatMoney(item.getProductPrice()));
        response.setQuantity(safeInt(item.getQuantity()));
        response.setTotalPrice(formatMoney(item.getTotalPrice()));
        return response;
    }

    private List<AdminAfterSalesMediaItemResponse> listAfterSalesImages(String applicationId) {
        if (!StringUtils.hasText(applicationId)) {
            return Collections.emptyList();
        }
        return imagesService.list(
            new LambdaQueryWrapper<Images>()
                .eq(Images::getBusinessType, AFTER_SALES_BUSINESS_TYPE)
                .eq(Images::getBusinessId, applicationId)
                .eq(Images::getIsDelete, 0)
                .orderByAsc(Images::getCreatedTime)
        ).stream().map(this::buildImageItem).collect(Collectors.toCollection(ArrayList::new));
    }

    private List<AdminAfterSalesMediaItemResponse> listAfterSalesVideos(String applicationId) {
        if (!StringUtils.hasText(applicationId)) {
            return Collections.emptyList();
        }
        return videosService.list(
            new LambdaQueryWrapper<Videos>()
                .eq(Videos::getBusinessType, AFTER_SALES_BUSINESS_TYPE)
                .eq(Videos::getBusinessId, applicationId)
                .eq(Videos::getIsDelete, 0)
                .orderByAsc(Videos::getCreatedTime)
        ).stream().map(this::buildVideoItem).collect(Collectors.toCollection(ArrayList::new));
    }

    private AdminAfterSalesMediaItemResponse buildImageItem(Images image) {
        AdminAfterSalesMediaItemResponse item = new AdminAfterSalesMediaItemResponse();
        item.setId(image.getId());
        item.setUrl(safe(image.getFileUrl()));
        item.setThumbnailUrl(safe(image.getFileUrl()));
        item.setName(StringUtils.hasText(image.getOriginalName()) ? image.getOriginalName() : safe(image.getFileName()));
        item.setMimeType(safe(image.getMimeType()));
        return item;
    }

    private AdminAfterSalesMediaItemResponse buildVideoItem(Videos video) {
        AdminAfterSalesMediaItemResponse item = new AdminAfterSalesMediaItemResponse();
        item.setId(video.getId());
        item.setUrl(safe(video.getFileUrl()));
        item.setThumbnailUrl(safe(video.getThumbnailUrl()));
        item.setName(StringUtils.hasText(video.getOriginalName()) ? video.getOriginalName() : safe(video.getFileName()));
        item.setMimeType(safe(video.getMimeType()));
        item.setDuration(video.getDuration());
        return item;
    }

    private LoginUserInfo requireAdmin() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问商品售后管理");
        }
        return user;
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
        if (status == AFTER_SALES_STATUS_CANCELED) {
            return "已取消";
        }
        if (status == AFTER_SALES_STATUS_COMPLETED) {
            return "已完成";
        }
        return "未知状态";
    }

    private String getOrderStatusText(Integer value) {
        int status = safeInt(value);
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
        if (status == ORDER_STATUS_REFUNDED) {
            return "已退款";
        }
        return "订单处理中";
    }

    private String getPaymentStatusText(Integer value) {
        int status = safeInt(value);
        if (status == PAYMENT_STATUS_PAID) {
            return "已支付";
        }
        if (status == PAYMENT_STATUS_REFUNDED) {
            return "已退款";
        }
        return "待支付";
    }

    private String getPaymentMethodText(Integer value) {
        int paymentMethod = safeInt(value);
        if (paymentMethod == PAYMENT_METHOD_WECHAT) {
            return "微信支付";
        }
        if (paymentMethod == PAYMENT_METHOD_ALIPAY) {
            return "支付宝支付";
        }
        if (paymentMethod == PAYMENT_METHOD_WALLET) {
            return "钱包支付";
        }
        return "未知支付方式";
    }

    private String getDeliveryStatusText(Integer value) {
        int status = safeInt(value);
        if (status == 1) {
            return "待发货";
        }
        if (status == 2) {
            return "已发货";
        }
        if (status == 3) {
            return "配送中";
        }
        if (status == 4) {
            return "已送达";
        }
        return "物流未知";
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String formatMoney(BigDecimal value) {
        return normalizeMoney(value).toPlainString();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
