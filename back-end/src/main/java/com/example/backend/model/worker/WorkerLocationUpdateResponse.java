package com.example.backend.model.worker;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WorkerLocationUpdateResponse {

    private BigDecimal latitude;
    private BigDecimal longitude;
    private String address;
    private String areaName;
    private String province;
    private String city;
    private String district;
    private Long locationUpdateTime;
}
