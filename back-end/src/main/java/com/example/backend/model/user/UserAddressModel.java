package com.example.backend.model.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

public class UserAddressModel {

    @Data
    public static class AddressItem {
        private String id;
        private String contactName;
        private String contactPhone;
        private String province;
        private String city;
        private String district;
        private String street;
        private String detailedAddress;
        private String postalCode;
        private String longitude;
        private String latitude;
        private Integer isDefault;
        private Integer addressType;
        private String addressTypeName;
        private String fullAddress;
        private String label;
        private String detail;
        private Long createdTime;
        private Long updatedTime;
    }

    @Data
    public static class SaveRequest {
        @NotBlank(message = "联系人不能为空")
        @Size(max = 50, message = "联系人长度不能超过50")
        private String contactName;

        @NotBlank(message = "联系电话不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "联系电话格式不正确")
        private String contactPhone;

        @NotBlank(message = "省份不能为空")
        @Size(max = 50, message = "省份长度不能超过50")
        private String province;

        @NotBlank(message = "城市不能为空")
        @Size(max = 50, message = "城市长度不能超过50")
        private String city;

        @NotBlank(message = "区县不能为空")
        @Size(max = 50, message = "区县长度不能超过50")
        private String district;

        @Size(max = 100, message = "街道长度不能超过100")
        private String street;

        @NotBlank(message = "详细地址不能为空")
        @Size(max = 500, message = "详细地址长度不能超过500")
        private String detailedAddress;

        @Size(max = 10, message = "邮编长度不能超过10")
        private String postalCode;

        private BigDecimal longitude;
        private BigDecimal latitude;

        @Min(value = 0, message = "isDefault仅支持0或1")
        @Max(value = 1, message = "isDefault仅支持0或1")
        private Integer isDefault;

        @Min(value = 1, message = "addressType仅支持1-3")
        @Max(value = 3, message = "addressType仅支持1-3")
        private Integer addressType;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class UpdateRequest extends SaveRequest {
        @NotBlank(message = "id不能为空")
        private String id;
    }

    @Data
    public static class IdRequest {
        @NotBlank(message = "addressId不能为空")
        private String addressId;
    }



    @Data
    public static class LocationResolveResponse {
        private String province;
        private String city;
        private String district;
        private String street;
        private String fullAddress;
        private BigDecimal longitude;
        private BigDecimal latitude;
    }

    @Data
    public static class SaveResponse {
        private String addressId;
    }
}
