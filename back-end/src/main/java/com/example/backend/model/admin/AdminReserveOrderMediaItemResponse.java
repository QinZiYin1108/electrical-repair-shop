package com.example.backend.model.admin;

import lombok.Data;

@Data
public class AdminReserveOrderMediaItemResponse {

    private String id;
    private String name;
    private String url;
    private String thumbnailUrl;
    private String mimeType;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private Integer duration;
}
