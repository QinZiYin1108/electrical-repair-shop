package com.example.backend.model.user;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserOrderFaultItemResponse {

    private String id;
    private String faultPhenomenonId;
    private String faultPhenomenonName;
    private String faultPhenomenonDescription;
    private String faultDescription;
    private List<UserOrderMediaItemResponse> images = new ArrayList<>();
    private List<UserOrderMediaItemResponse> videos = new ArrayList<>();
}
