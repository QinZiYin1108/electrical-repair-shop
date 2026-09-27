package com.example.backend.model.review;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "评价状态Update请求")
@Data
public class ReviewStatusUpdateRequest {

    @Schema(description = "状态")
    private Integer status;
}
