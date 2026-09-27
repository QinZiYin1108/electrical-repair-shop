package com.example.backend.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户订单上门二维码响应")
@Data
public class UserOrderDoorQrResponse {

    @Schema(description = "排序ID")
    private String orderId;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "状态Text")
    private String statusText;

    @Schema(description = "二维码图片URL")
    private String qrImageUrl;

    @Schema(description = "过期时间")
    private Long expireTime;
}
