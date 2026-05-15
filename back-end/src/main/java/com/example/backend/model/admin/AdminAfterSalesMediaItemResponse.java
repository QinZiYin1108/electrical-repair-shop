package com.example.backend.model.admin;

import lombok.Data;

@Data
public class AdminAfterSalesMediaItemResponse {

    private String id;
    private String url;
    private String thumbnailUrl;
    private String name;
    private String mimeType;
    private Integer duration;
}
