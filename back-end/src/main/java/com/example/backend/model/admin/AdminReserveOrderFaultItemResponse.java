package com.example.backend.model.admin;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminReserveOrderFaultItemResponse {

    private String id;
    private String faultPhenomenonId;
    private String faultPhenomenonName;
    private String faultPhenomenonDescription;
    private String faultDescription;
    private List<AdminReserveOrderMediaItemResponse> images = new ArrayList<>();
    private List<AdminReserveOrderMediaItemResponse> videos = new ArrayList<>();
}
