package com.example.backend.model.worker;

import java.math.BigDecimal;

public class WorkerStoreAddressRequest {

    private BigDecimal latitude;
    private BigDecimal longitude;
    private String coordType;

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public String getCoordType() { return coordType; }
    public void setCoordType(String coordType) { this.coordType = coordType; }
}
