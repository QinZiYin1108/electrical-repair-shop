package com.example.backend.model.review;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "评价Reply请求")
@Data
public class ReviewReplyRequest {

    @Schema(description = "reply内容")
    private String replyContent;
}
