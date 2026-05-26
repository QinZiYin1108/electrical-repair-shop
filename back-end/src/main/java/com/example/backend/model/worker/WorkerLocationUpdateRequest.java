package com.example.backend.model.worker;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WorkerLocationUpdateRequest {

    private BigDecimal latitude;
    private BigDecimal longitude;
    /**
     * 百度 reverse_geocoding coordtype，例如：gcj02ll / wgs84ll / bd09ll
     */
    private String coordType;
    /**
     * 可选：如果前端已拿到地址，可直接传入，后端将优先使用
     */
    private String address;
}

