package com.example.backend.model.admin;

import lombok.Data;

@Data
public class AdminStoreBusinessHourItem {

    private Integer dayOfWeek;

    private String startTime;

    private String endTime;

    private Integer isAvailable;
}
