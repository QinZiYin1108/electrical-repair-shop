package com.example.backend.model.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AdminStoreResponse {

    private String id;

    private String name;

    private String logoImageId;

    private String logoImageUrl;

    private String storeAdminId;

    private String storeAdminName;

    private String storeAdminPhone;

    private String storeAdminEmail;

    private String contactPhone;

    private String address;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private Integer businessStatus;

    private BigDecimal rating;

    private Integer ratingCount;

    private String description;

    private String businessLicense;

    private Integer auditStatus;

    private String auditRemark;

    private Long auditTime;

    private Integer isOnline;

    private Integer technicianCount;

    private List<AdminStoreBusinessHourItem> businessHours;

    private Long createdTime;

    private Long updatedTime;
}
