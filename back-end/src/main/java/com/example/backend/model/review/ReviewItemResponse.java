package com.example.backend.model.review;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "评价Item响应")
@Data
public class ReviewItemResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "排序ID")
    private String orderId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "排序类型")
    private Integer orderType;

    @Schema(description = "排序类型Text")
    private String orderTypeText;

    @Schema(description = "评分")
    private Integer rating;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "isAnonymous")
    private Integer isAnonymous;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "状态Text")
    private String statusText;

    @Schema(description = "创建时间")
    private Long createdTime;

    @Schema(description = "更新时间")
    private Long updatedTime;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名称")
    private String userName;

    @Schema(description = "用户Display名称")
    private String userDisplayName;

    @Schema(description = "师傅ID")
    private String technicianId;

    @Schema(description = "师傅名称")
    private String technicianName;

    @Schema(description = "服务类型名称")
    private String serviceTypeName;

    @Schema(description = "商品ID")
    private String productId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "reply内容")
    private String replyContent;

    @Schema(description = "reply时间")
    private Long replyTime;

    @Schema(description = "hasReply")
    private Boolean hasReply;

    @Schema(description = "canReply")
    private Boolean canReply;

    private List<ReviewImageItemResponse> images = new ArrayList<>();
}
