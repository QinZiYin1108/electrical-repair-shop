package com.example.backend.model.user;

import lombok.Data;

@Data
public class UserAfterSalesDetailResponse {

    private String orderId;
    private String orderNo;
    private Integer orderStatus;
    private String orderStatusText;
    private String serviceTypeName;
    private String serviceCategoryName;
    private String serviceModeText;
    private String technicianName;
    private Boolean canApplyAfterSales;
    private String afterSalesTip;
    private UserAfterSalesApplicationDetailResponse application;
}
