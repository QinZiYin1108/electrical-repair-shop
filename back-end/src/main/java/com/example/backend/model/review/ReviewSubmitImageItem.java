package com.example.backend.model.review;

import lombok.Data;

@Data
public class ReviewSubmitImageItem {

    private String url;

    private String name;

    private Long fileSize;

    private String mimeType;

    private Integer width;

    private Integer height;
}
