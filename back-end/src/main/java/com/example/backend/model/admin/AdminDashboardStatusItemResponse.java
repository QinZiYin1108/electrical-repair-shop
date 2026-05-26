package com.example.backend.model.admin;

import lombok.Data;

@Data
public class AdminDashboardStatusItemResponse {

    private Integer status;
    private String label;
    private Long count;
}
