package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "管理员师傅订单Stats响应")
public class AdminWorkerOrderStatsResponse {

    @Schema(description = "总数数量")
    private Long totalCount;

    @Schema(description = "waiting数量")
    private Long waitingCount;

    @Schema(description = "ongoing数量")
    private Long ongoingCount;

    @Schema(description = "waitingPay数量")
    private Long waitingPayCount;

    @Schema(description = "completed数量")
    private Long completedCount;

    @Schema(description = "canceled数量")
    private Long canceledCount;

    @Schema(description = "refunded数量")
    private Long refundedCount;

    @Schema(description = "latest排序时间")
    private Long latestOrderTime;

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Long getWaitingCount() {
        return waitingCount;
    }

    public void setWaitingCount(Long waitingCount) {
        this.waitingCount = waitingCount;
    }

    public Long getOngoingCount() {
        return ongoingCount;
    }

    public void setOngoingCount(Long ongoingCount) {
        this.ongoingCount = ongoingCount;
    }

    public Long getWaitingPayCount() {
        return waitingPayCount;
    }

    public void setWaitingPayCount(Long waitingPayCount) {
        this.waitingPayCount = waitingPayCount;
    }

    public Long getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(Long completedCount) {
        this.completedCount = completedCount;
    }

    public Long getCanceledCount() {
        return canceledCount;
    }

    public void setCanceledCount(Long canceledCount) {
        this.canceledCount = canceledCount;
    }

    public Long getRefundedCount() {
        return refundedCount;
    }

    public void setRefundedCount(Long refundedCount) {
        this.refundedCount = refundedCount;
    }

    public Long getLatestOrderTime() {
        return latestOrderTime;
    }

    public void setLatestOrderTime(Long latestOrderTime) {
        this.latestOrderTime = latestOrderTime;
    }
}
