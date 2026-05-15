package com.example.backend.model.worker;

import lombok.Data;

@Data
public class WorkerSkillServiceTypeOption {

    private String id;

    private String name;

    private Integer type;

    private String typeText;

    private String categoryId;

    private String categoryName;

    private String categoryPath;

    private String description;
}
