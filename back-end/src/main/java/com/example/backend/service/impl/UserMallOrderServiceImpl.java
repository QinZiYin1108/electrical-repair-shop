
package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.AccountBalances;
import com.example.backend.entity.Coupons;
import com.example.backend.entity.FundFlows;
import com.example.backend.entity.OrderItems;
import com.example.backend.entity.PaymentRecords;
import com.example.backend.entity.ProductCategories;
import com.example.backend.entity.ProductOrders;
import com.example.backend.entity.Products;
import com.example.backend.entity.ShoppingCarts;
import com.example.backend.entity.UserAddresses;
import com.example.backend.entity.UserCoupons;
import com.example.backend.entity.WarrantyCards;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.ShoppingCartsMapper;
import com.example.backend.model.user.UserMallOrderModel;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.AccountBalancesService;
import com.example.backend.service.CouponsService;
import com.example.backend.service.FundFlowsService;
import com.example.backend.service.OrderItemsService;
import com.example.backend.service.PaymentRecordsService;
import com.example.backend.service.ProductCategoriesService;
import com.example.backend.service.ProductOrdersService;
import com.example.backend.service.ProductsService;
import com.example.backend.service.ShoppingCartsService;
import com.example.backend.service.UserAddressesService;
import com.example.backend.service.UserCouponsService;
import com.example.backend.service.UserMallOrderService;
import com.example.backend.service.WarrantyCardsService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserMallOrderServiceImpl implements UserMallOrderService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};
    private static final int ACCOUNT_TYPE_USER = 1;
    private static final int CART_SELECTED = 1;
    private static final int CART_UNSELECTED = 0;
    private static final int ORDER_STATUS_PENDING_DELIVERY = 2;
    private static final int PAYMENT_STATUS_PAID = 2;
    private static final int DELIVERY_STATUS_PENDING = 1;
    private static final int FLOW_TYPE_EXPENSE = 2;
    private static final int PAYMENT_METHOD_WECHAT = 1;
    private static final int PAYMENT_METHOD_ALIPAY = 2;
    private static final int PAYMENT_METHOD_WALLET = 5;
    private static final int PAYMENT_RECORD_STATUS_SUCCESS = 3;
    private static final int ORDER_TYPE_PRODUCT = 2;
    private static final int USER_COUPON_STATUS_UNUSED = 1;
    private static final int USER_COUPON_STATUS_USED = 2;
    private static final int USER_COUPON_STATUS_EXPIRED = 3;
    private static final int COUPON_STATUS_ACTIVE = 1;
    private static final int COUPON_DISCOUNT_TYPE_FIXED = 1;
    private static final int COUPON_DISCOUNT_TYPE_PERCENT = 2;
    private static final int COUPON_TYPE_FREE = 3;
    private static final int COUPON_APPLICABLE_ALL = 1;
    private static final int COUPON_APPLICABLE_CATEGORY = 2;
    private static final int COUPON_APPLICABLE_PRODUCT = 3;
    private static final int WARRANTY_TYPE_STORE = 2;
    private static final int WARRANTY_STATUS_ACTIVE = 1;
    private static final String BUSINESS_TYPE_PRODUCT_ORDER_PAY = "PRODUCT_ORDER_PAY";

    private final ShoppingCartsService shoppingCartsService;
    private final ShoppingCartsMapper shoppingCartsMapper;
    private final ProductsService productsService;
    private final ProductCategoriesService productCategoriesService;
    private final UserAddressesService userAddressesService;
    private final ProductOrdersService productOrdersService;
    private final OrderItemsService orderItemsService;
    private final AccountBalancesService accountBalancesService;
    private final FundFlowsService fundFlowsService;
    private final PaymentRecordsService paymentRecordsService;
    private final CouponsService couponsService;
    private final UserCouponsService userCouponsService;
    private final WarrantyCardsService warrantyCardsService;

    public UserMallOrderServiceImpl(
        ShoppingCartsService shoppingCartsService,
        ShoppingCartsMapper shoppingCartsMapper,
        ProductsService productsService,
        ProductCategoriesService productCategoriesService,
        UserAddressesService userAddressesService,
        ProductOrdersService productOrdersService,
        OrderItemsService orderItemsService,
        AccountBalancesService accountBalancesService,
        FundFlowsService fundFlowsService,
        PaymentRecordsService paymentRecordsService,
        CouponsService couponsService,
        UserCouponsService userCouponsService,
        WarrantyCardsService warrantyCardsService
    ) {
        this.shoppingCartsService = shoppingCartsService;
        this.shoppingCartsMapper = shoppingCartsMapper;
        this.productsService = productsService;
        this.productCategoriesService = productCategoriesService;
        this.userAddressesService = userAddressesService;
        this.productOrdersService = productOrdersService;
        this.orderItemsService = orderItemsService;
        this.accountBalancesService = accountBalancesService;
        this.fundFlowsService = fundFlowsService;
        this.paymentRecordsService = paymentRecordsService;
        this.couponsService = couponsService;
        this.userCouponsService = userCouponsService;
        this.warrantyCardsService = warrantyCardsService;
    }

    @Override
    public UserMallOrderModel.CartListResponse listCurrentUserCart() {
        LoginUserInfo user = requireCurrentUser();
        return buildCartListResponse(user.getAccountId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserMallOrderModel.CartListResponse addCurrentUserCart(UserMallOrderModel.AddCartRequest request) {
        LoginUserInfo user = requireCurrentUser();
        String productId = trimToNull(request == null ? null : request.getProductId());
        int quantity = normalizeQuantity(request == null ? null : request.getQuantity());
        Products product = requireProduct(productId);
        validateStock(product, quantity);

        ShoppingCarts existing = shoppingCartsMapper.selectAnyByAccountIdAndProductId(user.getAccountId(), productId);
        long now = System.currentTimeMillis();
        if (existing == null) {
            ShoppingCarts cart = new ShoppingCarts();
            cart.setId(SnowflakeIdUtil.nextShoppingCartId());
            cart.setAccountId(user.getAccountId());
            cart.setProductId(productId);
            cart.setQuantity(quantity);
            cart.setSelected(CART_SELECTED);
            cart.setCreatedTime(now);
            cart.setUpdatedTime(now);
            cart.setVersion(0);
            cart.setIsDelete(0);
            if (!shoppingCartsService.save(cart)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "加入购物车失败");
            }
        } else {
            int nextQuantity = Math.min(99, defaultIfNull(existing.getQuantity(), 0) + quantity);
            validateStock(product, nextQuantity);
            if (Objects.equals(existing.getIsDelete(), 0)) {
                ShoppingCarts update = new ShoppingCarts();
                update.setId(existing.getId());
                update.setQuantity(nextQuantity);
                update.setSelected(CART_SELECTED);
                update.setUpdatedTime(now);
                shoppingCartsService.updateById(update);
            } else {
                shoppingCartsMapper.restoreById(existing.getId(), nextQuantity, CART_SELECTED, now);
            }
        }
        return buildCartListResponse(user.getAccountId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserMallOrderModel.CartListResponse updateCurrentUserCartQuantity(UserMallOrderModel.UpdateCartQuantityRequest request) {
        LoginUserInfo user = requireCurrentUser();
        String cartId = trimToNull(request == null ? null : request.getCartId());
        int quantity = normalizeQuantity(request == null ? null : request.getQuantity());
        ShoppingCarts cart = requireCartItem(user.getAccountId(), cartId);
        Products product = requireProduct(cart.getProductId());
        validateStock(product, quantity);

        ShoppingCarts update = new ShoppingCarts();
        update.setId(cart.getId());
        update.setQuantity(quantity);
        update.setUpdatedTime(System.currentTimeMillis());
        shoppingCartsService.updateById(update);
        return buildCartListResponse(user.getAccountId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserMallOrderModel.CartListResponse toggleCurrentUserCartSelected(UserMallOrderModel.ToggleCartSelectedRequest request) {
        LoginUserInfo user = requireCurrentUser();
        String cartId = trimToNull(request == null ? null : request.getCartId());
        boolean selected = !Boolean.FALSE.equals(request == null ? null : request.getSelected());
        ShoppingCarts cart = requireCartItem(user.getAccountId(), cartId);

        ShoppingCarts update = new ShoppingCarts();
        update.setId(cart.getId());
        update.setSelected(selected ? CART_SELECTED : CART_UNSELECTED);
        update.setUpdatedTime(System.currentTimeMillis());
        shoppingCartsService.updateById(update);
        return buildCartListResponse(user.getAccountId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserMallOrderModel.CartListResponse toggleCurrentUserCartSelectedAll(UserMallOrderModel.ToggleAllCartSelectedRequest request) {
        LoginUserInfo user = requireCurrentUser();
        boolean selected = !Boolean.FALSE.equals(request == null ? null : request.getSelected());
        shoppingCartsService.update(
            new LambdaUpdateWrapper<ShoppingCarts>()
                .eq(ShoppingCarts::getAccountId, user.getAccountId())
                .eq(ShoppingCarts::getIsDelete, 0)
                .set(ShoppingCarts::getSelected, selected ? CART_SELECTED : CART_UNSELECTED)
                .set(ShoppingCarts::getUpdatedTime, System.currentTimeMillis())
        );
        return buildCartListResponse(user.getAccountId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserMallOrderModel.CartListResponse removeCurrentUserCartItems(UserMallOrderModel.RemoveCartItemsRequest request) {
        LoginUserInfo user = requireCurrentUser();
        List<String> cartIds = normalizeIdList(request == null ? null : request.getCartIds());
        if (cartIds.isEmpty()) {
            return buildCartListResponse(user.getAccountId());
        }
        shoppingCartsService.update(
            new LambdaUpdateWrapper<ShoppingCarts>()
                .eq(ShoppingCarts::getAccountId, user.getAccountId())
                .in(ShoppingCarts::getId, cartIds)
                .eq(ShoppingCarts::getIsDelete, 0)
                .set(ShoppingCarts::getIsDelete, 1)
                .set(ShoppingCarts::getUpdatedTime, System.currentTimeMillis())
        );
        return buildCartListResponse(user.getAccountId());
    }
    @Override
    public UserMallOrderModel.AvailableCouponListResponse listCurrentUserAvailableCoupons(UserMallOrderModel.AvailableCouponRequest request) {
        LoginUserInfo user = requireCurrentUser();
        CheckoutContext checkoutContext = buildCheckoutContext(
            resolveSubmitSources(
                user.getAccountId(),
                request == null ? Collections.emptyList() : request.getCartIds(),
                request == null ? Collections.emptyList() : request.getItems()
            )
        );
        return buildAvailableCouponResponse(user.getAccountId(), checkoutContext);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserMallOrderModel.SubmitOrderResponse submitCurrentUserProductOrder(UserMallOrderModel.SubmitOrderRequest request) {
        LoginUserInfo user = requireCurrentUser();
        String addressId = trimToNull(request == null ? null : request.getAddressId());
        UserAddresses address = requireAddress(user.getAccountId(), addressId);
        int paymentMethod = normalizePaymentMethod(request == null ? null : request.getPaymentMethod());
        String remark = trimToNull(request == null ? null : request.getRemark());

        List<CartSubmitSource> sources = resolveSubmitSources(
            user.getAccountId(),
            request == null ? Collections.emptyList() : request.getCartIds(),
            request == null ? Collections.emptyList() : request.getItems()
        );
        CheckoutContext checkoutContext = buildCheckoutContext(sources);
        if (checkoutContext.sources.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "结算商品不能为空");
        }

        long now = System.currentTimeMillis();
        String orderId = SnowflakeIdUtil.nextProductOrderId();
        String orderNo = buildProductOrderNo(now);
        CouponUsageResult couponUsage = validateSelectedCoupon(
            user.getAccountId(),
            request == null ? null : request.getUserCouponId(),
            checkoutContext,
            orderId,
            now
        );

        BigDecimal discountAmount = couponUsage.discountAmount;
        BigDecimal totalAmount = checkoutContext.totalAmount;
        BigDecimal actualAmount = totalAmount.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
        int itemCount = 0;
        List<OrderItems> orderItems = new ArrayList<>();
        List<String> cartIdsToDelete = new ArrayList<>();

        for (CartSubmitSource source : checkoutContext.sources) {
            Products product = requireSubmitProduct(checkoutContext.productMap.get(source.getProductId()));
            validateStock(product, source.getQuantity());

            BigDecimal price = defaultAmount(product.getSellingPrice()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineAmount = price.multiply(BigDecimal.valueOf(source.getQuantity())).setScale(2, RoundingMode.HALF_UP);
            itemCount += source.getQuantity();

            OrderItems orderItem = new OrderItems();
            orderItem.setId(SnowflakeIdUtil.nextOrderItemId());
            orderItem.setOrderId(orderId);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImageUrl());
            orderItem.setProductPrice(price);
            orderItem.setQuantity(source.getQuantity());
            orderItem.setTotalPrice(lineAmount);
            orderItem.setCreatedTime(now);
            orderItem.setVersion(0);
            orderItem.setIsDelete(0);
            orderItems.add(orderItem);

            Products updateProduct = new Products();
            updateProduct.setId(product.getId());
            updateProduct.setStockQuantity(defaultIfNull(product.getStockQuantity(), 0) - source.getQuantity());
            updateProduct.setSalesCount(defaultIfNull(product.getSalesCount(), 0) + source.getQuantity());
            updateProduct.setUpdatedTime(now);
            productsService.updateById(updateProduct);

            if (StringUtils.hasText(source.getCartId())) {
                cartIdsToDelete.add(source.getCartId());
            }
        }

        ProductOrders order = new ProductOrders();
        order.setId(orderId);
        order.setOrderNo(orderNo);
        order.setAccountId(user.getAccountId());
        order.setOrderStatus(ORDER_STATUS_PENDING_DELIVERY);
        order.setPaymentStatus(PAYMENT_STATUS_PAID);
        order.setDeliveryStatus(DELIVERY_STATUS_PENDING);
        order.setTotalAmount(totalAmount);
        order.setProductAmount(checkoutContext.productAmount);
        order.setShippingFee(checkoutContext.shippingFee);
        order.setDiscountAmount(discountAmount);
        order.setActualAmount(actualAmount);
        order.setCouponId(couponUsage.couponId);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentTime(now);
        order.setDeliveryAddressId(address.getId());
        order.setDeliveryName(address.getContactName());
        order.setDeliveryPhone(address.getContactPhone());
        order.setDeliveryAddress(buildFullAddress(address));
        order.setRemark(defaultText(remark, ""));
        order.setCreatedTime(now);
        order.setUpdatedTime(now);
        order.setVersion(0);
        order.setIsDelete(0);
        if (!productOrdersService.save(order)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存订单失败");
        }
        if (!orderItemsService.saveBatch(orderItems)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存订单明细失败");
        }

        markCouponUsed(couponUsage, now);
        processProductOrderPayment(user.getAccountId(), order, paymentMethod, now);
        createWarrantyCards(user.getAccountId(), checkoutContext, now);

        if (!cartIdsToDelete.isEmpty()) {
            shoppingCartsService.update(
                new LambdaUpdateWrapper<ShoppingCarts>()
                    .eq(ShoppingCarts::getAccountId, user.getAccountId())
                    .in(ShoppingCarts::getId, cartIdsToDelete)
                    .eq(ShoppingCarts::getIsDelete, 0)
                    .set(ShoppingCarts::getIsDelete, 1)
                    .set(ShoppingCarts::getUpdatedTime, now)
            );
        }

        UserMallOrderModel.SubmitOrderResponse response = new UserMallOrderModel.SubmitOrderResponse();
        response.setOrderId(orderId);
        response.setOrderNo(orderNo);
        response.setItemCount(itemCount);
        response.setActualAmount(order.getActualAmount());
        response.setDiscountAmount(order.getDiscountAmount());
        response.setUserCouponId(couponUsage.userCouponId);
        response.setCouponName(couponUsage.couponName);
        return response;
    }

    private void createWarrantyCards(String userId, CheckoutContext checkoutContext, long now) {
        if (!StringUtils.hasText(userId) || checkoutContext == null || checkoutContext.sources.isEmpty()) {
            return;
        }
        LocalDate baseDate = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate();
        Date purchaseDate = Date.from(baseDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        List<WarrantyCards> cards = new ArrayList<>();
        for (CartSubmitSource source : checkoutContext.sources) {
            Products product = checkoutContext.productMap.get(source.getProductId());
            int warrantyPeriod = defaultIfNull(product == null ? null : product.getWarrantyPeriod(), 0);
            if (product == null || warrantyPeriod <= 0) {
                continue;
            }
            Date warrantyStartDate = purchaseDate;
            Date warrantyEndDate = Date.from(baseDate.plusMonths(warrantyPeriod).atStartOfDay(ZoneId.systemDefault()).toInstant());
            for (int index = 0; index < source.getQuantity(); index++) {
                WarrantyCards card = new WarrantyCards();
                String cardId = SnowflakeIdUtil.nextWarrantyCardId();
                card.setId(cardId);
                card.setCardNo(buildWarrantyCardNo(cardId));
                card.setUserId(userId);
                card.setProductId(product.getId());
                card.setProductName(defaultText(product.getName(), "商品"));
                card.setProductModel(defaultText(product.getModel(), "-"));
                card.setPurchaseDate(purchaseDate);
                card.setWarrantyStartDate(warrantyStartDate);
                card.setWarrantyEndDate(warrantyEndDate);
                card.setWarrantyPeriod(warrantyPeriod);
                card.setWarrantyType(WARRANTY_TYPE_STORE);
                card.setWarrantyStatus(WARRANTY_STATUS_ACTIVE);
                card.setRepairCount(0);
                card.setCreatedTime(now);
                card.setUpdatedTime(now);
                card.setVersion(0);
                card.setIsDelete(0);
                cards.add(card);
            }
        }
        if (!cards.isEmpty() && !warrantyCardsService.saveBatch(cards)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成保修卡失败");
        }
    }

    private CouponUsageResult validateSelectedCoupon(
        String accountId,
        String userCouponId,
        CheckoutContext checkoutContext,
        String orderId,
        long now
    ) {
        CouponUsageResult empty = CouponUsageResult.empty();
        String normalizedUserCouponId = trimToNull(userCouponId);
        if (!StringUtils.hasText(normalizedUserCouponId)) {
            return empty;
        }
        refreshExpiredUserCoupons(accountId, now);
        UserCoupons userCoupon = userCouponsService.getOne(
            new LambdaQueryWrapper<UserCoupons>()
                .eq(UserCoupons::getId, normalizedUserCouponId)
                .eq(UserCoupons::getUserId, accountId)
                .eq(UserCoupons::getStatus, USER_COUPON_STATUS_UNUSED)
                .last("limit 1"),
            false
        );
        if (userCoupon == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "优惠券不存在或不可用");
        }
        Coupons coupon = couponsService.getOne(
            new LambdaQueryWrapper<Coupons>()
                .eq(Coupons::getId, userCoupon.getCouponId())
                .last("limit 1"),
            false
        );
        CouponPreview preview = buildCouponPreview(userCoupon, coupon, checkoutContext, now);
        if (!preview.available) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, defaultText(preview.reason, "当前优惠券不可用"));
        }
        CouponUsageResult result = new CouponUsageResult();
        result.userCouponId = userCoupon.getId();
        result.couponId = userCoupon.getCouponId();
        result.couponName = coupon == null ? "" : defaultText(coupon.getName(), "优惠券");
        result.discountAmount = normalizeMoney(preview.discountAmount);
        result.orderId = orderId;
        result.userCoupon = userCoupon;
        result.coupon = coupon;
        return result;
    }

    private void markCouponUsed(CouponUsageResult couponUsage, long now) {
        if (couponUsage == null || !couponUsage.hasCoupon()) {
            return;
        }
        UserCoupons userCoupon = couponUsage.userCoupon;
        userCoupon.setStatus(USER_COUPON_STATUS_USED);
        userCoupon.setUseTime(now);
        userCoupon.setOrderId(couponUsage.orderId);
        userCoupon.setUpdatedTime(now);
        if (!userCouponsService.updateById(userCoupon)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户优惠券失败");
        }
        Coupons coupon = couponUsage.coupon;
        coupon.setUsedCount(defaultIfNull(coupon.getUsedCount(), 0) + 1);
        coupon.setUpdatedTime(now);
        if (!couponsService.updateById(coupon)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新优惠券失败");
        }
    }
    private void refreshExpiredUserCoupons(String accountId, long now) {
        if (!StringUtils.hasText(accountId)) {
            return;
        }
        userCouponsService.update(
            new LambdaUpdateWrapper<UserCoupons>()
                .eq(UserCoupons::getUserId, accountId)
                .eq(UserCoupons::getStatus, USER_COUPON_STATUS_UNUSED)
                .lt(UserCoupons::getExpireTime, now)
                .set(UserCoupons::getStatus, USER_COUPON_STATUS_EXPIRED)
                .set(UserCoupons::getUpdatedTime, now)
        );
    }

    private UserMallOrderModel.AvailableCouponListResponse buildAvailableCouponResponse(String accountId, CheckoutContext checkoutContext) {
        UserMallOrderModel.AvailableCouponListResponse response = new UserMallOrderModel.AvailableCouponListResponse();
        response.setBestDiscountAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        if (checkoutContext == null || checkoutContext.sources.isEmpty()) {
            return response;
        }
        long now = System.currentTimeMillis();
        refreshExpiredUserCoupons(accountId, now);
        List<UserCoupons> userCouponList = userCouponsService.list(
            new LambdaQueryWrapper<UserCoupons>()
                .eq(UserCoupons::getUserId, accountId)
                .eq(UserCoupons::getStatus, USER_COUPON_STATUS_UNUSED)
                .orderByAsc(UserCoupons::getExpireTime)
                .orderByDesc(UserCoupons::getCreatedTime)
        );
        if (userCouponList.isEmpty()) {
            return response;
        }
        Set<String> couponIds = userCouponList.stream()
            .map(UserCoupons::getCouponId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, Coupons> couponMap = couponsService.list(
            new LambdaQueryWrapper<Coupons>()
                .in(!couponIds.isEmpty(), Coupons::getId, couponIds)
        ).stream().collect(Collectors.toMap(Coupons::getId, item -> item, (a, b) -> a, LinkedHashMap::new));

        List<CouponPreview> previews = new ArrayList<>();
        for (UserCoupons userCoupon : userCouponList) {
            previews.add(buildCouponPreview(userCoupon, couponMap.get(userCoupon.getCouponId()), checkoutContext, now));
        }
        previews.sort(Comparator
            .comparing((CouponPreview item) -> !item.available)
            .thenComparing(CouponPreview::getDiscountAmount, Comparator.reverseOrder())
            .thenComparing(CouponPreview::getExpireTime, Comparator.nullsLast(Long::compareTo))
        );

        BigDecimal bestDiscount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        String bestCouponId = "";
        for (CouponPreview preview : previews) {
            UserMallOrderModel.AvailableCouponItem item = new UserMallOrderModel.AvailableCouponItem();
            item.setUserCouponId(preview.userCouponId);
            item.setCouponId(preview.couponId);
            item.setName(preview.name);
            item.setType(preview.type);
            item.setTypeText(preview.typeText);
            item.setDiscountText(preview.discountText);
            item.setMinAmount(preview.minAmount);
            item.setDiscountAmount(preview.discountAmount);
            item.setAvailable(preview.available);
            item.setUnavailableReason(preview.reason);
            item.setExpireTime(preview.expireTime);
            item.setApplicableText(preview.applicableText);
            response.getCoupons().add(item);
            if (preview.available && preview.discountAmount.compareTo(bestDiscount) > 0) {
                bestDiscount = preview.discountAmount;
                bestCouponId = preview.userCouponId;
            }
        }
        response.setBestCouponId(bestCouponId);
        response.setBestDiscountAmount(bestDiscount);
        return response;
    }

    private CouponPreview buildCouponPreview(UserCoupons userCoupon, Coupons coupon, CheckoutContext checkoutContext, long now) {
        CouponPreview preview = new CouponPreview();
        preview.userCouponId = userCoupon == null ? "" : defaultText(userCoupon.getId(), "");
        preview.couponId = userCoupon == null ? "" : defaultText(userCoupon.getCouponId(), "");
        preview.expireTime = userCoupon == null ? null : userCoupon.getExpireTime();
        preview.discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (coupon == null) {
            preview.available = false;
            preview.name = "优惠券";
            preview.reason = "优惠券信息不存在或已失效";
            preview.typeText = "优惠券";
            preview.discountText = "优惠金额待定";
            preview.applicableText = "使用范围以券面说明为准";
            return preview;
        }

        preview.name = defaultText(coupon.getName(), "优惠券");
        preview.type = coupon.getType();
        preview.typeText = getCouponTypeText(coupon.getType());
        preview.discountText = buildCouponDiscountText(coupon);
        preview.minAmount = normalizeMoney(coupon.getMinAmount());
        preview.applicableText = buildCouponApplicableText(coupon);

        if (defaultIfNull(userCoupon.getStatus(), USER_COUPON_STATUS_UNUSED) != USER_COUPON_STATUS_UNUSED) {
            preview.available = false;
            preview.reason = "该优惠券当前不可用";
            return preview;
        }
        if (userCoupon.getExpireTime() != null && userCoupon.getExpireTime() < now) {
            preview.available = false;
            preview.reason = "该优惠券已过期";
            return preview;
        }
        if (!Objects.equals(coupon.getStatus(), COUPON_STATUS_ACTIVE)) {
            preview.available = false;
            preview.reason = "该优惠券未启用";
            return preview;
        }
        if (coupon.getEndTime() != null && coupon.getEndTime() < now) {
            preview.available = false;
            preview.reason = "该优惠券已过期";
            return preview;
        }
        if (coupon.getStartTime() != null && coupon.getStartTime() > now) {
            preview.available = false;
            preview.reason = "该优惠券尚未生效";
            return preview;
        }
        int applicableType = defaultIfNull(coupon.getApplicableType(), COUPON_APPLICABLE_ALL);
        if (applicableType != COUPON_APPLICABLE_ALL
            && applicableType != COUPON_APPLICABLE_CATEGORY
            && applicableType != COUPON_APPLICABLE_PRODUCT) {
            preview.available = false;
            preview.reason = "优惠券适用范围配置错误";
            return preview;
        }
        if (checkoutContext.totalAmount.compareTo(normalizeMoney(coupon.getMinAmount())) < 0) {
            preview.available = false;
            preview.reason = "订单金额满 " + normalizeMoney(coupon.getMinAmount()).toPlainString() + " 元可用";
            return preview;
        }
        List<String> applicableIds = parseApplicableIds(coupon.getApplicableIds());
        if (applicableType == COUPON_APPLICABLE_PRODUCT) {
            if (!applicableIds.isEmpty() && Collections.disjoint(applicableIds, checkoutContext.productIds)) {
                preview.available = false;
                preview.reason = "当前商品不支持使用该优惠券";
                return preview;
            }
        } else if (applicableType == COUPON_APPLICABLE_CATEGORY) {
            if (!applicableIds.isEmpty() && Collections.disjoint(applicableIds, checkoutContext.categoryIds)) {
                preview.available = false;
                preview.reason = "当前分类不支持使用该优惠券";
                return preview;
            }
        }
        BigDecimal discountAmount = calculateCouponDiscount(coupon, checkoutContext.totalAmount);
        if (discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            preview.available = false;
            preview.reason = "当前订单暂不满足使用条件";
            return preview;
        }
        preview.available = true;
        preview.discountAmount = discountAmount;
        preview.reason = "";
        return preview;
    }

    private BigDecimal calculateCouponDiscount(Coupons coupon, BigDecimal totalAmount) {
        BigDecimal baseAmount = normalizeMoney(totalAmount);
        if (baseAmount.compareTo(BigDecimal.ZERO) <= 0 || coupon == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal discountAmount;
        if (Objects.equals(coupon.getType(), COUPON_TYPE_FREE)) {
            discountAmount = baseAmount;
        } else if (Objects.equals(coupon.getDiscountType(), COUPON_DISCOUNT_TYPE_PERCENT)) {
            BigDecimal ratio = normalizeDiscountRatio(coupon.getDiscountValue());
            discountAmount = baseAmount.multiply(BigDecimal.ONE.subtract(ratio));
        } else {
            discountAmount = normalizeMoney(coupon.getDiscountValue());
        }
        BigDecimal maxDiscount = coupon.getMaxDiscount() == null ? null : normalizeMoney(coupon.getMaxDiscount());
        if (maxDiscount != null && maxDiscount.compareTo(BigDecimal.ZERO) > 0 && discountAmount.compareTo(maxDiscount) > 0) {
            discountAmount = maxDiscount;
        }
        if (discountAmount.compareTo(baseAmount) > 0) {
            discountAmount = baseAmount;
        }
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            discountAmount = BigDecimal.ZERO;
        }
        return discountAmount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeDiscountRatio(BigDecimal value) {
        BigDecimal source = value == null ? BigDecimal.TEN : value;
        if (source.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }
        if (source.compareTo(BigDecimal.ONE) <= 0) {
            return source;
        }
        if (source.compareTo(BigDecimal.TEN) <= 0) {
            return source.divide(BigDecimal.TEN, 4, RoundingMode.HALF_UP);
        }
        return source.divide(BigDecimal.valueOf(100L), 4, RoundingMode.HALF_UP);
    }

    private String buildCouponDiscountText(Coupons coupon) {
        if (coupon == null) {
            return "优惠券";
        }
        if (Objects.equals(coupon.getType(), COUPON_TYPE_FREE)) {
            return "免单";
        }
        if (Objects.equals(coupon.getDiscountType(), COUPON_DISCOUNT_TYPE_PERCENT)) {
            BigDecimal value = coupon.getDiscountValue() == null ? BigDecimal.TEN : coupon.getDiscountValue();
            return value.stripTrailingZeros().toPlainString() + " 折";
        }
        return "减 " + normalizeMoney(coupon.getDiscountValue()).toPlainString() + " 元";
    }

    private String buildCouponApplicableText(Coupons coupon) {
        List<String> applicableIds = parseApplicableIds(coupon == null ? null : coupon.getApplicableIds());
        if (coupon == null) {
            return "使用范围以券面说明为准";
        }
        int applicableType = defaultIfNull(coupon.getApplicableType(), COUPON_APPLICABLE_ALL);
        if (applicableType == COUPON_APPLICABLE_ALL) {
            return "全场通用";
        }
        if (applicableType == COUPON_APPLICABLE_CATEGORY) {
            return applicableIds.isEmpty() ? "分类商品可用" : "指定分类可用";
        }
        if (applicableType == COUPON_APPLICABLE_PRODUCT) {
            return applicableIds.isEmpty() ? "商品可用" : "指定商品可用";
        }
        return "使用范围以券面说明为准";
    }

    private String getCouponTypeText(Integer type) {
        if (Objects.equals(type, 1)) {
            return "满减券";
        }
        if (Objects.equals(type, 2)) {
            return "折扣券";
        }
        if (Objects.equals(type, 3)) {
            return "免单券";
        }
        return "优惠券";
    }

    private List<String> parseApplicableIds(String rawValue) {
        String normalized = trimToNull(rawValue);
        if (!StringUtils.hasText(normalized)) {
            return new ArrayList<>();
        }
        try {
            List<String> list = OBJECT_MAPPER.readValue(normalized, STRING_LIST_TYPE);
            return normalizeIdList(list);
        } catch (Exception ignored) {
            return normalizeIdList(List.of(normalized.split(",")));
        }
    }
    private CheckoutContext buildCheckoutContext(List<CartSubmitSource> sources) {
        CheckoutContext context = new CheckoutContext();
        context.sources = sources == null ? new ArrayList<>() : new ArrayList<>(sources);
        context.productAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        context.shippingFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        context.totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (context.sources.isEmpty()) {
            return context;
        }
        Set<String> productIds = context.sources.stream()
            .map(CartSubmitSource::getProductId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        context.productIds = productIds;
        context.productMap = productsService.list(
            new LambdaQueryWrapper<Products>()
                .in(!productIds.isEmpty(), Products::getId, productIds)
                .eq(Products::getIsDelete, 0)
        ).stream().collect(Collectors.toMap(Products::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
        context.categoryIds = collectCheckoutCategoryIds(context.productMap.values());
        for (CartSubmitSource source : context.sources) {
            Products product = requireSubmitProduct(context.productMap.get(source.getProductId()));
            validateStock(product, source.getQuantity());
            BigDecimal lineAmount = normalizeMoney(product.getSellingPrice())
                .multiply(BigDecimal.valueOf(source.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineShipping = Objects.equals(product.getIsFreeShipping(), 1)
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : normalizeMoney(product.getShippingFee());
            context.productAmount = context.productAmount.add(lineAmount).setScale(2, RoundingMode.HALF_UP);
            context.shippingFee = context.shippingFee.add(lineShipping).setScale(2, RoundingMode.HALF_UP);
        }
        context.totalAmount = context.productAmount.add(context.shippingFee).setScale(2, RoundingMode.HALF_UP);
        return context;
    }

    private Set<String> collectCheckoutCategoryIds(Collection<Products> products) {
        Set<String> categoryIds = (products == null ? Collections.<Products>emptyList() : products).stream()
            .map(Products::getCategoryId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (categoryIds.isEmpty()) {
            return new LinkedHashSet<>();
        }

        Set<String> result = new LinkedHashSet<>(categoryIds);
        Map<String, ProductCategories> loadedMap = new LinkedHashMap<>();
        Set<String> pending = new LinkedHashSet<>(categoryIds);
        while (!pending.isEmpty()) {
            List<ProductCategories> categories = productCategoriesService.list(
                new LambdaQueryWrapper<ProductCategories>()
                    .in(ProductCategories::getId, pending)
                    .eq(ProductCategories::getIsDelete, 0)
            );
            if (categories.isEmpty()) {
                break;
            }
            Set<String> nextPending = new LinkedHashSet<>();
            for (ProductCategories category : categories) {
                if (category == null || !StringUtils.hasText(category.getId())) {
                    continue;
                }
                loadedMap.put(category.getId(), category);
                result.add(category.getId());
                String parentId = trimToNull(category.getParentId());
                if (StringUtils.hasText(parentId) && !loadedMap.containsKey(parentId)) {
                    nextPending.add(parentId);
                }
            }
            pending = nextPending;
        }
        return result;
    }

    private void processProductOrderPayment(String accountId, ProductOrders order, int paymentMethod, long now) {
        if (!StringUtils.hasText(accountId) || order == null) {
            return;
        }
        BigDecimal amount = normalizeMoney(order.getActualAmount());
        BigDecimal balanceBefore = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal balanceAfter = balanceBefore;

        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            AccountBalances balance = ensureUserBalance(accountId, now);
            balanceBefore = normalizeMoney(balance.getBalance());
            balanceAfter = balanceBefore;

            if (paymentMethod == PAYMENT_METHOD_WALLET) {
                if (balanceBefore.compareTo(amount) < 0) {
                    throw new BusinessException(ErrorCode.BUSINESS_ERROR, "钱包余额不足");
                }
                balanceAfter = balanceBefore.subtract(amount).setScale(2, RoundingMode.HALF_UP);
                balance.setBalance(balanceAfter);
            }

            balance.setTotalExpense(defaultAmount(balance.getTotalExpense()).add(amount).setScale(2, RoundingMode.HALF_UP));
            balance.setUpdatedTime(now);
            if (!accountBalancesService.updateById(balance)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户余额失败");
            }

            FundFlows flow = new FundFlows();
            flow.setId(SnowflakeIdUtil.nextFundFlowId());
            flow.setAccountId(accountId);
            flow.setAccountType(ACCOUNT_TYPE_USER);
            flow.setFlowType(FLOW_TYPE_EXPENSE);
            flow.setAmount(amount);
            flow.setBalanceBefore(balanceBefore);
            flow.setBalanceAfter(balanceAfter);
            flow.setBusinessType(BUSINESS_TYPE_PRODUCT_ORDER_PAY);
            flow.setBusinessId(order.getId());
            flow.setDescription(buildProductOrderPaymentDescription(paymentMethod));
            flow.setRemark("orderNo=" + safeText(order.getOrderNo()));
            flow.setCreatedTime(now);
            flow.setVersion(0);
            flow.setIsDelete(0);
            if (!fundFlowsService.save(flow)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存资金流水失败");
            }
        }

        PaymentRecords record = new PaymentRecords();
        record.setId(SnowflakeIdUtil.nextPaymentRecordId());
        record.setPaymentNo(buildProductPaymentNo(record.getId()));
        record.setOrderId(order.getId());
        record.setOrderType(ORDER_TYPE_PRODUCT);
        record.setAccountId(accountId);
        record.setPaymentMethod(paymentMethod);
        record.setPaymentAmount(amount);
        record.setPaymentStatus(PAYMENT_RECORD_STATUS_SUCCESS);
        record.setThirdPartyNo(buildThirdPartyNo(record.getPaymentNo(), paymentMethod));
        record.setPaymentTime(now);
        record.setRefundAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        record.setRemark(buildProductPaymentRemark(paymentMethod));
        record.setCreatedTime(now);
        record.setUpdatedTime(now);
        record.setVersion(0);
        record.setIsDelete(0);
        if (!paymentRecordsService.save(record)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存支付记录失败");
        }
    }

    private UserMallOrderModel.CartListResponse buildCartListResponse(String accountId) {
        List<ShoppingCarts> carts = shoppingCartsService.list(
            new LambdaQueryWrapper<ShoppingCarts>()
                .eq(ShoppingCarts::getAccountId, accountId)
                .eq(ShoppingCarts::getIsDelete, 0)
                .orderByDesc(ShoppingCarts::getUpdatedTime)
                .orderByDesc(ShoppingCarts::getCreatedTime)
        );
        UserMallOrderModel.CartListResponse response = new UserMallOrderModel.CartListResponse();
        if (carts.isEmpty()) {
            response.setItems(new ArrayList<>());
            response.setTotalCount(0);
            response.setSelectedCount(0);
            response.setSelectedAmount(BigDecimal.ZERO);
            return response;
        }

        Set<String> productIds = carts.stream()
            .map(ShoppingCarts::getProductId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, Products> productMap = productsService.list(
            new LambdaQueryWrapper<Products>()
                .in(Products::getId, productIds)
                .eq(Products::getStatus, 1)
                .eq(Products::getIsDelete, 0)
        ).stream().collect(Collectors.toMap(Products::getId, item -> item, (a, b) -> a, LinkedHashMap::new));

        List<UserMallOrderModel.CartItem> items = new ArrayList<>();
        int selectedCount = 0;
        BigDecimal selectedAmount = BigDecimal.ZERO;
        for (ShoppingCarts cart : carts) {
            Products product = productMap.get(cart.getProductId());
            if (product == null) {
                continue;
            }
            UserMallOrderModel.CartItem item = new UserMallOrderModel.CartItem();
            item.setCartId(cart.getId());
            item.setProductId(product.getId());
            item.setName(product.getName());
            item.setMainImageUrl(product.getMainImageUrl());
            item.setCategoryPath("");
            item.setBrand(defaultText(product.getBrand(), ""));
            item.setModel(defaultText(product.getModel(), ""));
            item.setSellingPrice(defaultAmount(product.getSellingPrice()));
            item.setOriginalPrice(defaultAmount(product.getOriginalPrice()));
            item.setQuantity(defaultIfNull(cart.getQuantity(), 1));
            item.setSelected(defaultIfNull(cart.getSelected(), CART_SELECTED));
            item.setStockQuantity(defaultIfNull(product.getStockQuantity(), 0));
            item.setLineAmount(item.getSellingPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            items.add(item);
            if (Objects.equals(item.getSelected(), CART_SELECTED)) {
                selectedCount += item.getQuantity();
                selectedAmount = selectedAmount.add(item.getLineAmount());
            }
        }
        response.setItems(items);
        response.setTotalCount(items.size());
        response.setSelectedCount(selectedCount);
        response.setSelectedAmount(selectedAmount);
        return response;
    }

    private List<CartSubmitSource> resolveSubmitSources(
        String accountId,
        Collection<String> cartIdValues,
        List<UserMallOrderModel.SubmitOrderItem> submitItems
    ) {
        List<String> cartIds = normalizeIdList(cartIdValues);
        if (!cartIds.isEmpty()) {
            return shoppingCartsService.list(
                new LambdaQueryWrapper<ShoppingCarts>()
                    .eq(ShoppingCarts::getAccountId, accountId)
                    .in(ShoppingCarts::getId, cartIds)
                    .eq(ShoppingCarts::getIsDelete, 0)
            ).stream()
                .map(item -> new CartSubmitSource(item.getId(), item.getProductId(), defaultIfNull(item.getQuantity(), 1)))
                .collect(Collectors.toCollection(ArrayList::new));
        }
        return (submitItems == null ? Collections.<UserMallOrderModel.SubmitOrderItem>emptyList() : submitItems).stream()
            .filter(Objects::nonNull)
            .map(item -> new CartSubmitSource("", trimToNull(item.getProductId()), normalizeQuantity(item.getQuantity())))
            .filter(item -> StringUtils.hasText(item.getProductId()))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private Products requireProduct(String productId) {
        if (!StringUtils.hasText(productId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品ID不能为空");
        }
        Products product = productsService.getOne(
            new LambdaQueryWrapper<Products>()
                .eq(Products::getId, productId)
                .eq(Products::getStatus, 1)
                .eq(Products::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在或已下架");
        }
        return product;
    }
    private Products requireSubmitProduct(Products product) {
        if (product == null || !Objects.equals(product.getStatus(), 1) || !Objects.equals(product.getIsDelete(), 0)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在或已下架");
        }
        return product;
    }

    private ShoppingCarts requireCartItem(String accountId, String cartId) {
        if (!StringUtils.hasText(cartId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "购物车项ID不能为空");
        }
        ShoppingCarts cart = shoppingCartsService.getOne(
            new LambdaQueryWrapper<ShoppingCarts>()
                .eq(ShoppingCarts::getId, cartId)
                .eq(ShoppingCarts::getAccountId, accountId)
                .eq(ShoppingCarts::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (cart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "购物车项不存在");
        }
        return cart;
    }

    private UserAddresses requireAddress(String accountId, String addressId) {
        if (!StringUtils.hasText(addressId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "收货地址不能为空");
        }
        UserAddresses address = userAddressesService.getOne(
            new LambdaQueryWrapper<UserAddresses>()
                .eq(UserAddresses::getId, addressId)
                .eq(UserAddresses::getAccountId, accountId)
                .eq(UserAddresses::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (address == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "收货地址不存在");
        }
        return address;
    }

    private void validateStock(Products product, int quantity) {
        if (defaultIfNull(product.getStockQuantity(), 0) < quantity) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "商品库存不足");
        }
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

    private int normalizeQuantity(Integer quantity) {
        int normalized = quantity == null ? 1 : quantity;
        if (normalized < 1 || normalized > 99) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "购买数量需在 1 到 99 之间");
        }
        return normalized;
    }

    private int normalizePaymentMethod(Integer paymentMethod) {
        int normalized = paymentMethod == null ? PAYMENT_METHOD_WECHAT : paymentMethod;
        if (normalized != PAYMENT_METHOD_WECHAT
            && normalized != PAYMENT_METHOD_ALIPAY
            && normalized != PAYMENT_METHOD_WALLET) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "支付方式不支持");
        }
        return normalized;
    }

    private AccountBalances ensureUserBalance(String accountId, long now) {
        AccountBalances existing = accountBalancesService.getOne(
            new LambdaQueryWrapper<AccountBalances>()
                .eq(AccountBalances::getAccountId, accountId)
                .eq(AccountBalances::getAccountType, ACCOUNT_TYPE_USER)
                .eq(AccountBalances::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (existing != null) {
            return existing;
        }

        AccountBalances balance = new AccountBalances();
        balance.setId(SnowflakeIdUtil.nextAccountBalanceId());
        balance.setAccountId(accountId);
        balance.setAccountType(ACCOUNT_TYPE_USER);
        balance.setBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        balance.setTotalIncome(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        balance.setTotalExpense(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        balance.setCreatedTime(now);
        balance.setUpdatedTime(now);
        balance.setVersion(0);
        balance.setIsDelete(0);
        if (!accountBalancesService.save(balance)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化用户余额失败");
        }
        return balance;
    }

    private BigDecimal normalizeMoney(BigDecimal amount) {
        return defaultAmount(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private String buildProductPaymentNo(String paymentRecordId) {
        if (!StringUtils.hasText(paymentRecordId) || paymentRecordId.length() <= 2) {
            return "PAY" + System.currentTimeMillis();
        }
        return "PAY" + paymentRecordId.substring(2);
    }

    private String buildThirdPartyNo(String paymentNo, int paymentMethod) {
        String prefix = paymentMethod == PAYMENT_METHOD_ALIPAY ? "ALI" : paymentMethod == PAYMENT_METHOD_WALLET ? "WLT" : "WX";
        return prefix + compactTradeNo(paymentNo);
    }

    private String compactTradeNo(String source) {
        String value = StringUtils.hasText(source) ? source.replaceAll("[^0-9A-Za-z]", "") : "";
        if (!StringUtils.hasText(value)) {
            value = String.valueOf(System.currentTimeMillis());
        }
        return value.length() > 28 ? value.substring(value.length() - 28) : value;
    }

    private String buildProductOrderPaymentDescription(int paymentMethod) {
        if (paymentMethod == PAYMENT_METHOD_WALLET) {
            return "商城订单支付，使用余额完成支付";
        }
        if (paymentMethod == PAYMENT_METHOD_ALIPAY) {
            return "商城订单支付，使用支付宝完成支付";
        }
        return "商城订单支付，使用微信完成支付";
    }

    private String buildProductPaymentRemark(int paymentMethod) {
        if (paymentMethod == PAYMENT_METHOD_WALLET) {
            return "商城订单余额支付";
        }
        if (paymentMethod == PAYMENT_METHOD_ALIPAY) {
            return "商城订单支付宝支付";
        }
        return "商城订单微信支付";
    }

    private List<String> normalizeIdList(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        return values.stream()
            .map(UserMallOrderServiceImpl::trimToNull)
            .filter(StringUtils::hasText)
            .distinct()
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private String buildFullAddress(UserAddresses address) {
        return defaultText(address.getProvince(), "")
            + defaultText(address.getCity(), "")
            + defaultText(address.getDistrict(), "")
            + defaultText(address.getStreet(), "")
            + defaultText(address.getDetailedAddress(), "");
    }

    private String buildProductOrderNo(long now) {
        return "PO" + now + String.valueOf(Math.abs((int) (now % 1000000)));
    }

    private String buildWarrantyCardNo(String cardId) {
        if (!StringUtils.hasText(cardId) || cardId.length() <= 2) {
            return "BW" + System.currentTimeMillis();
        }
        return "BW" + cardId.substring(2);
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed) || "undefined".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private static <T> T defaultIfNull(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private static BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }

    private static final class CartSubmitSource {
        private final String cartId;
        private final String productId;
        private final int quantity;

        private CartSubmitSource(String cartId, String productId, int quantity) {
            this.cartId = cartId;
            this.productId = productId;
            this.quantity = quantity;
        }

        public String getCartId() {
            return cartId;
        }

        public String getProductId() {
            return productId;
        }

        public int getQuantity() {
            return quantity;
        }
    }

    private static final class CheckoutContext {
        private List<CartSubmitSource> sources = new ArrayList<>();
        private Map<String, Products> productMap = new LinkedHashMap<>();
        private Set<String> productIds = new LinkedHashSet<>();
        private Set<String> categoryIds = new LinkedHashSet<>();
        private BigDecimal productAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private BigDecimal shippingFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private BigDecimal totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private static final class CouponPreview {
        private String userCouponId;
        private String couponId;
        private String name;
        private Integer type;
        private String typeText;
        private String discountText;
        private BigDecimal minAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private boolean available;
        private String reason;
        private Long expireTime;
        private String applicableText;

        public BigDecimal getDiscountAmount() {
            return discountAmount == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : discountAmount;
        }

        public Long getExpireTime() {
            return expireTime;
        }
    }

    private static final class CouponUsageResult {
        private String userCouponId;
        private String couponId;
        private String couponName;
        private BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private String orderId;
        private UserCoupons userCoupon;
        private Coupons coupon;

        private boolean hasCoupon() {
            return StringUtils.hasText(userCouponId) && StringUtils.hasText(couponId);
        }

        private static CouponUsageResult empty() {
            return new CouponUsageResult();
        }
    }
}

