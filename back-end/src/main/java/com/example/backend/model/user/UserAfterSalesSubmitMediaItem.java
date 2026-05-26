package com.example.backend.model.user;

import lombok.Data;

@Data
public class UserAfterSalesSubmitMediaItem {

    private String url;
    private String name;
    private Long fileSize;
    private String mimeType;
    private Integer width;
    private Integer height;
    private Integer duration;
    private String thumbnailUrl;
}
