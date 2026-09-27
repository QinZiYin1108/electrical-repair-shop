package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Schema(description = "管理员门店响应")
@Data
public class AdminStoreResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "Logo图片ID")
    private String logoImageId;

    @Schema(description = "Logo图片URL")
    private String logoImageUrl;

    @Schema(description = "门店管理员ID")
    private String storeAdminId;

    @Schema(description = "门店管理员名称")
    private String storeAdminName;

    @Schema(description = "门店管理员手机")
    private String storeAdminPhone;

    @Schema(description = "门店管理员邮箱")
    private String storeAdminEmail;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "营业状态")
    private Integer businessStatus;

    @Schema(description = "评分")
    private BigDecimal rating;

    @Schema(description = "评价数量")
    private Integer ratingCount;

    @Schema(description = "description")
    private String description;

    @Schema(description = "营业执照")
    private String businessLicense;

    @Schema(description = "审核状态")
    private Integer auditStatus;

    @Schema(description = "审核备注")
    private String auditRemark;

    @Schema(description = "审核时间")
    private Long auditTime;

    @Schema(description = "是否在线")
    private Integer isOnline;

    @Schema(description = "师傅数量")
    private Integer technicianCount;

    @Schema(description = "营业时间")
    private List<AdminStoreBusinessHourItem> businessHours;

    @Schema(description = "创建时间")
    private Long createdTime;

    @Schema(description = "更新时间")
    private Long updatedTime;
}
