package com.example.backend.model.worker;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WorkerProfileResponse {

    private String id;

    private String username;

    private String phone;

    private String email;

    private Integer accountStatus;

    private Integer workStatus;

    private BigDecimal rating;

    private Integer orderCount;

    private BigDecimal completionRate;

    private String avatarUrl;

    private String realName;

    private String idCard;

    private Integer gender;

    private Long birthday;

    private Integer workYears;

    private String education;

    private String introduction;

    private Integer responseTime;

    private Long locationUpdateTime;
}
