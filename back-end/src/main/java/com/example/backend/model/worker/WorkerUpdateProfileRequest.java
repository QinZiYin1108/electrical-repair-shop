package com.example.backend.model.worker;

import lombok.Data;

@Data
public class WorkerUpdateProfileRequest {

    private String username;

    private Integer gender;

    private Long birthday;

    private Integer workYears;

    private String education;

    private String introduction;

    private Integer responseTime;
}
