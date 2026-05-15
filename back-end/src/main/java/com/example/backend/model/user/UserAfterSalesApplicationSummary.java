package com.example.backend.model.user;

import lombok.Data;

@Data
public class UserAfterSalesApplicationSummary {

    private String id;
    private Integer applicationType;
    private String applicationTypeText;
    private Integer status;
    private String statusText;
    private String reason;
    private String description;
    private String refundAmount;
    private String adminRemark;
    private Long createdTime;
    private Long updatedTime;
}
