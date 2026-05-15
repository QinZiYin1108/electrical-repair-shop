package com.example.backend.model.admin;

import lombok.Data;

@Data
public class AdminReserveOrderProgressItemResponse {

    private String id;
    private Integer status;
    private String statusText;
    private String description;
    private String operatorName;
    private Integer operatorType;
    private Long createdTime;
}
