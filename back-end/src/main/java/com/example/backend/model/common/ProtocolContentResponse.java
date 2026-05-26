package com.example.backend.model.common;

import lombok.Data;

@Data
public class ProtocolContentResponse {

    private String type;
    private String title;
    private String fileName;
    private String content;
    private Long updatedTime;
    private Boolean uploaded;
}
