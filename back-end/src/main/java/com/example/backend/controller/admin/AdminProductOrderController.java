package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.OrderItems;
import com.example.backend.entity.PaymentRecords;
import com.example.backend.entity.ProductOrders;
import com.example.backend.entity.UserAccounts;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.admin.AdminProductOrderModel;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.entity.Products;
import com.example.backend.service.OrderItemsService;
import com.example.backend.service.PaymentRecordsService;
import com.example.backend.service.ProductOrdersService;
import com.example.backend.service.ProductsService;
import com.example.backend.service.UserAccountsService;
import jakarta.validation.Valid;
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
@RequestMapping("/admin/orders/product")
public class AdminProductOrderController {

    private static final int ORDER_TYPE_PRODUCT = 2;
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

    private final ProductOrdersService productOrdersService;
    private final OrderItemsService orderItemsService;
    private final UserAccountsService userAccountsService;
    private final PaymentRecordsService paymentRecordsService;
    private final ProductsService productsService;

    public AdminProductOrderController(
        ProductOrdersService productOrdersService,
        OrderItemsService orderItemsService,
        UserAccountsService userAccountsService,
        PaymentRecordsService paymentRecordsService,
        ProductsService productsService
    ) {
        this.productOrdersService = productOrdersService;
        this.orderItemsService = orderItemsService;
        this.userAccountsService = userAccountsService;
        this.paymentRecordsService = paymentRecordsService;
        this.productsService = productsService;
    }

    @GetMapping
    public Result<Page<AdminProductOrderModel.ListItemResponse>> listOrders(
        @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
        @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "orderStatus", required = false) Integer orderStatus,
        @RequestParam(value = "paymentStatus", required = false) Integer paymentStatus,
        @RequestParam(value = "deliveryStatus", required = false) Integer deliveryStatus
    ) {
        LoginUserInfo admin = requireAdmin();
        long currentPage = pageNum <= 0 ? 1 : pageNum;
        long currentSize = pageSize <= 0 ? 10 : pageSize;

        LambdaQueryWrapper<ProductOrders> wrapper = new LambdaQueryWrapper<ProductOrders>()
            .eq(ProductOrders::getIsDelete, 0);

        // 门店管理员：仅查看本门店商品产生的订单
        applyStoreFilter(admin, wrapper);
        if (orderStatus != null) {
            wrapper.eq(ProductOrders::getOrderStatus, orderStatus);
        }
        if (paymentStatus != null) {
            wrapper.eq(ProductOrders::getPaymentStatus, paymentStatus);
        }
        if (deliveryStatus != null) {
            wrapper.eq(ProductOrders::getDeliveryStatus, deliveryStatus);
        }
        applyKeywordFilter(wrapper, keyword);
        wrapper.orderByDesc(ProductOrders::getUpdatedTime)
            .orderByDesc(ProductOrders::getCreatedTime);

        Page<ProductOrders> page = productOrdersService.page(new Page<>(currentPage, currentSize), wrapper);
        Page<AdminProductOrderModel.ListItemResponse> responsePage = new Page<>(
            page.getCurrent(),
            page.getSize(),
            page.getTotal()
        );
        responsePage.setRecords(buildListItems(page.getRecords()));
        return Result.success(responsePage);
    }

    @GetMapping("/{id}")
    public Result<AdminProductOrderModel.DetailResponse> getDetail(@PathVariable("id") String id) {
        requireAdmin();
        return Result.success(buildDetailResponse(requireOrder(id)));
    }

    @PostMapping("/{id}/ship")
    @Transactional(rollbackFor = Exception.class)
    public Result<AdminProductOrderModel.DetailResponse> shipOrder(
        @PathVariable("id") String id,
        @Valid @RequestBody AdminProductOrderModel.ShipRequest request
    ) {
        requireAdmin();
        ProductOrders order = requireOrder(id);
        if (safeInt(order.getPaymentStatus()) != PAYMENT_STATUS_PAID) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前订单未支付，无法发货");
        }
        if (safeInt(order.getOrderStatus()) != ORDER_STATUS_PENDING_DELIVERY
            || safeInt(order.getDeliveryStatus()) != DELIVERY_STATUS_PENDING) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前订单状态不支持发货");
        }

        long now = System.currentTimeMillis();
        order.setOrderStatus(ORDER_STATUS_PENDING_RECEIPT);
        order.setDeliveryStatus(DELIVERY_STATUS_SHIPPED);
        order.setDeliveryCompany(trimToNull(request.getDeliveryCompany()));
        order.setDeliveryNo(trimToNull(request.getDeliveryNo()));
        order.setDeliveryTime(now);
        order.setUpdatedTime(now);
        if (!productOrdersService.updateById(order)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "发货失败");
        }
        return Result.success(buildDetailResponse(requireOrder(id)));
    }

    private void applyKeywordFilter(LambdaQueryWrapper<ProductOrders> wrapper, String keyword) {
        String normalizedKeyword = trimToNull(keyword);
        if (!StringUtils.hasText(normalizedKeyword)) {
            return;
        }

        Set<String> userIds = userAccountsService.list(
            new LambdaQueryWrapper<UserAccounts>()
                .eq(UserAccounts::getIsDelete, 0)
                .and(q -> q.like(UserAccounts::getUsername, normalizedKeyword)
                    .or().like(UserAccounts::getPhone, normalizedKeyword)
                    .or().like(UserAccounts::getEmail, normalizedKeyword))
        ).stream().map(UserAccounts::getId).filter(StringUtils::hasText).collect(Collectors.toSet());

        Set<String> orderIds = orderItemsService.list(
            new LambdaQueryWrapper<OrderItems>()
                .eq(OrderItems::getIsDelete, 0)
                .and(q -> q.like(OrderItems::getProductName, normalizedKeyword)
                    .or().like(OrderItems::getProductId, normalizedKeyword))
        ).stream().map(OrderItems::getOrderId).filter(StringUtils::hasText).collect(Collectors.toSet());

        wrapper.and(q -> {
            q.like(ProductOrders::getOrderNo, normalizedKeyword)
                .or().like(ProductOrders::getDeliveryName, normalizedKeyword)
                .or().like(ProductOrders::getDeliveryPhone, normalizedKeyword)
                .or().like(ProductOrders::getDeliveryAddress, normalizedKeyword)
                .or().like(ProductOrders::getDeliveryCompany, normalizedKeyword)
                .or().like(ProductOrders::getDeliveryNo, normalizedKeyword);
            if (!userIds.isEmpty()) {
                q.or().in(ProductOrders::getAccountId, userIds);
            }
            if (!orderIds.isEmpty()) {
                q.or().in(ProductOrders::getId, orderIds);
            }
        });
    }

    private List<AdminProductOrderModel.ListItemResponse> buildListItems(List<ProductOrders> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, UserAccounts> userMap = listUserMap(orders);
        Map<String, List<OrderItems>> orderItemMap = listOrderItemMap(orders);
        Map<String, PaymentRecords> paymentMap = listPaymentMap(orders);

        List<AdminProductOrderModel.ListItemResponse> items = new ArrayList<>();
        for (ProductOrders order : orders) {
            List<OrderItems> orderItems = orderItemMap.getOrDefault(order.getId(), Collections.emptyList());
            PaymentRecords payment = paymentMap.get(order.getId());
            UserAccounts user = userMap.get(order.getAccountId());

            AdminProductOrderModel.ListItemResponse item = new AdminProductOrderModel.ListItemResponse();
            item.setId(order.getId());
            item.setOrderNo(safe(order.getOrderNo()));
            item.setOrderStatus(order.getOrderStatus());
            item.setOrderStatusText(getOrderStatusText(order.getOrderStatus()));
            item.setPaymentStatus(order.getPaymentStatus());
            item.setPaymentStatusText(getPaymentStatusText(order.getPaymentStatus()));
            item.setDeliveryStatus(order.getDeliveryStatus());
            item.setDeliveryStatusText(getDeliveryStatusText(order.getDeliveryStatus()));
            item.setUserId(safe(order.getAccountId()));
            item.setUserName(user == null ? "" : safe(user.getUsername()));
            item.setUserPhone(user == null ? "" : safe(user.getPhone()));
            item.setDeliveryName(safe(order.getDeliveryName()));
            item.setDeliveryPhone(safe(order.getDeliveryPhone()));
            item.setDeliveryAddress(safe(order.getDeliveryAddress()));
            item.setItemCount(calculateItemCount(orderItems));
            item.setProductSummary(buildProductSummary(orderItems));
            item.setTotalAmount(formatMoney(order.getTotalAmount()));
            item.setActualAmount(formatMoney(order.getActualAmount()));
            item.setPaymentMethod(order.getPaymentMethod());
            item.setPaymentMethodText(getPaymentMethodText(order.getPaymentMethod()));
            item.setCreatedTime(order.getCreatedTime());
            item.setPaymentTime(order.getPaymentTime());
            item.setDeliveryTime(order.getDeliveryTime());
            if (payment != null && !StringUtils.hasText(item.getPaymentMethodText())) {
                item.setPaymentMethodText(getPaymentMethodText(payment.getPaymentMethod()));
            }
            items.add(item);
        }
        return items;
    }

    private AdminProductOrderModel.DetailResponse buildDetailResponse(ProductOrders order) {
        List<ProductOrders> orders = Collections.singletonList(order);
        Map<String, UserAccounts> userMap = listUserMap(orders);
        Map<String, List<OrderItems>> orderItemMap = listOrderItemMap(orders);
        Map<String, PaymentRecords> paymentMap = listPaymentMap(orders);

        List<OrderItems> orderItems = orderItemMap.getOrDefault(order.getId(), Collections.emptyList());
        PaymentRecords payment = paymentMap.get(order.getId());
        UserAccounts user = userMap.get(order.getAccountId());

        AdminProductOrderModel.DetailResponse response = new AdminProductOrderModel.DetailResponse();
        response.setId(order.getId());
        response.setOrderNo(safe(order.getOrderNo()));
        response.setOrderStatus(order.getOrderStatus());
        response.setOrderStatusText(getOrderStatusText(order.getOrderStatus()));
        response.setPaymentStatus(order.getPaymentStatus());
        response.setPaymentStatusText(getPaymentStatusText(order.getPaymentStatus()));
        response.setDeliveryStatus(order.getDeliveryStatus());
        response.setDeliveryStatusText(getDeliveryStatusText(order.getDeliveryStatus()));
        response.setUserId(safe(order.getAccountId()));
        response.setUserName(user == null ? "" : safe(user.getUsername()));
        response.setUserPhone(user == null ? "" : safe(user.getPhone()));
        response.setDeliveryName(safe(order.getDeliveryName()));
        response.setDeliveryPhone(safe(order.getDeliveryPhone()));
        response.setDeliveryAddress(safe(order.getDeliveryAddress()));
        response.setDeliveryCompany(safe(order.getDeliveryCompany()));
        response.setDeliveryNo(safe(order.getDeliveryNo()));
        response.setItemCount(calculateItemCount(orderItems));
        response.setProductSummary(buildProductSummary(orderItems));
        response.setTotalAmount(formatMoney(order.getTotalAmount()));
        response.setProductAmount(formatMoney(order.getProductAmount()));
        response.setShippingFee(formatMoney(order.getShippingFee()));
        response.setDiscountAmount(formatMoney(order.getDiscountAmount()));
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

    private Map<String, UserAccounts> listUserMap(List<ProductOrders> orders) {
        Set<String> userIds = orders.stream()
            .map(ProductOrders::getAccountId)
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

    private ProductOrders requireOrder(String id) {
        ProductOrders order = productOrdersService.getOne(
            new LambdaQueryWrapper<ProductOrders>()
                .eq(ProductOrders::getId, id)
                .eq(ProductOrders::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品订单不存在");
        }
        return order;
    }

    /**
     * 门店管理员过滤：仅查看本门店商品产生的订单
     */
    private void applyStoreFilter(LoginUserInfo admin, LambdaQueryWrapper<ProductOrders> wrapper) {
        if (admin == null || !admin.isStoreAdmin() || !StringUtils.hasText(admin.getStoreId())) {
            return;
        }
        List<Products> storeProducts = productsService.list(
            new LambdaQueryWrapper<Products>()
                .eq(Products::getStoreId, admin.getStoreId())
                .eq(Products::getIsDelete, 0)
        );
        if (storeProducts.isEmpty()) {
            wrapper.eq(ProductOrders::getId, "-1");
            return;
        }
        Set<String> productIds = storeProducts.stream().map(Products::getId).collect(Collectors.toSet());
        List<OrderItems> items = orderItemsService.list(
            new LambdaQueryWrapper<OrderItems>()
                .in(OrderItems::getProductId, productIds)
                .eq(OrderItems::getIsDelete, 0)
        );
        if (items.isEmpty()) {
            wrapper.eq(ProductOrders::getId, "-1");
            return;
        }
        Set<String> orderIds = items.stream().map(OrderItems::getOrderId).collect(Collectors.toSet());
        wrapper.in(ProductOrders::getId, orderIds);
    }

    private LoginUserInfo requireAdmin() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问商品订单管理");
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
