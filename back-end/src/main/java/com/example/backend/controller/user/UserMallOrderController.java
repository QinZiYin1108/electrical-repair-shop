package com.example.backend.controller.user;

import com.example.backend.common.Result;
import com.example.backend.model.user.UserMallOrderModel;
import com.example.backend.service.UserMallOrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/mall")
public class UserMallOrderController {

    private final UserMallOrderService userMallOrderService;

    public UserMallOrderController(UserMallOrderService userMallOrderService) {
        this.userMallOrderService = userMallOrderService;
    }

    @GetMapping("/cart")
    public Result<UserMallOrderModel.CartListResponse> getCartList() {
        return Result.success(userMallOrderService.listCurrentUserCart());
    }

    @PostMapping("/cart/add")
    public Result<UserMallOrderModel.CartListResponse> addCart(@Valid @RequestBody UserMallOrderModel.AddCartRequest request) {
        return Result.success(userMallOrderService.addCurrentUserCart(request));
    }

    @PostMapping("/cart/update-quantity")
    public Result<UserMallOrderModel.CartListResponse> updateCartQuantity(@Valid @RequestBody UserMallOrderModel.UpdateCartQuantityRequest request) {
        return Result.success(userMallOrderService.updateCurrentUserCartQuantity(request));
    }

    @PostMapping("/cart/toggle-selected")
    public Result<UserMallOrderModel.CartListResponse> toggleCartSelected(@Valid @RequestBody UserMallOrderModel.ToggleCartSelectedRequest request) {
        return Result.success(userMallOrderService.toggleCurrentUserCartSelected(request));
    }

    @PostMapping("/cart/toggle-all")
    public Result<UserMallOrderModel.CartListResponse> toggleCartSelectedAll(@RequestBody(required = false) UserMallOrderModel.ToggleAllCartSelectedRequest request) {
        return Result.success(userMallOrderService.toggleCurrentUserCartSelectedAll(request));
    }

    @PostMapping("/cart/remove")
    public Result<UserMallOrderModel.CartListResponse> removeCartItems(@RequestBody(required = false) UserMallOrderModel.RemoveCartItemsRequest request) {
        return Result.success(userMallOrderService.removeCurrentUserCartItems(request));
    }

    @PostMapping("/orders/available-coupons")
    public Result<UserMallOrderModel.AvailableCouponListResponse> listAvailableCoupons(
        @RequestBody(required = false) UserMallOrderModel.AvailableCouponRequest request
    ) {
        return Result.success(userMallOrderService.listCurrentUserAvailableCoupons(request));
    }

    @PostMapping("/orders/submit")
    public Result<UserMallOrderModel.SubmitOrderResponse> submitOrder(@Valid @RequestBody UserMallOrderModel.SubmitOrderRequest request) {
        return Result.success(userMallOrderService.submitCurrentUserProductOrder(request));
    }
}
