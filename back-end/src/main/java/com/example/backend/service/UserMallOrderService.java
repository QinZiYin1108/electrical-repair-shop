package com.example.backend.service;

import com.example.backend.model.user.UserMallOrderModel;

public interface UserMallOrderService {

    UserMallOrderModel.CartListResponse listCurrentUserCart();

    UserMallOrderModel.CartListResponse addCurrentUserCart(
            UserMallOrderModel.AddCartRequest request);

    UserMallOrderModel.CartListResponse updateCurrentUserCartQuantity(
            UserMallOrderModel.UpdateCartQuantityRequest request);

    UserMallOrderModel.CartListResponse toggleCurrentUserCartSelected(
            UserMallOrderModel.ToggleCartSelectedRequest request);

    UserMallOrderModel.CartListResponse toggleCurrentUserCartSelectedAll(
            UserMallOrderModel.ToggleAllCartSelectedRequest request);

    UserMallOrderModel.CartListResponse removeCurrentUserCartItems(
            UserMallOrderModel.RemoveCartItemsRequest request);

    UserMallOrderModel.AvailableCouponListResponse listCurrentUserAvailableCoupons(
            UserMallOrderModel.AvailableCouponRequest request);

    UserMallOrderModel.SubmitOrderResponse submitCurrentUserProductOrder(
            UserMallOrderModel.SubmitOrderRequest request);

    /** 释放未支付外部订单占用的资源：回补预占库存并恢复优惠券。 */
    void releaseUnpaidOrderResources(String orderId);

    /** 关闭超时未支付的商品订单并释放其预占资源，返回关闭的订单数。 */
    int closeTimedOutUnpaidOrders(long now, long timeoutMillis);
}
