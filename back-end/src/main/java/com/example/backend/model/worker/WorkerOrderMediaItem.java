package com.example.backend.model.worker;

import lombok.Data;

@Data
public class WorkerOrderMediaItem {

    private String id;
    private String url;
    private String thumbnailUrl;
    private String name;
    private String mimeType;
    private Integer duration;
}
