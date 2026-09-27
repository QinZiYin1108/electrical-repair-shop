package com.example.backend.controller.user;

import com.example.backend.common.Result;
import com.example.backend.model.user.UserAddressModel;
import com.example.backend.service.UserAddressesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "用户端/收货地址")
@RequestMapping("/user/addresses")
public class UserAddressController {

    private final UserAddressesService userAddressesService;

    public UserAddressController(UserAddressesService userAddressesService) {
        this.userAddressesService = userAddressesService;
    }

    @Operation(summary = "查询地址列表")
    @GetMapping("/list")
    public Result<List<UserAddressModel.AddressItem>> listAddresses() {
        return Result.success(userAddressesService.listCurrentUserAddresses());
    }

    @Operation(summary = "查询Address详情")
    @GetMapping("/detail")
    public Result<UserAddressModel.AddressItem> getAddressDetail(
            @RequestParam("addressId") String addressId) {
        return Result.success(userAddressesService.getCurrentUserAddressDetail(addressId));
    }

    @Operation(summary = "创建创建Address")
    @PostMapping("/create")
    public Result<UserAddressModel.SaveResponse> createAddress(
            @Valid @RequestBody UserAddressModel.SaveRequest request) {
        String addressId = userAddressesService.createCurrentUserAddress(request);
        UserAddressModel.SaveResponse response = new UserAddressModel.SaveResponse();
        response.setAddressId(addressId);
        return Result.success(response);
    }

    @Operation(summary = "创建编辑Address")
    @PostMapping("/update")
    public Result<Void> updateAddress(@Valid @RequestBody UserAddressModel.UpdateRequest request) {
        userAddressesService.updateCurrentUserAddress(request);
        return Result.success();
    }

    @Operation(summary = "创建删除Address")
    @PostMapping("/delete")
    public Result<Void> deleteAddress(@Valid @RequestBody UserAddressModel.IdRequest request) {
        userAddressesService.deleteCurrentUserAddress(request.getAddressId());
        return Result.success();
    }

    @Operation(summary = "创建setDefaultAddress")
    @PostMapping("/set-default")
    public Result<Void> setDefaultAddress(@Valid @RequestBody UserAddressModel.IdRequest request) {
        userAddressesService.setCurrentUserDefaultAddress(request.getAddressId());
        return Result.success();
    }

    @Operation(summary = "查询reverseGeocode")
    @GetMapping("/reverse-geocode")
    public Result<UserAddressModel.LocationResolveResponse> reverseGeocode(
            @RequestParam("latitude") BigDecimal latitude,
            @RequestParam("longitude") BigDecimal longitude) {
        return Result.success(userAddressesService.reverseGeocodeCurrentUser(latitude, longitude));
    }
}
