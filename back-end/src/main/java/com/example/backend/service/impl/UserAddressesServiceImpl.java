package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.UserAddresses;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.UserAddressesMapper;
import com.example.backend.model.user.UserAddressModel;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.UserAddressesService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class UserAddressesServiceImpl extends ServiceImpl<UserAddressesMapper, UserAddresses>
    implements UserAddressesService {

    private static final int DEFAULT_NO = 0;
    private static final int DEFAULT_YES = 1;
    private static final int ADDRESS_TYPE_HOME = 1;

    @Value("${baidu.map.ak:}")
    private String baiduAk;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<UserAddressModel.AddressItem> listCurrentUserAddresses() {
        LoginUserInfo user = requireCurrentUser();
        return toAddressItems(listAddressEntities(user.getAccountId()));
    }

    @Override
    public UserAddressModel.AddressItem getCurrentUserAddressDetail(String addressId) {
        LoginUserInfo user = requireCurrentUser();
        UserAddresses entity = requireOwnedAddress(user.getAccountId(), addressId);
        return toAddressItem(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createCurrentUserAddress(UserAddressModel.SaveRequest request) {
        LoginUserInfo user = requireCurrentUser();
        String accountId = user.getAccountId();
        long now = System.currentTimeMillis();

        int isDefault = normalizeDefaultFlag(request.getIsDefault());
        long addressCount = count(
            new LambdaQueryWrapper<UserAddresses>()
                .eq(UserAddresses::getAccountId, accountId)
                .eq(UserAddresses::getIsDelete, 0)
        );
        if (addressCount == 0) {
            isDefault = DEFAULT_YES;
        }

        if (isDefault == DEFAULT_YES) {
            clearDefaultAddress(accountId, null, now);
        }

        UserAddresses entity = new UserAddresses();
        entity.setId(SnowflakeIdUtil.nextUserAddressId());
        entity.setAccountId(accountId);
        entity.setCreatedTime(now);
        entity.setUpdatedTime(now);
        entity.setIsDelete(0);
        fillAddressFields(entity, request, isDefault);

        if (!save(entity)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "地址操作失败");
        }
        ensureOneDefaultAddress(accountId, now);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCurrentUserAddress(UserAddressModel.UpdateRequest request) {
        LoginUserInfo user = requireCurrentUser();
        String accountId = user.getAccountId();
        UserAddresses entity = requireOwnedAddress(accountId, request.getId());

        int currentDefault = entity.getIsDefault() != null && entity.getIsDefault() == 1 ? DEFAULT_YES : DEFAULT_NO;
        int nextDefault = normalizeDefaultFlag(request.getIsDefault());

        if (currentDefault == DEFAULT_YES && nextDefault == DEFAULT_NO && !hasOtherAddress(accountId, entity.getId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "至少保留一个默认地址");
        }

        long now = System.currentTimeMillis();
        if (nextDefault == DEFAULT_YES) {
            clearDefaultAddress(accountId, entity.getId(), now);
        }

        entity.setUpdatedTime(now);
        fillAddressFields(entity, request, nextDefault);
        if (!updateById(entity)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "地址操作失败");
        }
        ensureOneDefaultAddress(accountId, now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCurrentUserAddress(String addressId) {
        LoginUserInfo user = requireCurrentUser();
        String accountId = user.getAccountId();
        UserAddresses entity = requireOwnedAddress(accountId, addressId);

        long now = System.currentTimeMillis();
        if (!removeById(entity.getId())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Address operation failed");
        }
        ensureOneDefaultAddress(accountId, now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setCurrentUserDefaultAddress(String addressId) {
        LoginUserInfo user = requireCurrentUser();
        String accountId = user.getAccountId();
        UserAddresses entity = requireOwnedAddress(accountId, addressId);
        if (entity.getIsDefault() != null && entity.getIsDefault() == DEFAULT_YES) {
            return;
        }

        long now = System.currentTimeMillis();
        clearDefaultAddress(accountId, entity.getId(), now);
        entity.setIsDefault(DEFAULT_YES);
        entity.setUpdatedTime(now);
        if (!updateById(entity)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "设置默认地址失败");
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDeleteUserAddress(String accountId, String addressId) {
        UserAddresses entity = requireOwnedAddress(accountId, addressId);
        long now = System.currentTimeMillis();
        if (!removeById(entity.getId())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "地址操作失败");
        }
        ensureOneDefaultAddress(accountId, now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminSetUserDefaultAddress(String accountId, String addressId) {
        UserAddresses entity = requireOwnedAddress(accountId, addressId);
        if (entity.getIsDefault() != null && entity.getIsDefault() == DEFAULT_YES) {
            return;
        }
        long now = System.currentTimeMillis();
        clearDefaultAddress(accountId, entity.getId(), now);
        entity.setIsDefault(DEFAULT_YES);
        entity.setUpdatedTime(now);
        if (!updateById(entity)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "设置默认地址失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminUpdateUserAddress(String accountId, String addressId, UserAddressModel.SaveRequest request) {
        UserAddresses entity = requireOwnedAddress(accountId, addressId);

        int currentDefault = entity.getIsDefault() != null && entity.getIsDefault() == 1 ? DEFAULT_YES : DEFAULT_NO;
        int nextDefault = normalizeDefaultFlag(request.getIsDefault());

        if (currentDefault == DEFAULT_YES && nextDefault == DEFAULT_NO && !hasOtherAddress(accountId, entity.getId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "至少保留一个默认地址");
        }

        long now = System.currentTimeMillis();
        if (nextDefault == DEFAULT_YES) {
            clearDefaultAddress(accountId, entity.getId(), now);
        }

        entity.setUpdatedTime(now);
        fillAddressFields(entity, request, nextDefault);
        if (!updateById(entity)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "地址操作失败");
        }
        ensureOneDefaultAddress(accountId, now);
    }

    @Override
    public UserAddressModel.LocationResolveResponse reverseGeocodeCurrentUser(BigDecimal latitude, BigDecimal longitude) {
        requireCurrentUser();
        if (latitude == null || longitude == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "经纬度不能为空");
        }
        if (!isValidLatLng(latitude, longitude)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非法的经纬度");
        }

        ReverseGeocodeResult geocode = reverseGeocode(latitude, longitude);
        UserAddressModel.LocationResolveResponse response = new UserAddressModel.LocationResolveResponse();
        response.setProvince(geocode.province);
        response.setCity(geocode.city);
        response.setDistrict(geocode.district);
        response.setStreet(geocode.street);
        response.setFullAddress(buildFullAddress(geocode.province, geocode.city, geocode.district, geocode.street));
        response.setLatitude(latitude);
        response.setLongitude(longitude);
        return response;
    }

    private void fillAddressFields(UserAddresses entity, UserAddressModel.SaveRequest request, int isDefault) {
        entity.setContactName(trimToNull(request.getContactName()));
        entity.setContactPhone(trimToNull(request.getContactPhone()));
        entity.setProvince(trimToNull(request.getProvince()));
        entity.setCity(trimToNull(request.getCity()));
        entity.setDistrict(trimToNull(request.getDistrict()));
        entity.setStreet(trimToNull(request.getStreet()));
        entity.setDetailedAddress(trimToNull(request.getDetailedAddress()));
        entity.setPostalCode(trimToNull(request.getPostalCode()));
        entity.setLongitude(request.getLongitude());
        entity.setLatitude(request.getLatitude());
        entity.setAddressType(normalizeAddressType(request.getAddressType()));
        entity.setIsDefault(isDefault);
    }

    private List<UserAddresses> listAddressEntities(String accountId) {
        return list(
            new LambdaQueryWrapper<UserAddresses>()
                .eq(UserAddresses::getAccountId, accountId)
                .eq(UserAddresses::getIsDelete, 0)
                .orderByDesc(UserAddresses::getIsDefault)
                .orderByDesc(UserAddresses::getUpdatedTime)
                .orderByDesc(UserAddresses::getCreatedTime)
        );
    }

    private UserAddresses requireOwnedAddress(String accountId, String addressId) {
        String normalizedAddressId = trimToNull(addressId);
        if (!StringUtils.hasText(normalizedAddressId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "addressId不能为空");
        }
        UserAddresses entity = getOne(
            new LambdaQueryWrapper<UserAddresses>()
                .eq(UserAddresses::getId, normalizedAddressId)
                .eq(UserAddresses::getAccountId, accountId)
                .eq(UserAddresses::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (entity == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "地址不存在");
        }
        return entity;
    }

    private void clearDefaultAddress(String accountId, String excludeAddressId, long now) {
        LambdaUpdateWrapper<UserAddresses> wrapper = new LambdaUpdateWrapper<UserAddresses>()
            .eq(UserAddresses::getAccountId, accountId)
            .eq(UserAddresses::getIsDelete, 0)
            .eq(UserAddresses::getIsDefault, 1);
        if (StringUtils.hasText(excludeAddressId)) {
            wrapper.ne(UserAddresses::getId, excludeAddressId);
        }
        UserAddresses update = new UserAddresses();
        update.setIsDefault(DEFAULT_NO);
        update.setUpdatedTime(now);
        update(update, wrapper);
    }

    private void ensureOneDefaultAddress(String accountId, long now) {
        long total = count(
            new LambdaQueryWrapper<UserAddresses>()
                .eq(UserAddresses::getAccountId, accountId)
                .eq(UserAddresses::getIsDelete, 0)
        );
        if (total <= 0) {
            return;
        }
        long defaultCount = count(
            new LambdaQueryWrapper<UserAddresses>()
                .eq(UserAddresses::getAccountId, accountId)
                .eq(UserAddresses::getIsDelete, 0)
                .eq(UserAddresses::getIsDefault, 1)
        );
        if (defaultCount > 0) {
            return;
        }
        UserAddresses fallback = getOne(
            new LambdaQueryWrapper<UserAddresses>()
                .eq(UserAddresses::getAccountId, accountId)
                .eq(UserAddresses::getIsDelete, 0)
                .orderByDesc(UserAddresses::getUpdatedTime)
                .orderByDesc(UserAddresses::getCreatedTime)
                .last("limit 1"),
            false
        );
        if (fallback == null) {
            return;
        }
        fallback.setIsDefault(DEFAULT_YES);
        fallback.setUpdatedTime(now);
        if (!updateById(fallback)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "设置默认地址失败");
        }
    }

    private boolean hasOtherAddress(String accountId, String excludeAddressId) {
        LambdaQueryWrapper<UserAddresses> wrapper = new LambdaQueryWrapper<UserAddresses>()
            .eq(UserAddresses::getAccountId, accountId)
            .eq(UserAddresses::getIsDelete, 0);
        if (StringUtils.hasText(excludeAddressId)) {
            wrapper.ne(UserAddresses::getId, excludeAddressId);
        }
        return count(wrapper) > 0;
    }

    private int normalizeDefaultFlag(Integer isDefault) {
        if (isDefault == null) {
            return DEFAULT_NO;
        }
        if (isDefault != 0 && isDefault != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "isDefault仅支持0或1");
        }
        return isDefault;
    }

    private int normalizeAddressType(Integer addressType) {
        if (addressType == null) {
            return ADDRESS_TYPE_HOME;
        }
        if (addressType < 1 || addressType > 3) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "addressType仅支持1-3");
        }
        return addressType;
    }

    private List<UserAddressModel.AddressItem> toAddressItems(List<UserAddresses> entities) {
        List<UserAddressModel.AddressItem> result = new ArrayList<>();
        for (UserAddresses entity : entities) {
            result.add(toAddressItem(entity));
        }
        return result;
    }

    private UserAddressModel.AddressItem toAddressItem(UserAddresses entity) {
        UserAddressModel.AddressItem item = new UserAddressModel.AddressItem();
        item.setId(entity.getId());
        item.setContactName(safe(entity.getContactName()));
        item.setContactPhone(safe(entity.getContactPhone()));
        item.setProvince(safe(entity.getProvince()));
        item.setCity(safe(entity.getCity()));
        item.setDistrict(safe(entity.getDistrict()));
        item.setStreet(safe(entity.getStreet()));
        item.setDetailedAddress(safe(entity.getDetailedAddress()));
        item.setPostalCode(safe(entity.getPostalCode()));
        item.setLongitude(entity.getLongitude() == null ? "" : entity.getLongitude().toPlainString());
        item.setLatitude(entity.getLatitude() == null ? "" : entity.getLatitude().toPlainString());
        item.setIsDefault(entity.getIsDefault() != null && entity.getIsDefault() == 1 ? 1 : 0);
        item.setAddressType(entity.getAddressType() == null ? ADDRESS_TYPE_HOME : entity.getAddressType());
        item.setAddressTypeName(mapAddressTypeName(item.getAddressType()));
        item.setFullAddress(buildFullAddress(entity));
        item.setLabel((safe(entity.getContactName()) + " " + safe(entity.getContactPhone())).trim());
        item.setDetail(item.getFullAddress());
        item.setCreatedTime(entity.getCreatedTime());
        item.setUpdatedTime(entity.getUpdatedTime());
        return item;
    }

    private String buildFullAddress(UserAddresses entity) {
        return safe(entity.getProvince())
            + safe(entity.getCity())
            + safe(entity.getDistrict())
            + safe(entity.getStreet())
            + safe(entity.getDetailedAddress());
    }

    private String mapAddressTypeName(Integer addressType) {
        if (addressType == null || addressType == 1) {
            return "家庭";
        }
        if (addressType == 2) {
            return "公司";
        }
        return "其他";
    }

    private boolean isValidLatLng(BigDecimal lat, BigDecimal lng) {
        return lat.compareTo(BigDecimal.valueOf(-90)) >= 0
            && lat.compareTo(BigDecimal.valueOf(90)) <= 0
            && lng.compareTo(BigDecimal.valueOf(-180)) >= 0
            && lng.compareTo(BigDecimal.valueOf(180)) <= 0;
    }

    private ReverseGeocodeResult reverseGeocode(BigDecimal latitude, BigDecimal longitude) {
        if (!StringUtils.hasText(baiduAk)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "百度地图AK未配置");
        }

        String url = UriComponentsBuilder
            .fromHttpUrl("https://api.map.baidu.com/reverse_geocoding/v3/")
            .queryParam("ak", baiduAk)
            .queryParam("output", "json")
            .queryParam("coordtype", "gcj02ll")
            .queryParam("extensions_poi", "0")
            .queryParam("location", latitude + "," + longitude)
            .toUriString();

        String body;
        try {
            body = restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "逆地理编码失败");
        }
        if (!StringUtils.hasText(body)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "逆地理编码失败");
        }

        try {
            Map<?, ?> root = objectMapper.readValue(body, Map.class);
            Object status = root.get("status");
            if (!(status instanceof Number) || ((Number) status).intValue() != 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "逆地理编码失败");
            }
            Object resultObj = root.get("result");
            if (!(resultObj instanceof Map)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "逆地理编码失败");
            }
            Map<?, ?> result = (Map<?, ?>) resultObj;
            Object componentObj = result.get("addressComponent");
            if (!(componentObj instanceof Map)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "逆地理编码失败");
            }
            Map<?, ?> component = (Map<?, ?>) componentObj;

            ReverseGeocodeResult ret = new ReverseGeocodeResult();
            ret.province = asText(component.get("province"));
            ret.city = asText(component.get("city"));
            ret.district = asText(component.get("district"));
            String street = concatText(asText(component.get("street")), asText(component.get("street_number")));
            if (!StringUtils.hasText(street)) {
                street = asText(component.get("town"));
            }
            ret.street = street;
            return ret;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "逆地理编码失败");
        }
    }

    private String buildFullAddress(String province, String city, String district, String street) {
        return safe(province) + safe(city) + safe(district) + safe(street);
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return StringUtils.hasText(text) ? text : null;
    }

    private String concatText(String first, String second) {
        if (!StringUtils.hasText(first) && !StringUtils.hasText(second)) {
            return null;
        }
        return safe(first) + safe(second);
    }

    private static class ReverseGeocodeResult {
        private String province;
        private String city;
        private String district;
        private String street;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private LoginUserInfo requireCurrentUser() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.USER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问用户地址");
        }
        return user;
    }
}
