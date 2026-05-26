package com.example.backend.model.admin;

import lombok.Data;

@Data
public class AdminProductUploadMediaResponse {

    private String url;

    private String name;

    private Long fileSize;

    private String mimeType;

    private String mediaType;

    private Integer width;

    private Integer height;
}
