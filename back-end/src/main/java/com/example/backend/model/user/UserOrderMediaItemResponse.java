package com.example.backend.model.user;

import lombok.Data;

@Data
public class UserOrderMediaItemResponse {

    private String id;
    private String url;
    private String thumbnailUrl;
    private String name;
    private String mimeType;
    private Integer duration;
}
