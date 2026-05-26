package com.example.backend.model.review;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReviewItemResponse {

    private String id;

    private String orderId;

    private String orderNo;

    private Integer orderType;

    private String orderTypeText;

    private Integer rating;

    private String content;

    private Integer isAnonymous;

    private Integer status;

    private String statusText;

    private Long createdTime;

    private Long updatedTime;

    private String userId;

    private String userName;

    private String userDisplayName;

    private String technicianId;

    private String technicianName;

    private String serviceTypeName;

    private String productId;

    private String productName;

    private String replyContent;

    private Long replyTime;

    private Boolean hasReply;

    private Boolean canReply;

    private List<ReviewImageItemResponse> images = new ArrayList<>();
}
