package com.example.backend.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

public class UserAddressModel {

    @Data
    public static class AddressItem {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "联系名称")
        private String contactName;

        @Schema(description = "联系电话")
        private String contactPhone;

        @Schema(description = "province")
        private String province;

        @Schema(description = "city")
        private String city;

        @Schema(description = "district")
        private String district;

        @Schema(description = "street")
        private String street;

        @Schema(description = "detailed地址")
        private String detailedAddress;

        @Schema(description = "postal编码")
        private String postalCode;

        @Schema(description = "经度")
        private String longitude;

        @Schema(description = "纬度")
        private String latitude;

        @Schema(description = "isDefault")
        private Integer isDefault;

        @Schema(description = "地址类型")
        private Integer addressType;

        @Schema(description = "地址类型名称")
        private String addressTypeName;

        @Schema(description = "full地址")
        private String fullAddress;

        @Schema(description = "标签")
        private String label;

        @Schema(description = "详情")
        private String detail;

        @Schema(description = "创建时间")
        private Long createdTime;

        @Schema(description = "更新时间")
        private Long updatedTime;
    }

    @Data
    public static class SaveRequest {
        @NotBlank(message = "联系人不能为空")
        @Size(max = 50, message = "联系人长度不能超过50")
        @Schema(description = "联系名称")
        private String contactName;

        @NotBlank(message = "联系电话不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "联系电话格式不正确")
        @Schema(description = "联系电话")
        private String contactPhone;

        @NotBlank(message = "省份不能为空")
        @Size(max = 50, message = "省份长度不能超过50")
        @Schema(description = "province")
        private String province;

        @NotBlank(message = "城市不能为空")
        @Size(max = 50, message = "城市长度不能超过50")
        @Schema(description = "city")
        private String city;

        @NotBlank(message = "区县不能为空")
        @Size(max = 50, message = "区县长度不能超过50")
        @Schema(description = "district")
        private String district;

        @Size(max = 100, message = "街道长度不能超过100")
        @Schema(description = "street")
        private String street;

        @NotBlank(message = "详细地址不能为空")
        @Size(max = 500, message = "详细地址长度不能超过500")
        @Schema(description = "detailed地址")
        private String detailedAddress;

        @Size(max = 10, message = "邮编长度不能超过10")
        @Schema(description = "postal编码")
        private String postalCode;

        @Schema(description = "经度")
        private BigDecimal longitude;

        @Schema(description = "纬度")
        private BigDecimal latitude;

        @Min(value = 0, message = "isDefault仅支持0或1")
        @Max(value = 1, message = "isDefault仅支持0或1")
        @Schema(description = "isDefault")
        private Integer isDefault;

        @Min(value = 1, message = "addressType仅支持1-3")
        @Max(value = 3, message = "addressType仅支持1-3")
        @Schema(description = "地址类型")
        private Integer addressType;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class UpdateRequest extends SaveRequest {
        @NotBlank(message = "id不能为空")
        @Schema(description = "ID")
        private String id;
    }

    @Data
    public static class IdRequest {
        @NotBlank(message = "addressId不能为空")
        @Schema(description = "地址ID")
        private String addressId;
    }

    @Data
    public static class LocationResolveResponse {
        @Schema(description = "province")
        private String province;

        @Schema(description = "city")
        private String city;

        @Schema(description = "district")
        private String district;

        @Schema(description = "street")
        private String street;

        @Schema(description = "full地址")
        private String fullAddress;

        @Schema(description = "经度")
        private BigDecimal longitude;

        @Schema(description = "纬度")
        private BigDecimal latitude;
    }

    @Data
    public static class SaveResponse {
        @Schema(description = "地址ID")
        private String addressId;
    }
}
