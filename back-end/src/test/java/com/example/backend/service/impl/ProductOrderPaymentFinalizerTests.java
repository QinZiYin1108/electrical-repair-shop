package com.example.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.backend.entity.OrderItems;
import com.example.backend.entity.PaymentRecords;
import com.example.backend.entity.ProductOrders;
import com.example.backend.entity.Products;
import com.example.backend.service.InventoryService;
import com.example.backend.service.OrderItemsService;
import com.example.backend.service.ProductOrdersService;
import com.example.backend.service.ProductsService;
import com.example.backend.service.WarrantyCardsService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductOrderPaymentFinalizerTests {
    private ProductOrdersService orders;
    private OrderItemsService orderItems;
    private ProductsService products;
    private InventoryService inventory;
    private WarrantyCardsService warrantyCards;
    private ProductOrderPaymentFinalizer finalizer;

    @BeforeEach
    void setUp() {
        orders = mock(ProductOrdersService.class);
        orderItems = mock(OrderItemsService.class);
        products = mock(ProductsService.class);
        inventory = mock(InventoryService.class);
        warrantyCards = mock(WarrantyCardsService.class);
        finalizer =
                new ProductOrderPaymentFinalizer(
                        orders, orderItems, products, inventory, warrantyCards);
    }

    @Test
    @SuppressWarnings("unchecked")
    void paidSuccessMarksOrderConfirmedAndCreatesWarrantyCards() {
        ProductOrders order = new ProductOrders();
        order.setId("PO1");
        order.setAccountId("U1");
        order.setPaymentStatus(1);
        when(orders.getOne(any(Wrapper.class), eq(false))).thenReturn(order);
        when(orders.updateById(any(ProductOrders.class))).thenReturn(true);
        when(orderItems.list(any(Wrapper.class))).thenReturn(List.of(item("P1", 2)));
        Products product = new Products();
        product.setId("P1");
        product.setName("电风扇");
        product.setModel("F-1");
        product.setWarrantyPeriod(12);
        when(products.getById("P1")).thenReturn(product);
        when(warrantyCards.saveBatch(any())).thenReturn(true);

        finalizer.onPaymentSuccess(payment(), 1000L);

        assertEquals(2, order.getPaymentStatus());
        assertEquals(2, order.getOrderStatus());
        assertEquals(1000L, order.getPaymentTime());
        verify(inventory).increaseSales("P1", 2, 1000L);
        verify(warrantyCards).saveBatch(any());
        verify(orders).updateById(any(ProductOrders.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void alreadyPaidOrderIsIdempotent() {
        ProductOrders order = new ProductOrders();
        order.setId("PO1");
        order.setAccountId("U1");
        order.setPaymentStatus(2);
        when(orders.getOne(any(Wrapper.class), eq(false))).thenReturn(order);

        finalizer.onPaymentSuccess(payment(), 1000L);

        verify(inventory, never()).increaseSales(any(), anyInt(), anyLong());
        verify(warrantyCards, never()).saveBatch(any());
        verify(orders, never()).updateById(any());
    }

    private PaymentRecords payment() {
        PaymentRecords payment = new PaymentRecords();
        payment.setPaymentNo("PAY1");
        payment.setOrderId("PO1");
        payment.setOrderType(2);
        payment.setAccountId("U1");
        payment.setPaymentMethod(1);
        payment.setPaymentStatus(3);
        return payment;
    }

    private OrderItems item(String productId, int quantity) {
        OrderItems item = new OrderItems();
        item.setId("OI1");
        item.setOrderId("PO1");
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }
}
