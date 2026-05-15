package com.example.backend.model.worker;

import lombok.Data;

@Data
public class WorkerOrderProgressItem {

    private String id;
    private Integer status;
    private String statusText;
    private String description;
    private String operatorName;
    private Integer operatorType;
    private Long createdTime;
}
