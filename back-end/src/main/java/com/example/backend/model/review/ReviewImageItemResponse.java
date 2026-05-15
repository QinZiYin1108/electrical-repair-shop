package com.example.backend.model.review;

import lombok.Data;

@Data
public class ReviewImageItemResponse {

    private String id;

    private String url;

    private String thumbnailUrl;

    private String name;

    private Integer width;

    private Integer height;
}
