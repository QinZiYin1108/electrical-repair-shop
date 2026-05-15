package com.example.backend.model.user;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserAfterSalesApplicationDetailResponse {

    private String id;
    private Integer applicationType;
    private String applicationTypeText;
    private Integer status;
    private String statusText;
    private String reason;
    private String description;
    private String refundAmount;
    private String adminRemark;
    private String contactPhone;
    private String contactAddress;
    private Boolean canCancel;
    private Long createdTime;
    private Long updatedTime;
    private Long processedTime;
    private Long completedTime;
    private List<UserOrderMediaItemResponse> evidenceImages = new ArrayList<>();
    private List<UserOrderMediaItemResponse> evidenceVideos = new ArrayList<>();
}
