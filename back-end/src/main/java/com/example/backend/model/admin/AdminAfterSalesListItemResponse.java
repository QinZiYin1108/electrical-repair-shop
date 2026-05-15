package com.example.backend.model.admin;

import lombok.Data;

@Data
public class AdminAfterSalesListItemResponse {

    private String id;
    private String orderId;
    private String orderNo;
    private Integer status;
    private String statusText;
    private Integer applicationType;
    private String applicationTypeText;
    private String reason;
    private String userId;
    private String userName;
    private String userPhone;
    private String technicianId;
    private String technicianName;
    private String serviceTypeName;
    private String serviceCategoryName;
    private Long createdTime;
    private Long updatedTime;
}
