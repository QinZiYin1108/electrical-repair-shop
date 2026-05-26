package com.example.backend.model.user;

import lombok.Data;

@Data
public class UserMallCategoryResponse {

    private String id;

    private String name;

    private String pathText;

    private String iconUrl;

    private Integer productCount;
}
