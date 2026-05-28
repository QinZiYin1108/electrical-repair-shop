package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.AfterSalesApplications;
import com.example.backend.entity.OrderItems;
import com.example.backend.entity.ProductOrders;
import com.example.backend.entity.RepairOrderPayments;
import com.example.backend.entity.RepairOrders;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.admin.AdminDashboardOverviewResponse;
import com.example.backend.model.admin.AdminDashboardProductPaymentItemResponse;
import com.example.backend.model.admin.AdminDashboardProductSalesResponse;
import com.example.backend.model.admin.AdminDashboardProductSalesTrendItemResponse;
import com.example.backend.model.admin.AdminDashboardProductTopItemResponse;
import com.example.backend.model.admin.AdminDashboardStatusItemResponse;
import com.example.backend.model.admin.AdminDashboardTrendItemResponse;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.AfterSalesApplicationsService;
import com.example.backend.service.OrderItemsService;
import com.example.backend.service.ProductOrdersService;
import com.example.backend.service.ProductsService;
import com.example.backend.service.RepairOrderPaymentsService;
import com.example.backend.service.RepairOrdersService;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.service.UserAccountsService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final int PRODUCT_ORDER_STATUS_PENDING_PAYMENT = 1;
    private static final int PRODUCT_ORDER_STATUS_PENDING_DELIVERY = 2;
    private static final int PRODUCT_ORDER_STATUS_PENDING_RECEIPT = 3;
    private static final int PRODUCT_ORDER_STATUS_PENDING_REVIEW = 4;
    private static final int PRODUCT_ORDER_STATUS_COMPLETED = 5;
    private static final int PRODUCT_ORDER_STATUS_CANCELED = 6;
    private static final int PRODUCT_ORDER_STATUS_REFUNDED = 7;
    private static final int PRODUCT_PAYMENT_STATUS_PAID = 2;
    private static final int PRODUCT_PAYMENT_STATUS_REFUNDED = 3;
    private static final int PRODUCT_PAYMENT_METHOD_WECHAT = 1;
    private static final int PRODUCT_PAYMENT_METHOD_ALIPAY = 2;
    private static final int PRODUCT_PAYMENT_METHOD_WALLET = 5;

    private final UserAccountsService userAccountsService;
    private final TechnicianAccountsService technicianAccountsService;
    private final RepairOrdersService repairOrdersService;
    private final RepairOrderPaymentsService repairOrderPaymentsService;
    private final AfterSalesApplicationsService afterSalesApplicationsService;
    private final ProductOrdersService productOrdersService;
    private final OrderItemsService orderItemsService;
    private final ProductsService productsService;

    public AdminDashboardController(
        UserAccountsService userAccountsService,
        TechnicianAccountsService technicianAccountsService,
        RepairOrdersService repairOrdersService,
        RepairOrderPaymentsService repairOrderPaymentsService,
        AfterSalesApplicationsService afterSalesApplicationsService,
        ProductOrdersService productOrdersService,
        OrderItemsService orderItemsService,
        ProductsService productsService
    ) {
        this.userAccountsService = userAccountsService;
        this.technicianAccountsService = technicianAccountsService;
        this.repairOrdersService = repairOrdersService;
        this.repairOrderPaymentsService = repairOrderPaymentsService;
        this.afterSalesApplicationsService = afterSalesApplicationsService;
        this.productOrdersService = productOrdersService;
        this.orderItemsService = orderItemsService;
        this.productsService = productsService;
    }

    @GetMapping("/overview")
    public Result<AdminDashboardOverviewResponse> getOverview() {
        LoginUserInfo admin = requireAdmin();
        String storeId = (admin != null && admin.isStoreAdmin()) ? admin.getStoreId() : null;
        Set<String> storeTechIds = resolveStoreTechIds(storeId);
        Set<String> storeOrderIds = resolveStoreOrderIds(storeTechIds);
        boolean isStoreMode = storeId != null && StringUtils.hasText(storeId);
        System.out.println("[Dashboard] adminRole=" + (admin != null ? admin.getAdminRole() : "null")
            + " isStoreAdmin=" + (admin != null && admin.isStoreAdmin())
            + " storeId=" + storeId
            + " storeTechIds.size=" + (storeTechIds != null ? storeTechIds.size() : 0)
            + " storeOrderIds.size=" + (storeOrderIds != null ? storeOrderIds.size() : 0)
            + " isStoreMode=" + isStoreMode);

        long now = System.currentTimeMillis();
        long todayStart = startOfDay(now);
        long sevenDaysStart = startOfDay(now, 6);

        AdminDashboardOverviewResponse response = new AdminDashboardOverviewResponse();
        response.setTotalUsers(isStoreMode ? 0L : userAccountsService.count(
            new LambdaQueryWrapper<com.example.backend.entity.UserAccounts>()
                .eq(com.example.backend.entity.UserAccounts::getIsDelete, 0)
        ));
        // 门店管理员：师傅数只统计本门店
        if (isStoreMode) {
            response.setTotalWorkers(storeTechIds != null ? (long) storeTechIds.size() : 0L);
            response.setActiveWorkers(storeTechIds != null && !storeTechIds.isEmpty()
                ? (long) technicianAccountsService.count(new LambdaQueryWrapper<com.example.backend.entity.TechnicianAccounts>()
                    .in(com.example.backend.entity.TechnicianAccounts::getId, storeTechIds)
                    .eq(com.example.backend.entity.TechnicianAccounts::getAccountStatus, 1)
                )
                : 0L
            );
        } else {
            response.setTotalWorkers(technicianAccountsService.count(
                new LambdaQueryWrapper<com.example.backend.entity.TechnicianAccounts>()
                    .eq(com.example.backend.entity.TechnicianAccounts::getIsDelete, 0)
            ));
            response.setActiveWorkers(technicianAccountsService.count(new LambdaQueryWrapper<com.example.backend.entity.TechnicianAccounts>()
                .eq(com.example.backend.entity.TechnicianAccounts::getIsDelete, 0)
                .eq(com.example.backend.entity.TechnicianAccounts::getAccountStatus, 1)
                .in(com.example.backend.entity.TechnicianAccounts::getWorkStatus, 1, 2, 3)
            ));
        }

        response.setTotalOrders(repairOrdersService.count(buildOrderWrapper(storeTechIds)));
        response.setTodayOrders(repairOrdersService.count(buildOrderWrapper(storeTechIds).ge(RepairOrders::getCreatedTime, todayStart)));
        response.setPendingOrders(repairOrdersService.count(buildOrderWrapper(storeTechIds).in(RepairOrders::getStatus, 1, 2, 3, 4, 5)));
        response.setTodayCompletedOrders(repairOrdersService.count(buildOrderWrapper(storeTechIds).eq(RepairOrders::getStatus, 6).ge(RepairOrders::getCompletionTime, todayStart)));

        LambdaQueryWrapper<AfterSalesApplications> asWrapper = new LambdaQueryWrapper<AfterSalesApplications>()
            .eq(AfterSalesApplications::getIsDelete, 0)
            .in(AfterSalesApplications::getStatus, 1, 2, 4);
        if (isStoreMode && storeOrderIds != null) {
            asWrapper.in(AfterSalesApplications::getOrderId, storeOrderIds.isEmpty() ? Collections.singleton("-1") : storeOrderIds);
        }
        response.setPendingAfterSales(afterSalesApplicationsService.count(asWrapper));

        List<RepairOrderPayments> allPayments = repairOrderPaymentsService.list(buildPaymentWrapper(storeOrderIds));
        List<RepairOrderPayments> todayPayments = repairOrderPaymentsService.list(
            buildPaymentWrapper(storeOrderIds).ge(RepairOrderPayments::getPaymentTime, todayStart)
        );

        List<RepairOrders> refundedOrders = repairOrdersService.list(
            buildOrderWrapper(storeTechIds).gt(RepairOrders::getRefundAmount, BigDecimal.ZERO)
        );

        BigDecimal totalGrossIncome = sumActualAmount(allPayments);
        BigDecimal todayIncome = sumActualAmount(todayPayments);
        BigDecimal totalRefundAmount = sumRefundAmount(refundedOrders);

        response.setTotalGrossIncome(totalGrossIncome);
        response.setTodayIncome(todayIncome);
        response.setTotalRefundAmount(totalRefundAmount);
        response.setTotalNetIncome(toMoney(totalGrossIncome.subtract(totalRefundAmount)));
        response.setRecentTrend(buildTrend(sevenDaysStart, now, storeTechIds, storeOrderIds));
        response.setOrderStatusDistribution(buildStatusDistribution(storeTechIds));
        return Result.success(response);
    }

    @GetMapping("/product-sales")
    public Result<AdminDashboardProductSalesResponse> getProductSales() {
        LoginUserInfo admin = requireAdmin();
        String storeId = (admin != null && admin.isStoreAdmin()) ? admin.getStoreId() : null;
        boolean isStoreMode = storeId != null && StringUtils.hasText(storeId);
        System.out.println("[Dashboard] product-sales - adminRole=" + (admin != null ? admin.getAdminRole() : "null")
            + " isStoreAdmin=" + (admin != null && admin.isStoreAdmin())
            + " storeId=" + storeId + " isStoreMode=" + isStoreMode);

        long now = System.currentTimeMillis();
        long todayStart = startOfDay(now);
        long sevenDaysStart = startOfDay(now, 6);

        // 门店管理员：查询本门店的商品ID，过滤商品订单
        Set<String> storeProductIds = resolveStoreProductIds(storeId);
        List<ProductOrders> allOrders;
        if (isStoreMode && storeProductIds != null) {
            List<OrderItems> storeItems = orderItemsService.list(
                new LambdaQueryWrapper<OrderItems>()
                    .in(OrderItems::getProductId, storeProductIds.isEmpty() ? Collections.singleton("-1") : storeProductIds)
                    .eq(OrderItems::getIsDelete, 0)
            );
            Set<String> storeProductOrderIds = storeItems.stream().map(OrderItems::getOrderId).collect(Collectors.toSet());
            allOrders = storeProductOrderIds.isEmpty() ? Collections.emptyList() : productOrdersService.list(
                new LambdaQueryWrapper<ProductOrders>()
                    .eq(ProductOrders::getIsDelete, 0)
                    .in(ProductOrders::getId, storeProductOrderIds)
            );
        } else {
            allOrders = productOrdersService.list(
                new LambdaQueryWrapper<ProductOrders>()
                    .eq(ProductOrders::getIsDelete, 0)
            );
        }
        Set<String> paidOrderIds = allOrders.stream()
            .filter(this::isPaidProductOrder)
            .map(ProductOrders::getId)
            .filter(this::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, List<OrderItems>> paidOrderItemMap = listProductOrderItemMap(paidOrderIds);

        AdminDashboardProductSalesResponse response = new AdminDashboardProductSalesResponse();
        response.setTotalOrderCount((long) allOrders.size());
        response.setTotalPaidOrderCount(allOrders.stream().filter(this::isPaidProductOrder).count());
        response.setTodayPaidOrderCount(allOrders.stream()
            .filter(this::isPaidProductOrder)
            .filter(order -> isAfterTime(order.getPaymentTime(), todayStart))
            .count());
        response.setPendingDeliveryOrderCount(allOrders.stream().filter(this::isPendingDeliveryOrder).count());
        response.setRefundedOrderCount(allOrders.stream().filter(this::isRefundedProductOrder).count());
        response.setTotalSoldQuantity(sumOrderItemQuantity(paidOrderItemMap));
        response.setTotalSalesAmount(sumProductOrderActualAmount(allOrders, 0L));
        response.setTodaySalesAmount(sumProductOrderActualAmount(allOrders, todayStart));
        response.setTotalRefundAmount(sumProductRefundAmount(allOrders));
        response.setRecentTrend(buildProductSalesTrend(sevenDaysStart, now, allOrders, paidOrderItemMap));
        response.setOrderStatusDistribution(buildProductOrderStatusDistribution(allOrders));
        response.setTopProducts(buildProductTopProducts(allOrders, paidOrderItemMap));
        response.setPaymentMethodDistribution(buildProductPaymentDistribution(allOrders));
        return Result.success(response);
    }

    private List<AdminDashboardTrendItemResponse> buildTrend(long startTime, long now, Set<String> storeTechIds, Set<String> storeOrderIds) {
        Map<LocalDate, AdminDashboardTrendItemResponse> trendMap = new LinkedHashMap<>();
        LocalDate startDate = Instant.ofEpochMilli(startTime).atZone(ZONE_ID).toLocalDate();
        LocalDate endDate = Instant.ofEpochMilli(now).atZone(ZONE_ID).toLocalDate();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            AdminDashboardTrendItemResponse item = new AdminDashboardTrendItemResponse();
            item.setDateLabel(date.format(DATE_LABEL_FORMATTER));
            item.setOrderCount(0L);
            item.setCompletedCount(0L);
            item.setIncome(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            trendMap.put(date, item);
        }

        LambdaQueryWrapper<RepairOrders> orderWrapper = new LambdaQueryWrapper<RepairOrders>()
            .eq(RepairOrders::getIsDelete, 0)
            .ge(RepairOrders::getCreatedTime, startTime);
        if (storeTechIds != null) {
            orderWrapper.in(RepairOrders::getTechnicianAccountId, storeTechIds.isEmpty() ? Collections.singleton("-1") : storeTechIds);
        }
        List<RepairOrders> createdOrders = repairOrdersService.list(orderWrapper);
        for (RepairOrders order : createdOrders) {
            LocalDate date = toLocalDate(order.getCreatedTime());
            AdminDashboardTrendItemResponse item = trendMap.get(date);
            if (item != null) {
                item.setOrderCount(item.getOrderCount() + 1);
            }
        }

        LambdaQueryWrapper<RepairOrders> completedWrapper = new LambdaQueryWrapper<RepairOrders>()
            .eq(RepairOrders::getIsDelete, 0)
            .eq(RepairOrders::getStatus, 6)
            .ge(RepairOrders::getCompletionTime, startTime);
        if (storeTechIds != null) {
            completedWrapper.in(RepairOrders::getTechnicianAccountId, storeTechIds.isEmpty() ? Collections.singleton("-1") : storeTechIds);
        }
        List<RepairOrders> completedOrders = repairOrdersService.list(completedWrapper);
        for (RepairOrders order : completedOrders) {
            LocalDate date = toLocalDate(order.getCompletionTime());
            AdminDashboardTrendItemResponse item = trendMap.get(date);
            if (item != null) {
                item.setCompletedCount(item.getCompletedCount() + 1);
            }
        }

        LambdaQueryWrapper<RepairOrderPayments> paymentWrapper = new LambdaQueryWrapper<RepairOrderPayments>()
            .eq(RepairOrderPayments::getIsDelete, 0)
            .gt(RepairOrderPayments::getActualAmount, BigDecimal.ZERO)
            .ge(RepairOrderPayments::getPaymentTime, startTime);
        if (storeOrderIds != null) {
            paymentWrapper.in(RepairOrderPayments::getRepairOrderId, storeOrderIds.isEmpty() ? Collections.singleton("-1") : storeOrderIds);
        }
        List<RepairOrderPayments> recentPayments = repairOrderPaymentsService.list(paymentWrapper);
        for (RepairOrderPayments payment : recentPayments) {
            LocalDate date = toLocalDate(payment.getPaymentTime());
            AdminDashboardTrendItemResponse item = trendMap.get(date);
            if (item != null) {
                item.setIncome(toMoney(item.getIncome().add(safeMoney(payment.getActualAmount()))));
            }
        }
        return new ArrayList<>(trendMap.values());
    }

    private List<AdminDashboardProductSalesTrendItemResponse> buildProductSalesTrend(
        long startTime,
        long now,
        List<ProductOrders> orders,
        Map<String, List<OrderItems>> orderItemMap
    ) {
        Map<LocalDate, AdminDashboardProductSalesTrendItemResponse> trendMap = new LinkedHashMap<>();
        LocalDate startDate = Instant.ofEpochMilli(startTime).atZone(ZONE_ID).toLocalDate();
        LocalDate endDate = Instant.ofEpochMilli(now).atZone(ZONE_ID).toLocalDate();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            AdminDashboardProductSalesTrendItemResponse item = new AdminDashboardProductSalesTrendItemResponse();
            item.setDateLabel(date.format(DATE_LABEL_FORMATTER));
            item.setPaidOrderCount(0L);
            item.setSoldQuantity(0L);
            item.setSalesAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            trendMap.put(date, item);
        }

        for (ProductOrders order : orders) {
            if (!isPaidProductOrder(order) || !isAfterTime(order.getPaymentTime(), startTime)) {
                continue;
            }
            LocalDate date = toLocalDate(order.getPaymentTime());
            AdminDashboardProductSalesTrendItemResponse item = trendMap.get(date);
            if (item == null) {
                continue;
            }
            item.setPaidOrderCount(item.getPaidOrderCount() + 1);
            item.setSoldQuantity(item.getSoldQuantity() + sumQuantity(orderItemMap.get(order.getId())));
            item.setSalesAmount(toMoney(item.getSalesAmount().add(safeMoney(order.getActualAmount()))));
        }
        return new ArrayList<>(trendMap.values());
    }

    private List<AdminDashboardStatusItemResponse> buildStatusDistribution(Set<String> storeTechIds) {
        int[] statuses = {1, 2, 3, 4, 5, 6, 7, 8};
        List<AdminDashboardStatusItemResponse> result = new ArrayList<>();
        for (int status : statuses) {
            AdminDashboardStatusItemResponse item = new AdminDashboardStatusItemResponse();
            item.setStatus(status);
            item.setLabel(resolveOrderStatusText(status));
            LambdaQueryWrapper<RepairOrders> statusWrapper = new LambdaQueryWrapper<RepairOrders>()
                .eq(RepairOrders::getIsDelete, 0)
                .eq(RepairOrders::getStatus, status);
            if (storeTechIds != null) {
                statusWrapper.in(RepairOrders::getTechnicianAccountId, storeTechIds.isEmpty() ? Collections.singleton("-1") : storeTechIds);
            }
            item.setCount(repairOrdersService.count(statusWrapper));
            result.add(item);
        }
        return result;
    }

    private List<AdminDashboardStatusItemResponse> buildProductOrderStatusDistribution(List<ProductOrders> orders) {
        int[] statuses = {
            PRODUCT_ORDER_STATUS_PENDING_PAYMENT,
            PRODUCT_ORDER_STATUS_PENDING_DELIVERY,
            PRODUCT_ORDER_STATUS_PENDING_RECEIPT,
            PRODUCT_ORDER_STATUS_PENDING_REVIEW,
            PRODUCT_ORDER_STATUS_COMPLETED,
            PRODUCT_ORDER_STATUS_CANCELED,
            PRODUCT_ORDER_STATUS_REFUNDED
        };
        List<AdminDashboardStatusItemResponse> result = new ArrayList<>();
        for (int status : statuses) {
            AdminDashboardStatusItemResponse item = new AdminDashboardStatusItemResponse();
            item.setStatus(status);
            item.setLabel(resolveProductOrderStatusText(status));
            item.setCount(orders.stream().filter(order -> safeInt(order.getOrderStatus()) == status).count());
            result.add(item);
        }
        return result;
    }

    private List<AdminDashboardProductTopItemResponse> buildProductTopProducts(
        List<ProductOrders> orders,
        Map<String, List<OrderItems>> orderItemMap
    ) {
        Map<String, AdminDashboardProductTopItemResponse> productMap = new LinkedHashMap<>();
        Map<String, Set<String>> productOrderIds = new LinkedHashMap<>();

        for (ProductOrders order : orders) {
            if (!isPaidProductOrder(order) || !hasText(order.getId())) {
                continue;
            }
            List<OrderItems> items = orderItemMap.getOrDefault(order.getId(), Collections.emptyList());
            for (OrderItems item : items) {
                String productKey = hasText(item.getProductId()) ? item.getProductId() : item.getProductName();
                if (!hasText(productKey)) {
                    continue;
                }
                AdminDashboardProductTopItemResponse product = productMap.computeIfAbsent(productKey, key -> {
                    AdminDashboardProductTopItemResponse response = new AdminDashboardProductTopItemResponse();
                    response.setProductId(item.getProductId());
                    response.setProductName(item.getProductName());
                    response.setProductImage(item.getProductImage());
                    response.setQuantity(0L);
                    response.setSalesAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                    response.setOrderCount(0L);
                    return response;
                });
                if (!hasText(product.getProductName()) && hasText(item.getProductName())) {
                    product.setProductName(item.getProductName());
                }
                if (!hasText(product.getProductImage()) && hasText(item.getProductImage())) {
                    product.setProductImage(item.getProductImage());
                }
                if (!hasText(product.getProductId()) && hasText(item.getProductId())) {
                    product.setProductId(item.getProductId());
                }
                product.setQuantity(product.getQuantity() + safeLong(item.getQuantity()));
                product.setSalesAmount(toMoney(product.getSalesAmount().add(resolveOrderItemAmount(item))));
                productOrderIds.computeIfAbsent(productKey, key -> new LinkedHashSet<>()).add(order.getId());
            }
        }

        List<AdminDashboardProductTopItemResponse> result = new ArrayList<>(productMap.values());
        for (Map.Entry<String, AdminDashboardProductTopItemResponse> entry : productMap.entrySet()) {
            entry.getValue().setOrderCount((long) productOrderIds.getOrDefault(entry.getKey(), Collections.emptySet()).size());
        }
        result.sort((left, right) -> {
            int quantityCompare = Long.compare(
                right == null || right.getQuantity() == null ? 0L : right.getQuantity(),
                left == null || left.getQuantity() == null ? 0L : left.getQuantity()
            );
            if (quantityCompare != 0) {
                return quantityCompare;
            }
            return (right == null || right.getSalesAmount() == null ? BigDecimal.ZERO : right.getSalesAmount())
                .compareTo(left == null || left.getSalesAmount() == null ? BigDecimal.ZERO : left.getSalesAmount());
        });
        return result.stream().limit(8).collect(Collectors.toCollection(ArrayList::new));
    }

    private List<AdminDashboardProductPaymentItemResponse> buildProductPaymentDistribution(List<ProductOrders> orders) {
        Map<Integer, AdminDashboardProductPaymentItemResponse> paymentMap = new LinkedHashMap<>();
        for (ProductOrders order : orders) {
            if (!isPaidProductOrder(order)) {
                continue;
            }
            int paymentMethod = safeInt(order.getPaymentMethod());
            AdminDashboardProductPaymentItemResponse item = paymentMap.computeIfAbsent(paymentMethod, key -> {
                AdminDashboardProductPaymentItemResponse response = new AdminDashboardProductPaymentItemResponse();
                response.setPaymentMethod(key);
                response.setLabel(resolveProductPaymentMethodText(key));
                response.setCount(0L);
                response.setAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                return response;
            });
            item.setCount(item.getCount() + 1);
            item.setAmount(toMoney(item.getAmount().add(safeMoney(order.getActualAmount()))));
        }
        List<AdminDashboardProductPaymentItemResponse> result = new ArrayList<>(paymentMap.values());
        result.sort(
            Comparator.comparing(AdminDashboardProductPaymentItemResponse::getAmount, Comparator.nullsLast(BigDecimal::compareTo)).reversed()
        );
        return result;
    }

    private Map<String, List<OrderItems>> listProductOrderItemMap(Set<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return orderItemsService.list(
            new LambdaQueryWrapper<OrderItems>()
                .in(OrderItems::getOrderId, orderIds)
                .eq(OrderItems::getIsDelete, 0)
                .orderByAsc(OrderItems::getCreatedTime)
        ).stream().collect(Collectors.groupingBy(OrderItems::getOrderId, LinkedHashMap::new, Collectors.toList()));
    }

    private long sumOrderItemQuantity(Map<String, List<OrderItems>> orderItemMap) {
        long total = 0L;
        for (List<OrderItems> items : orderItemMap.values()) {
            total += sumQuantity(items);
        }
        return total;
    }

    private long sumQuantity(List<OrderItems> items) {
        long total = 0L;
        if (items == null) {
            return total;
        }
        for (OrderItems item : items) {
            total += safeLong(item == null ? null : item.getQuantity());
        }
        return total;
    }

    private BigDecimal sumProductOrderActualAmount(List<ProductOrders> orders, long startTime) {
        BigDecimal total = BigDecimal.ZERO;
        if (orders == null) {
            return toMoney(total);
        }
        for (ProductOrders order : orders) {
            if (!isPaidProductOrder(order)) {
                continue;
            }
            if (startTime > 0L && !isAfterTime(order.getPaymentTime(), startTime)) {
                continue;
            }
            total = total.add(safeMoney(order.getActualAmount()));
        }
        return toMoney(total);
    }

    private BigDecimal sumProductRefundAmount(List<ProductOrders> orders) {
        BigDecimal total = BigDecimal.ZERO;
        if (orders == null) {
            return toMoney(total);
        }
        for (ProductOrders order : orders) {
            total = total.add(safeMoney(order == null ? null : order.getRefundAmount()));
        }
        return toMoney(total);
    }

    private BigDecimal sumActualAmount(List<RepairOrderPayments> payments) {
        BigDecimal total = BigDecimal.ZERO;
        if (payments == null) {
            return toMoney(total);
        }
        for (RepairOrderPayments payment : payments) {
            total = total.add(safeMoney(payment == null ? null : payment.getActualAmount()));
        }
        return toMoney(total);
    }

    private BigDecimal sumRefundAmount(List<RepairOrders> orders) {
        BigDecimal total = BigDecimal.ZERO;
        if (orders == null) {
            return toMoney(total);
        }
        for (RepairOrders order : orders) {
            total = total.add(safeMoney(order == null ? null : order.getRefundAmount()));
        }
        return toMoney(total);
    }

    private long startOfDay(long timestamp) {
        return startOfDay(timestamp, 0);
    }

    private long startOfDay(long timestamp, int minusDays) {
        LocalDate date = Instant.ofEpochMilli(timestamp).atZone(ZONE_ID).toLocalDate().minusDays(minusDays);
        return date.atStartOfDay(ZONE_ID).toInstant().toEpochMilli();
    }

    private LocalDate toLocalDate(Long timestamp) {
        if (timestamp == null || timestamp <= 0L) {
            return null;
        }
        return Instant.ofEpochMilli(timestamp).atZone(ZONE_ID).toLocalDate();
    }

    private BigDecimal safeMoney(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private BigDecimal toMoney(BigDecimal amount) {
        return safeMoney(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveOrderItemAmount(OrderItems item) {
        if (item == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (item.getTotalPrice() != null) {
            return toMoney(item.getTotalPrice());
        }
        return toMoney(safeMoney(item.getProductPrice()).multiply(BigDecimal.valueOf(safeLong(item.getQuantity()))));
    }

    private boolean isPaidProductOrder(ProductOrders order) {
        int paymentStatus = safeInt(order == null ? null : order.getPaymentStatus());
        return paymentStatus == PRODUCT_PAYMENT_STATUS_PAID || paymentStatus == PRODUCT_PAYMENT_STATUS_REFUNDED;
    }

    private boolean isPendingDeliveryOrder(ProductOrders order) {
        if (order == null) {
            return false;
        }
        return safeInt(order.getOrderStatus()) == PRODUCT_ORDER_STATUS_PENDING_DELIVERY && safeInt(order.getPaymentStatus()) == PRODUCT_PAYMENT_STATUS_PAID;
    }

    private boolean isRefundedProductOrder(ProductOrders order) {
        if (order == null) {
            return false;
        }
        return safeInt(order.getOrderStatus()) == PRODUCT_ORDER_STATUS_REFUNDED
            || safeInt(order.getPaymentStatus()) == PRODUCT_PAYMENT_STATUS_REFUNDED
            || safeMoney(order.getRefundAmount()).compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean isAfterTime(Long timestamp, long startTime) {
        return timestamp != null && timestamp >= startTime;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private long safeLong(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String resolveOrderStatusText(int status) {
        return switch (status) {
            case 1 -> "待接单";
            case 2 -> "已接单";
            case 3 -> "上门中";
            case 4 -> "维修中";
            case 5 -> "待支付";
            case 6 -> "已完成";
            case 7 -> "已取消";
            case 8 -> "已退款";
            default -> "未知状态";
        };
    }

    private String resolveProductOrderStatusText(int status) {
        return switch (status) {
            case PRODUCT_ORDER_STATUS_PENDING_PAYMENT -> "待支付";
            case PRODUCT_ORDER_STATUS_PENDING_DELIVERY -> "待发货";
            case PRODUCT_ORDER_STATUS_PENDING_RECEIPT -> "待收货";
            case PRODUCT_ORDER_STATUS_PENDING_REVIEW -> "待评价";
            case PRODUCT_ORDER_STATUS_COMPLETED -> "已完成";
            case PRODUCT_ORDER_STATUS_CANCELED -> "已取消";
            case PRODUCT_ORDER_STATUS_REFUNDED -> "已退款";
            default -> "未知状态";
        };
    }

    private String resolveProductPaymentMethodText(int paymentMethod) {
        return switch (paymentMethod) {
            case PRODUCT_PAYMENT_METHOD_WECHAT -> "微信支付";
            case PRODUCT_PAYMENT_METHOD_ALIPAY -> "支付宝";
            case PRODUCT_PAYMENT_METHOD_WALLET -> "钱包支付";
            default -> "其他方式";
        };
    }

    private LambdaQueryWrapper<RepairOrders> buildOrderWrapper(Set<String> storeTechIds) {
        LambdaQueryWrapper<RepairOrders> wrapper = new LambdaQueryWrapper<RepairOrders>()
            .eq(RepairOrders::getIsDelete, 0);
        if (storeTechIds != null) {
            wrapper.in(RepairOrders::getTechnicianAccountId, storeTechIds.isEmpty() ? Collections.singleton("-1") : storeTechIds);
        }
        return wrapper;
    }

    private LambdaQueryWrapper<RepairOrderPayments> buildPaymentWrapper(Set<String> storeOrderIds) {
        LambdaQueryWrapper<RepairOrderPayments> wrapper = new LambdaQueryWrapper<RepairOrderPayments>()
            .eq(RepairOrderPayments::getIsDelete, 0)
            .gt(RepairOrderPayments::getActualAmount, BigDecimal.ZERO);
        if (storeOrderIds != null) {
            wrapper.in(RepairOrderPayments::getRepairOrderId, storeOrderIds.isEmpty() ? Collections.singleton("-1") : storeOrderIds);
        }
        return wrapper;
    }

    private Set<String> resolveStoreTechIds(String storeId) {
        if (!StringUtils.hasText(storeId)) return null;
        return technicianAccountsService.list(
            new LambdaQueryWrapper<com.example.backend.entity.TechnicianAccounts>()
                .eq(com.example.backend.entity.TechnicianAccounts::getStoreId, storeId)
                .eq(com.example.backend.entity.TechnicianAccounts::getIsDelete, 0)
        ).stream().map(com.example.backend.entity.TechnicianAccounts::getId).collect(Collectors.toSet());
    }

    private Set<String> resolveStoreProductIds(String storeId) {
        if (!StringUtils.hasText(storeId)) return null;
        List<com.example.backend.entity.Products> products = productsService.list(
            new LambdaQueryWrapper<com.example.backend.entity.Products>()
                .eq(com.example.backend.entity.Products::getStoreId, storeId)
                .eq(com.example.backend.entity.Products::getIsDelete, 0)
        );
        return products.stream().map(com.example.backend.entity.Products::getId).collect(Collectors.toSet());
    }

    private Set<String> resolveStoreOrderIds(Set<String> techIds) {
        if (techIds == null || techIds.isEmpty()) return Collections.emptySet();
        return repairOrdersService.list(
            new LambdaQueryWrapper<RepairOrders>()
                .in(RepairOrders::getTechnicianAccountId, techIds)
                .eq(RepairOrders::getIsDelete, 0)
        ).stream().map(RepairOrders::getId).collect(Collectors.toSet());
    }

    private LoginUserInfo requireAdmin() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问仪表盘");
        }
        return user;
    }
}
