package com.example.backend.model.worker;

import lombok.Data;

@Data
public class WorkerSkillItem {

    private String id;

    private String serviceTypeId;

    private String serviceTypeName;

    private Integer serviceMode;

    private String serviceModeText;

    private String categoryId;

    private String categoryName;

    private String categoryPath;

    private Integer skillLevel;

    private String skillLevelText;

    private Integer isActive;
}
