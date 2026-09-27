package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.OrderItems;
import com.example.backend.entity.PaymentRecords;
import com.example.backend.entity.ProductOrders;
import com.example.backend.entity.Products;
import com.example.backend.entity.WarrantyCards;
import com.example.backend.exception.BusinessException;
import com.example.backend.payment.OrderPaymentFinalizer;
import com.example.backend.service.InventoryService;
import com.example.backend.service.OrderItemsService;
import com.example.backend.service.ProductOrdersService;
import com.example.backend.service.ProductsService;
import com.example.backend.service.WarrantyCardsService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商城订单外部支付成功后的推进：钱包支付仍走原有同步链路，本类只处理渠道支付（微信）回调成功后的订单推进、销量确认与保修卡生成。
 *
 * <p>库存已在下单时预占（原子扣减），此处不再扣库存，只确认销量。
 */
@Component
public class ProductOrderPaymentFinalizer implements OrderPaymentFinalizer {
    private static final int ORDER_TYPE_PRODUCT = 2;
    private static final int ORDER_STATUS_PENDING_DELIVERY = 2;
    private static final int PAYMENT_STATUS_PAID = 2;
    private static final int WARRANTY_TYPE_STORE = 2;
    private static final int WARRANTY_STATUS_ACTIVE = 1;

    private final ProductOrdersService productOrdersService;
    private final OrderItemsService orderItemsService;
    private final ProductsService productsService;
    private final InventoryService inventoryService;
    private final WarrantyCardsService warrantyCardsService;

    public ProductOrderPaymentFinalizer(
            ProductOrdersService productOrdersService,
            OrderItemsService orderItemsService,
            ProductsService productsService,
            InventoryService inventoryService,
            WarrantyCardsService warrantyCardsService) {
        this.productOrdersService = productOrdersService;
        this.orderItemsService = orderItemsService;
        this.productsService = productsService;
        this.inventoryService = inventoryService;
        this.warrantyCardsService = warrantyCardsService;
    }

    @Override
    public int orderType() {
        return ORDER_TYPE_PRODUCT;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onPaymentSuccess(PaymentRecords payment, long now) {
        if (payment == null
                || !Integer.valueOf(ORDER_TYPE_PRODUCT).equals(payment.getOrderType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非商品订单支付单");
        }
        ProductOrders order =
                productOrdersService.getOne(
                        new LambdaQueryWrapper<ProductOrders>()
                                .eq(ProductOrders::getId, payment.getOrderId())
                                .eq(ProductOrders::getIsDelete, 0)
                                .last("limit 1 for update"),
                        false);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品订单不存在");
        }
        if (!order.getAccountId().equals(payment.getAccountId())) {
            throw new BusinessException(ErrorCode.DATA_INTEGRITY_ERROR, "支付单与订单账号不匹配");
        }
        if (Integer.valueOf(PAYMENT_STATUS_PAID).equals(order.getPaymentStatus())) {
            return;
        }

        List<OrderItems> items =
                orderItemsService.list(
                        new LambdaQueryWrapper<OrderItems>()
                                .eq(OrderItems::getOrderId, order.getId())
                                .eq(OrderItems::getIsDelete, 0));
        for (OrderItems item : items) {
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            if (quantity > 0) {
                inventoryService.increaseSales(item.getProductId(), quantity, now);
            }
        }
        createWarrantyCards(order, items, now);

        order.setOrderStatus(ORDER_STATUS_PENDING_DELIVERY);
        order.setPaymentStatus(PAYMENT_STATUS_PAID);
        order.setPaymentMethod(payment.getPaymentMethod());
        order.setPaymentTime(now);
        order.setUpdatedTime(now);
        if (!productOrdersService.updateById(order)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新订单状态失败");
        }
    }

    private void createWarrantyCards(ProductOrders order, List<OrderItems> items, long now) {
        LocalDate baseDate = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate();
        Date purchaseDate = Date.from(baseDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        List<WarrantyCards> cards = new ArrayList<>();
        for (OrderItems item : items) {
            Products product = productsService.getById(item.getProductId());
            int warrantyPeriod =
                    product == null || product.getWarrantyPeriod() == null
                            ? 0
                            : product.getWarrantyPeriod();
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            if (product == null || warrantyPeriod <= 0 || quantity <= 0) {
                continue;
            }
            Date warrantyEndDate =
                    Date.from(
                            baseDate.plusMonths(warrantyPeriod)
                                    .atStartOfDay(ZoneId.systemDefault())
                                    .toInstant());
            for (int index = 0; index < quantity; index++) {
                WarrantyCards card = new WarrantyCards();
                String cardId = SnowflakeIdUtil.nextWarrantyCardId();
                card.setId(cardId);
                card.setCardNo(buildWarrantyCardNo(cardId));
                card.setUserId(order.getAccountId());
                card.setProductId(product.getId());
                card.setProductName(product.getName() == null ? "商品" : product.getName());
                card.setProductModel(product.getModel() == null ? "-" : product.getModel());
                card.setPurchaseDate(purchaseDate);
                card.setWarrantyStartDate(purchaseDate);
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

    private String buildWarrantyCardNo(String cardId) {
        if (cardId == null || cardId.length() <= 2) {
            return "WC" + System.currentTimeMillis();
        }
        return "WC" + cardId.substring(2);
    }
}
