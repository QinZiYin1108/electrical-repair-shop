package com.example.backend.model.worker;

import lombok.Data;

import java.util.List;

@Data
public class WorkerOrderFaultItem {

    private String id;
    private String faultPhenomenonId;
    private String faultPhenomenonName;
    private String faultPhenomenonDescription;
    private String faultDescription;
    private List<WorkerOrderMediaItem> images;
    private List<WorkerOrderMediaItem> videos;
}
