package com.example.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.backend.entity.ProductOrders;
import com.example.backend.mapper.ShoppingCartsMapper;
import com.example.backend.service.AccountBalancesService;
import com.example.backend.service.CouponsService;
import com.example.backend.service.FundFlowsService;
import com.example.backend.service.InventoryService;
import com.example.backend.service.OrderItemsService;
import com.example.backend.service.PaymentRecordsService;
import com.example.backend.service.ProductCategoriesService;
import com.example.backend.service.ProductOrdersService;
import com.example.backend.service.ProductsService;
import com.example.backend.service.ShoppingCartsService;
import com.example.backend.service.StoresService;
import com.example.backend.service.UserAddressesService;
import com.example.backend.service.UserCouponsService;
import com.example.backend.service.WarrantyCardsService;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserMallOrderServiceImplTests {

    @Test
    @SuppressWarnings("unchecked")
    void closeTimedOutUnpaidOrdersCancelsAndReleases() {
        ProductOrdersService orders = mock(ProductOrdersService.class);
        OrderItemsService items = mock(OrderItemsService.class);
        UserCouponsService userCoupons = mock(UserCouponsService.class);
        UserMallOrderServiceImpl service =
                new UserMallOrderServiceImpl(
                        mock(ShoppingCartsService.class),
                        mock(ShoppingCartsMapper.class),
                        mock(ProductsService.class),
                        mock(ProductCategoriesService.class),
                        mock(UserAddressesService.class),
                        orders,
                        items,
                        mock(AccountBalancesService.class),
                        mock(FundFlowsService.class),
                        mock(PaymentRecordsService.class),
                        mock(CouponsService.class),
                        userCoupons,
                        mock(WarrantyCardsService.class),
                        mock(StoresService.class),
                        mock(InventoryService.class));

        ProductOrders order = new ProductOrders();
        order.setId("PO1");
        order.setOrderStatus(1);
        order.setPaymentStatus(1);
        order.setCreatedTime(1000L);
        when(orders.list(any(Wrapper.class))).thenReturn(List.of(order));
        when(items.list(any(Wrapper.class))).thenReturn(List.of());
        when(userCoupons.getOne(any(Wrapper.class), eq(false))).thenReturn(null);
        when(orders.updateById(any(ProductOrders.class))).thenReturn(true);

        int closed = service.closeTimedOutUnpaidOrders(10_000_000L, 60_000L);

        assertEquals(1, closed);
        assertEquals(6, order.getOrderStatus());
        assertEquals("支付超时，系统自动关闭", order.getCancelReason());
    }
}
