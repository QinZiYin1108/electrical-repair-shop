package com.example.backend.model.review;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "评价提交请求")
@Data
public class ReviewSubmitRequest {

    @Schema(description = "排序ID")
    private String orderId;

    @Schema(description = "评分")
    private Integer rating;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "isAnonymous")
    private Integer isAnonymous;

    private List<ReviewSubmitImageItem> images = new ArrayList<>();
}
