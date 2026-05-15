package com.example.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.entity.UserAddresses;
import com.example.backend.model.user.UserAddressModel;

import java.math.BigDecimal;
import java.util.List;

public interface UserAddressesService extends IService<UserAddresses> {

    List<UserAddressModel.AddressItem> listCurrentUserAddresses();

    UserAddressModel.AddressItem getCurrentUserAddressDetail(String addressId);

    String createCurrentUserAddress(UserAddressModel.SaveRequest request);

    void updateCurrentUserAddress(UserAddressModel.UpdateRequest request);

    void deleteCurrentUserAddress(String addressId);

    void setCurrentUserDefaultAddress(String addressId);

    void adminDeleteUserAddress(String accountId, String addressId);

    void adminSetUserDefaultAddress(String accountId, String addressId);

    void adminUpdateUserAddress(String accountId, String addressId, UserAddressModel.SaveRequest request);

    UserAddressModel.LocationResolveResponse reverseGeocodeCurrentUser(BigDecimal latitude, BigDecimal longitude);
}
