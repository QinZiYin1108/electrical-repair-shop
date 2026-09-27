package com.example.backend.controller.user;

import com.example.backend.common.Result;
import com.example.backend.model.user.UserMallOrderModel;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.service.CreditRecordsService;
import com.example.backend.service.UserMallOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "用户端/商城下单")
@RequestMapping("/user/mall")
public class UserMallOrderController {

    private final UserMallOrderService userMallOrderService;
    private final CreditRecordsService creditRecordsService;

    public UserMallOrderController(
            UserMallOrderService userMallOrderService, CreditRecordsService creditRecordsService) {
        this.userMallOrderService = userMallOrderService;
        this.creditRecordsService = creditRecordsService;
    }

    @Operation(summary = "查询Cart列表")
    @GetMapping("/cart")
    public Result<UserMallOrderModel.CartListResponse> getCartList() {
        return Result.success(userMallOrderService.listCurrentUserCart());
    }

    @Operation(summary = "创建addCart")
    @PostMapping("/cart/add")
    public Result<UserMallOrderModel.CartListResponse> addCart(
            @Valid @RequestBody UserMallOrderModel.AddCartRequest request) {
        return Result.success(userMallOrderService.addCurrentUserCart(request));
    }

    @Operation(summary = "修改编辑CartQuantity")
    @PostMapping("/cart/update-quantity")
    public Result<UserMallOrderModel.CartListResponse> updateCartQuantity(
            @Valid @RequestBody UserMallOrderModel.UpdateCartQuantityRequest request) {
        return Result.success(userMallOrderService.updateCurrentUserCartQuantity(request));
    }

    @Operation(summary = "切换切换CartSelected")
    @PostMapping("/cart/toggle-selected")
    public Result<UserMallOrderModel.CartListResponse> toggleCartSelected(
            @Valid @RequestBody UserMallOrderModel.ToggleCartSelectedRequest request) {
        return Result.success(userMallOrderService.toggleCurrentUserCartSelected(request));
    }

    @Operation(summary = "切换切换CartSelectedAll")
    @PostMapping("/cart/toggle-all")
    public Result<UserMallOrderModel.CartListResponse> toggleCartSelectedAll(
            @RequestBody(required = false)
                    UserMallOrderModel.ToggleAllCartSelectedRequest request) {
        return Result.success(userMallOrderService.toggleCurrentUserCartSelectedAll(request));
    }

    @Operation(summary = "删除removeCartItems")
    @PostMapping("/cart/remove")
    public Result<UserMallOrderModel.CartListResponse> removeCartItems(
            @RequestBody(required = false) UserMallOrderModel.RemoveCartItemsRequest request) {
        return Result.success(userMallOrderService.removeCurrentUserCartItems(request));
    }

    @Operation(summary = "提交Available优惠券列表")
    @PostMapping("/orders/available-coupons")
    public Result<UserMallOrderModel.AvailableCouponListResponse> listAvailableCoupons(
            @RequestBody(required = false) UserMallOrderModel.AvailableCouponRequest request) {
        return Result.success(userMallOrderService.listCurrentUserAvailableCoupons(request));
    }

    @Operation(summary = "提交提交Order")
    @PostMapping("/orders/submit")
    public Result<UserMallOrderModel.SubmitOrderResponse> submitOrder(
            @Valid @RequestBody UserMallOrderModel.SubmitOrderRequest request) {
        creditRecordsService.checkCreditLimit(AuthUserContext.get().getAccountId(), 1, "提交商品订单");
        return Result.success(userMallOrderService.submitCurrentUserProductOrder(request));
    }
}
