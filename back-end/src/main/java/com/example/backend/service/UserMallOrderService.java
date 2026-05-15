package com.example.backend.service;

import com.example.backend.model.user.UserMallOrderModel;

public interface UserMallOrderService {

    UserMallOrderModel.CartListResponse listCurrentUserCart();

    UserMallOrderModel.CartListResponse addCurrentUserCart(UserMallOrderModel.AddCartRequest request);

    UserMallOrderModel.CartListResponse updateCurrentUserCartQuantity(UserMallOrderModel.UpdateCartQuantityRequest request);

    UserMallOrderModel.CartListResponse toggleCurrentUserCartSelected(UserMallOrderModel.ToggleCartSelectedRequest request);

    UserMallOrderModel.CartListResponse toggleCurrentUserCartSelectedAll(UserMallOrderModel.ToggleAllCartSelectedRequest request);

    UserMallOrderModel.CartListResponse removeCurrentUserCartItems(UserMallOrderModel.RemoveCartItemsRequest request);

    UserMallOrderModel.AvailableCouponListResponse listCurrentUserAvailableCoupons(UserMallOrderModel.AvailableCouponRequest request);

    UserMallOrderModel.SubmitOrderResponse submitCurrentUserProductOrder(UserMallOrderModel.SubmitOrderRequest request);
}
