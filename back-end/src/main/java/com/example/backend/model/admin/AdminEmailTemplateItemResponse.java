package com.example.backend.model.admin;

import lombok.Data;

@Data
public class AdminEmailTemplateItemResponse {

    private String type;
    private String title;
    private String fileId;
    private String fileName;
    private String fileUrl;
    private Long updatedTime;
    private Boolean uploaded;
}