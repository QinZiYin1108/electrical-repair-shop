package com.example.backend.model.admin;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminProductCategoryResponse {

    private String id;

    private String name;

    private String parentId;

    private String parentName;

    private Integer level;

    private String description;

    private String iconUrl;

    private Integer sortOrder;

    private Integer isActive;

    private Long createdTime;

    private Long updatedTime;

    private List<AdminProductCategoryResponse> children = new ArrayList<>();
}
