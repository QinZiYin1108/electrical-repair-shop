package com.example.backend.model.admin;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminStoreUpdateRequest {

    private String name;

    private String logoImageId;

    private String contactPhone;

    private String address;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private String description;

    private String businessLicense;

    private String storeAdminId;

    private Integer isOnline;
}
