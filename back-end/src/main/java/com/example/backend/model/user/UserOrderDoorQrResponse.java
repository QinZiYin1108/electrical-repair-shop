package com.example.backend.model.user;

import lombok.Data;

@Data
public class UserOrderDoorQrResponse {

    private String orderId;
    private Integer status;
    private String statusText;
    private String qrImageUrl;
    private Long expireTime;
}
