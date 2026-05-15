package com.example.backend.model.user;

import lombok.Data;

@Data
public class UserOrderProgressItemResponse {

    private String id;
    private Integer status;
    private String statusText;
    private String description;
    private String operatorName;
    private Integer operatorType;
    private Long createdTime;
}
