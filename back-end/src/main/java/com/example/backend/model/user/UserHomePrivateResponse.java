package com.example.backend.model.user;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserHomePrivateResponse {

    private List<OrderSummaryItem> orderSummary = new ArrayList<>();

    private LatestOrder latestOrder;

    private List<FollowedWorkerItem> followedWorkers = new ArrayList<>();

    @Data
    public static class OrderSummaryItem {
        private String key;
        private String label;
        private Integer count;
    }

    @Data
    public static class LatestOrder {
        private String orderId;
        private String orderNo;
        private String appliance;
        private String statusText;
        private Integer stepActive;
        private List<StepItem> steps = new ArrayList<>();
    }

    @Data
    public static class StepItem {
        private String text;
    }

    @Data
    public static class FollowedWorkerItem {
        private String id;
        private String name;
        private String initial;
        private String skill;
        private String score;
        private Integer accountStatus;
        private Integer workStatus;
        private String statusText;
        private String statusType;
        private String avatarUrl;
    }
}
