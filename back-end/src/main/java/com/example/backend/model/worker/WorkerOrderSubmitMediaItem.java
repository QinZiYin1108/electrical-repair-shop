package com.example.backend.model.worker;

import lombok.Data;

@Data
public class WorkerOrderSubmitMediaItem {

    private String url;
    private String name;
    private Long fileSize;
    private String mimeType;
    private Integer width;
    private Integer height;
    private Integer duration;
    private String thumbnailUrl;
}
