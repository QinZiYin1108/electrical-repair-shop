package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理员门店Update请求")
@Data
public class AdminStoreUpdateRequest {

    @Schema(description = "名称")
    private String name;

    @Schema(description = "Logo图片ID")
    private String logoImageId;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "description")
    private String description;

    @Schema(description = "营业执照")
    private String businessLicense;

    @Schema(description = "门店管理员ID")
    private String storeAdminId;

    @Schema(description = "是否在线")
    private Integer isOnline;
}
