package com.example.backend.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "用户首页Private响应")
@Data
public class UserHomePrivateResponse {

    private List<OrderSummaryItem> orderSummary = new ArrayList<>();

    @Schema(description = "latest排序")
    private LatestOrder latestOrder;

    private List<FollowedWorkerItem> followedWorkers = new ArrayList<>();

    @Data
    public static class OrderSummaryItem {
        @Schema(description = "键")
        private String key;

        @Schema(description = "标签")
        private String label;

        @Schema(description = "数量")
        private Integer count;
    }

    @Data
    public static class LatestOrder {
        @Schema(description = "排序ID")
        private String orderId;

        @Schema(description = "订单号")
        private String orderNo;

        @Schema(description = "电器")
        private String appliance;

        @Schema(description = "状态Text")
        private String statusText;

        @Schema(description = "step是否启用")
        private Integer stepActive;

        private List<StepItem> steps = new ArrayList<>();
    }

    @Data
    public static class StepItem {
        @Schema(description = "text")
        private String text;
    }

    @Data
    public static class FollowedWorkerItem {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "名称")
        private String name;

        @Schema(description = "initial")
        private String initial;

        @Schema(description = "技能")
        private String skill;

        @Schema(description = "分数")
        private String score;

        @Schema(description = "账号状态")
        private Integer accountStatus;

        @Schema(description = "工作状态")
        private Integer workStatus;

        @Schema(description = "状态Text")
        private String statusText;

        @Schema(description = "状态类型")
        private String statusType;

        @Schema(description = "头像URL")
        private String avatarUrl;
    }
}
