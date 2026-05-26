package com.example.backend.model.review;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReviewSubmitRequest {

    private String orderId;

    private Integer rating;

    private String content;

    private Integer isAnonymous;

    private List<ReviewSubmitImageItem> images = new ArrayList<>();
}
