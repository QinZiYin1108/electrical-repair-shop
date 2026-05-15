package com.example.backend.controller.user;

import com.example.backend.common.Result;
import com.example.backend.model.user.UserAddressModel;
import com.example.backend.service.UserAddressesService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/user/addresses")
public class UserAddressController {

    private final UserAddressesService userAddressesService;

    public UserAddressController(UserAddressesService userAddressesService) {
        this.userAddressesService = userAddressesService;
    }

    @GetMapping("/list")
    public Result<List<UserAddressModel.AddressItem>> listAddresses() {
        return Result.success(userAddressesService.listCurrentUserAddresses());
    }

    @GetMapping("/detail")
    public Result<UserAddressModel.AddressItem> getAddressDetail(@RequestParam("addressId") String addressId) {
        return Result.success(userAddressesService.getCurrentUserAddressDetail(addressId));
    }

    @PostMapping("/create")
    public Result<UserAddressModel.SaveResponse> createAddress(@Valid @RequestBody UserAddressModel.SaveRequest request) {
        String addressId = userAddressesService.createCurrentUserAddress(request);
        UserAddressModel.SaveResponse response = new UserAddressModel.SaveResponse();
        response.setAddressId(addressId);
        return Result.success(response);
    }

    @PostMapping("/update")
    public Result<Void> updateAddress(@Valid @RequestBody UserAddressModel.UpdateRequest request) {
        userAddressesService.updateCurrentUserAddress(request);
        return Result.success();
    }

    @PostMapping("/delete")
    public Result<Void> deleteAddress(@Valid @RequestBody UserAddressModel.IdRequest request) {
        userAddressesService.deleteCurrentUserAddress(request.getAddressId());
        return Result.success();
    }

    @PostMapping("/set-default")
    public Result<Void> setDefaultAddress(@Valid @RequestBody UserAddressModel.IdRequest request) {
        userAddressesService.setCurrentUserDefaultAddress(request.getAddressId());
        return Result.success();
    }

    @GetMapping("/reverse-geocode")
    public Result<UserAddressModel.LocationResolveResponse> reverseGeocode(
        @RequestParam("latitude") BigDecimal latitude,
        @RequestParam("longitude") BigDecimal longitude
    ) {
        return Result.success(userAddressesService.reverseGeocodeCurrentUser(latitude, longitude));
    }
}
