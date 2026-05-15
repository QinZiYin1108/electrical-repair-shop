package com.example.backend.model.worker;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WorkerSkillCategoryNode {

    private String id;

    private String name;

    private Integer level;

    private String parentId;

    private List<WorkerSkillCategoryNode> children = new ArrayList<>();
}
